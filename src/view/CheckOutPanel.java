package view;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;
import db.DatabaseConnector;
import view.dialogs.EditCheckOutDialog;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class CheckOutPanel extends JPanel {
    private JTable table;
    private JTextField txtFiltro;
    private TableRowSorter<DefaultTableModel> sorter;

    public CheckOutPanel() {
        setLayout(null);

        JLabel lblFiltro = new JLabel("Buscar Hóspede:");
        lblFiltro.setBounds(30, 48, 150, 25);
        add(lblFiltro);

        txtFiltro = new JTextField();
        txtFiltro.setBounds(150, 48, 300, 25);
        add(txtFiltro);

        String[] colunas = { "ID", "Hóspede", "Email", "Telefone", "Cama", "Entrada", "Saída" };
        DefaultTableModel modelo = new DefaultTableModel(new Object[][] {}, colunas) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(modelo) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                int modelRow = convertRowIndexToModel(row);
                String dataSaidaStr = getModel().getValueAt(modelRow, 6).toString();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                try {
                    LocalDate dataSaida = LocalDate.parse(dataSaidaStr, formatter);
                    LocalDate hoje = LocalDate.now();
                    long diasRestantes = ChronoUnit.DAYS.between(hoje, dataSaida);

                    if (diasRestantes < 0) {
                        c.setBackground(Color.RED);
                        c.setForeground(Color.BLACK);
                    } else if (diasRestantes <= 28) {
                        c.setBackground(Color.YELLOW);
                        c.setForeground(Color.BLACK);
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                } catch (Exception e) {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }

                return c;
            }
        };

        sorter = new TableRowSorter<>(modelo);
        table.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBounds(30, 100, 720, 365);
        add(scroll);

        // Oculta a coluna ID
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);

        // Ajusta larguras
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getColumnModel().getColumn(6).setPreferredWidth(100);

        // Filtro por nome
        txtFiltro.getDocument().addDocumentListener(new DocumentListener() {
            private void filtrar() {
                String t = txtFiltro.getText().trim();
                sorter.setRowFilter(t.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(t), 1));
            }

            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e)  { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
        });

        // Duplo clique para editar
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        int modelRow = table.convertRowIndexToModel(row);
                        int idHospedagem = (int) table.getModel().getValueAt(modelRow, 0);
                        EditCheckOutDialog dialog = new EditCheckOutDialog(SwingUtilities.getWindowAncestor(CheckOutPanel.this), idHospedagem);
                        dialog.setLocationRelativeTo(CheckOutPanel.this);
                        dialog.setVisible(true);
                        atualizarTabela();
                    }
                }
            }
        });

        atualizarTabela();
    }

    private void atualizarTabela() {
        DefaultTableModel modelo = (DefaultTableModel) table.getModel();
        modelo.setRowCount(0);

        String sql = """
            SELECT h.id, ho.nome AS hospede, ho.email, ho.telefone,
                   c.descricao AS cama, h.data_entrada, h.data_saida
            FROM hospedagem h
            JOIN hospede ho ON ho.id = h.hospede_id
            JOIN cama c ON c.id = h.cama_id
            WHERE h.status = 1
            ORDER BY h.id DESC
        """;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String entradaFormatada = LocalDate.parse(rs.getString("data_entrada")).format(formatter);
                String saidaFormatada = LocalDate.parse(rs.getString("data_saida")).format(formatter);

                modelo.addRow(new Object[] {
                    rs.getInt("id"),
                    rs.getString("hospede"),
                    rs.getString("email"),
                    rs.getString("telefone"),
                    rs.getString("cama"),
                    entradaFormatada,
                    saidaFormatada
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
