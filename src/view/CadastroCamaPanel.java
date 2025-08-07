package view;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import db.DatabaseConnector;
import view.dialogs.EditCamaDialog;

import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class CadastroCamaPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private JTextField txtDescricao;
    private JComboBox<Integer> comboNumeroQuarto;
    private JTable table;

    public CadastroCamaPanel() {
        setLayout(null); // layout absoluto

        JLabel lblDescricao = new JLabel("Descrição:");
        lblDescricao.setBounds(248, 50, 80, 25);
        add(lblDescricao);

        txtDescricao = new JTextField();
        txtDescricao.setBounds(338, 50, 200, 25);
        add(txtDescricao);

        JLabel lblNumeroQuarto = new JLabel("Nº do Quarto:");
        lblNumeroQuarto.setBounds(248, 85, 80, 25);
        add(lblNumeroQuarto);

        comboNumeroQuarto = new JComboBox<>();
        comboNumeroQuarto.setBounds(338, 85, 200, 25);
        comboNumeroQuarto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(comboNumeroQuarto);

        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.setBounds(338, 130, 100, 30);
        btnSalvar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(btnSalvar);

        // TABELA: inclui coluna ID oculta para localizar o registro no diálogo
        String[] colunas = { "ID", "Descrição", "Nº do Quarto", "Status" };
        DefaultTableModel modelo = new DefaultTableModel(new Object[][] {}, colunas) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Integer.class : String.class;
            }
        };
        table = new JTable(modelo);
        table.setAutoCreateRowSorter(true);

        // Oculta visualmente a coluna ID
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);

        // Duplo clique: abre o diálogo de edição/exclusão
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    int viewRow = table.getSelectedRow();
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    Integer id = (Integer) table.getModel().getValueAt(modelRow, 0);
                    abrirDialogEdicao(id);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(34, 180, 715, 257);
        add(scrollPane);

        carregarQuartos();
        atualizarTabela();

        btnSalvar.addActionListener(e -> {
            String descricao = txtDescricao.getText().replaceAll("\\s+", "");
            Integer numeroQuarto = (Integer) comboNumeroQuarto.getSelectedItem();
            if (numeroQuarto == null) {
                javax.swing.JOptionPane.showMessageDialog(this, "Selecione um quarto.", "Atenção", javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            String descricaoFinal = descricao + "_" + numeroQuarto;

            inserirNoBanco(descricaoFinal, numeroQuarto);
            atualizarTabela();
            limpaCampos();
        });
    }

    private void abrirDialogEdicao(int idCama) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        EditCamaDialog dlg = new EditCamaDialog(parent, idCama);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        atualizarTabela();
    }

    private void carregarQuartos() {
        String sql = "SELECT numero FROM quarto ORDER BY numero";
        comboNumeroQuarto.removeAllItems();

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                comboNumeroQuarto.addItem(rs.getInt("numero"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void inserirNoBanco(String descricao, int numeroQuarto) {
        String sql = "INSERT INTO cama (descricao, quarto_numero) VALUES (?, ?)";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, descricao);
            stmt.setInt(2, numeroQuarto);
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this, "Erro ao inserir cama.", "Erro", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    public void atualizarTabela() {
        DefaultTableModel modelo = (DefaultTableModel) table.getModel();
        modelo.setRowCount(0);

        String sql = "SELECT id, descricao, quarto_numero, status FROM cama ORDER BY id DESC";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String descricao = rs.getString("descricao");
                int numeroQuarto = rs.getInt("quarto_numero");
                int status = rs.getInt("status");

                String statusTexto = (status == 0) ? "Desocupado" : "Ocupado";
                modelo.addRow(new Object[] { id, descricao, String.valueOf(numeroQuarto), statusTexto });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void limpaCampos() {
        txtDescricao.setText("");
        if (comboNumeroQuarto.getItemCount() > 0) comboNumeroQuarto.setSelectedIndex(0);
    }
}
