package view.dialogs;

import db.DatabaseConnector;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("serial")
public class QuartoDialog extends JDialog {

    private final JTable tabela;
    private final int numeroQuarto;

    public QuartoDialog(Window window, int numeroQuarto) {
        super(window, "Quarto " + numeroQuarto);
        this.numeroQuarto = numeroQuarto;

        setLayout(new BorderLayout(10, 10));
        setSize(700, 400);
        setLocationRelativeTo(window);

        tabela = new JTable(new DefaultTableModel(
                new Object[]{"hospedagemId", "Nome", "Telefone", "Email", "Entrada", "Saída"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        tabela.setRowSelectionAllowed(true);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Esconder coluna hospedagemId (índice 0)
        tabela.getColumnModel().getColumn(0).setMinWidth(0);
        tabela.getColumnModel().getColumn(0).setMaxWidth(0);
        tabela.getColumnModel().getColumn(0).setWidth(0);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(0);

        // Custom renderer para pintar linhas com data saída anterior a hoje de vermelho
        tabela.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    try {
                        int modelRow = table.convertRowIndexToModel(row);
                        String dataSaidaStr = (String) table.getModel().getValueAt(modelRow, 5);

                        if (dataSaidaStr != null && !dataSaidaStr.isEmpty()) {
                            LocalDate dataSaida = LocalDate.parse(dataSaidaStr, formatter);
                            LocalDate hoje = LocalDate.now();

                            if (dataSaida.isBefore(hoje)) {
                                c.setBackground(Color.RED);
                                c.setForeground(Color.WHITE);
                            } else if (!dataSaida.isAfter(hoje.plusDays(28))) {
                                c.setBackground(Color.YELLOW);
                                c.setForeground(Color.BLACK);
                            } else {
                                c.setBackground(Color.WHITE);
                                c.setForeground(Color.BLACK);
                            }
                        } else {
                            c.setBackground(Color.WHITE);
                            c.setForeground(Color.BLACK);
                        }
                    } catch (Exception e) {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                } else {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                }

                return c;
            }
        });





        // Duplo clique para abrir EditCheckOutDialog
        tabela.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2 && !evt.isConsumed()) {
                    evt.consume();
                    int row = tabela.getSelectedRow();
                    if (row != -1) {
                        int hospedagemId = (int) tabela.getModel().getValueAt(row, 0);
                        EditCheckOutDialog dialog = new EditCheckOutDialog(QuartoDialog.this, hospedagemId);
                        dialog.setVisible(true);
                        carregarHospedes();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tabela);
        add(scrollPane, BorderLayout.CENTER);

        JButton btnLiberar = new JButton("Liberar Quarto");
        btnLiberar.setPreferredSize(new Dimension(140, 32));
        btnLiberar.addActionListener(e -> liberarQuarto());
        btnLiberar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnLiberar);
        add(bottom, BorderLayout.SOUTH);

        carregarHospedes();
    }

    private void carregarHospedes() {
        DefaultTableModel modelo = (DefaultTableModel) tabela.getModel();
        modelo.setRowCount(0);

        String sql = "SELECT res.id AS hospedagem_id, h.nome, h.telefone, h.email, res.data_entrada, res.data_saida " +
                "FROM hospedagem res " +
                "JOIN cama c ON res.cama_id = c.id " +
                "JOIN hospede h ON res.hospede_id = h.id " +
                "WHERE c.quarto_numero = ? AND res.status = 1";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numeroQuarto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    modelo.addRow(new Object[]{
                            rs.getInt("hospedagem_id"),
                            rs.getString("nome"),
                            rs.getString("telefone"),
                            rs.getString("email"),
                            formatarData(rs.getString("data_entrada")),
                            formatarData(rs.getString("data_saida"))
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar hóspedes.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatarData(String dataISO) {
        if (dataISO == null || dataISO.isEmpty()) return "";
        LocalDate data = LocalDate.parse(dataISO);
        return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private void liberarQuarto() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Deseja realmente liberar todas as camas deste quarto?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DatabaseConnector.conectar()) {
            conn.setAutoCommit(false); // Transação

            String updateHospedagem = "UPDATE hospedagem " +
                    "SET status = 0, data_saida = ? " +
                    "WHERE cama_id IN (SELECT id FROM cama WHERE quarto_numero = ?) AND status = 1";

            String updateCamas = "UPDATE cama SET status = 0 WHERE quarto_numero = ?";

            try (PreparedStatement psHospedagem = conn.prepareStatement(updateHospedagem);
                 PreparedStatement psCamas = conn.prepareStatement(updateCamas)) {

                LocalDate hoje = LocalDate.now();
                psHospedagem.setString(1, hoje.toString());
                psHospedagem.setInt(2, numeroQuarto);
                psHospedagem.executeUpdate();

                psCamas.setInt(1, numeroQuarto);
                psCamas.executeUpdate();

                conn.commit();
                JOptionPane.showMessageDialog(this, "Quarto liberado com sucesso!");
                dispose();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao liberar quarto.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
