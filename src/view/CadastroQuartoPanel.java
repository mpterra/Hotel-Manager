package view;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import db.DatabaseConnector;
import model.Quarto;
import view.dialogs.EditQuartoDialog;

import java.awt.Cursor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.JButton;
import javax.swing.JTable;

public class CadastroQuartoPanel extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField txtNumero;
    private JTextField txtStatus;
    private JTable table;

    public CadastroQuartoPanel() {
        setLayout(null); // layout absoluto

        JLabel lblNumero = new JLabel("Número:");
        lblNumero.setBounds(248, 50, 80, 25);
        add(lblNumero);

        txtNumero = new JTextField();
        txtNumero.setBounds(338, 50, 200, 25);
        add(txtNumero);

        JLabel lblStatus = new JLabel("Status:");
        lblStatus.setBounds(248, 85, 80, 25);
        add(lblStatus);

        txtStatus = new JTextField();
        txtStatus.setBounds(338, 85, 200, 25);
        add(txtStatus);

        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.setBounds(338, 130, 100, 30);
        btnSalvar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(btnSalvar);

        // Criando tabela com modelo e colunas
        String[] colunas = { "Número", "Status" };
        DefaultTableModel modelo = new DefaultTableModel(new Object[][] {}, colunas) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isCellEditable(int row, int column) {
                return false; // impede edição em qualquer célula
            }
        };
        table = new JTable(modelo);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(34, 180, 715, 257);
        add(scrollPane);

        atualizarTabela();

        btnSalvar.addActionListener(e -> {
            int numero = Integer.parseInt(txtNumero.getText());
            String status = txtStatus.getText();

            Quarto quarto = new Quarto(numero, status);
            quarto.inserirNoBanco();

            atualizarTabela();
            limpaCampos();
        });
        
     // Detecta duplo clique na tabela para abrir diálogo de edição
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    int viewRow = table.getSelectedRow();
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    Integer numeroQuarto = (Integer) table.getModel().getValueAt(modelRow, 0);

                    EditQuartoDialog dlg = new EditQuartoDialog(
                        javax.swing.SwingUtilities.getWindowAncestor(CadastroQuartoPanel.this),
                        numeroQuarto
                    );
                    dlg.setLocationRelativeTo(CadastroQuartoPanel.this);
                    dlg.setVisible(true);

                    // Atualiza tabela após fechar diálogo
                    atualizarTabela();
                }
            }
        });
    }

    public void atualizarTabela() {
        DefaultTableModel modelo = (DefaultTableModel) table.getModel();
        modelo.setRowCount(0); // limpa as linhas

        String sql = "SELECT numero, status FROM quarto";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int numero = rs.getInt("numero");
                String status = rs.getString("status");

                modelo.addRow(new Object[] { numero, status });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void limpaCampos() {
        txtNumero.setText("");
        txtStatus.setText("");
    }
}
