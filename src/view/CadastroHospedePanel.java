package view;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import db.DatabaseConnector;

import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import model.Hospede;
import view.dialogs.EditHospedeDialog;

public class CadastroHospedePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private JTextField txtNome;
    private JTextField txtDocumento;
    private JTextField txtTelefone;
    private JTextField txtEmail;

    private JTable table;
    private JComboBox<String> comboBoxSexo = new JComboBox<>();

    public CadastroHospedePanel() {
        setLayout(null); // layout absoluto para facilitar edição no WindowBuilder

        JLabel lblNome = new JLabel("Nome:");
        lblNome.setBounds(248, 35, 80, 25);
        add(lblNome);

        txtNome = new JTextField();
        txtNome.setBounds(338, 35, 200, 25);
        add(txtNome);

        JLabel lblSexo = new JLabel("Sexo:");
        lblSexo.setBounds(248, 64, 80, 25);
        add(lblSexo);

        comboBoxSexo.setBounds(338, 64, 117, 25);
        comboBoxSexo.addItem(" ");        // vazio
        comboBoxSexo.addItem("Masculino");// M
        comboBoxSexo.addItem("Feminino"); // F
        add(comboBoxSexo);

        JLabel lblDocumento = new JLabel("Documento:");
        lblDocumento.setBounds(248, 94, 80, 25);
        add(lblDocumento);

        txtDocumento = new JTextField();
        txtDocumento.setBounds(338, 94, 200, 25);
        add(txtDocumento);

        JLabel lblTelefone = new JLabel("Telefone:");
        lblTelefone.setBounds(248, 123, 80, 25);
        add(lblTelefone);

        txtTelefone = new JTextField();
        txtTelefone.setBounds(338, 123, 200, 25);
        add(txtTelefone);

        JLabel lblEmail = new JLabel("Email:");
        lblEmail.setBounds(248, 151, 80, 25);
        add(lblEmail);

        txtEmail = new JTextField();
        txtEmail.setBounds(338, 153, 200, 25);
        add(txtEmail);

        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.setBounds(338, 189, 100, 30);
        btnSalvar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(btnSalvar);

        // -------- TABELA --------
        // Incluí a coluna "ID" (oculta) para sabermos qual registro abrir no diálogo.
        String[] colunas = { "ID", "Nome", "Sexo", "Documento", "Telefone", "Email" };
        DefaultTableModel modelo = new DefaultTableModel(new Object[][] {}, colunas) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // impede edição em qualquer célula
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // Garante que a coluna 0 (ID) seja Integer
                return columnIndex == 0 ? Integer.class : String.class;
            }
        };
        table = new JTable(modelo);
        table.setAutoCreateRowSorter(true); // permite ordenar

        // Oculta a coluna ID visualmente
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);

        // Double-click para abrir diálogo de edição
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    int viewRow = table.getSelectedRow();
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    Integer id = (Integer) table.getModel().getValueAt(modelRow, 0);
                    abrirDialogEdicao(id);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(34, 230, 715, 257);
        add(scrollPane);

        atualizarTabela();

        // ----- Ação Salvar (inserção) -----
        btnSalvar.addActionListener(e -> {
            String nome = txtNome.getText().trim();
            String sexoSelecionado = (String) comboBoxSexo.getSelectedItem();
            String sexo = mapSexoToDB(sexoSelecionado);
            String documento = txtDocumento.getText().trim();
            String telefone = txtTelefone.getText().trim();
            String email = txtEmail.getText().trim();

            Hospede hospede = new Hospede(nome, sexo, documento, telefone, email);
            hospede.inserirNoBanco();

            atualizarTabela();
            limpaCampos();
        });
    }

    private static String mapSexoToDB(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.equalsIgnoreCase("Masculino")) return "M";
        if (s.equalsIgnoreCase("Feminino"))  return "F";
        return ""; // vazio / não informado
    }

    private static String mapSexoToLabel(String db) {
        if (db == null) return " ";
        db = db.trim().toUpperCase();
        if ("M".equals(db)) return "Masculino";
        if ("F".equals(db)) return "Feminino";
        return " ";
    }

    private void abrirDialogEdicao(int idHospede) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        EditHospedeDialog dlg = new EditHospedeDialog(parent, idHospede);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        // Ao fechar o diálogo, atualiza a tabela
        atualizarTabela();
    }

    public void atualizarTabela() {
        DefaultTableModel modelo = (DefaultTableModel) table.getModel();
        modelo.setRowCount(0); // limpa as linhas existentes

        String sql = "SELECT id, nome, sexo, documento, telefone, email FROM hospede ORDER BY id DESC";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String nome = rs.getString("nome");
                String sexo = mapSexoToLabel(rs.getString("sexo")); // mostra label amigável
                String documento = rs.getString("documento");
                String telefone = rs.getString("telefone");
                String email = rs.getString("email");

                modelo.addRow(new Object[] { id, nome, sexo, documento, telefone, email });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void limpaCampos() {
        txtNome.setText("");
        txtDocumento.setText("");
        txtTelefone.setText("");
        txtEmail.setText("");
        comboBoxSexo.setSelectedIndex(0);
    }
}
