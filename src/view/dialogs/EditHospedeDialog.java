package view.dialogs;

import db.DatabaseConnector;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Diálogo para editar ou excluir um hóspede.
 * Abre com os dados carregados via ID e permite Salvar (UPDATE) ou Excluir (DELETE).
 */
public class EditHospedeDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final int idHospede;

    private JTextField txtNome;
    private JComboBox<String> comboSexo;
    private JTextField txtDocumento;
    private JTextField txtTelefone;
    private JTextField txtEmail;

    public EditHospedeDialog(Window owner, int idHospede) {
        super(owner, "Editar Hóspede", ModalityType.APPLICATION_MODAL);
        this.idHospede = idHospede;

        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 10, 6, 10);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0;

        // Campos
        add(new JLabel("Nome:"), gc);
        gc.gridx = 1;
        txtNome = new JTextField(22);
        add(txtNome, gc);

        gc.gridx = 0; gc.gridy++;
        add(new JLabel("Sexo:"), gc);
        gc.gridx = 1;
        comboSexo = new JComboBox<>();
        comboSexo.addItem(" ");
        comboSexo.addItem("Masculino");
        comboSexo.addItem("Feminino");
        add(comboSexo, gc);

        gc.gridx = 0; gc.gridy++;
        add(new JLabel("Documento:"), gc);
        gc.gridx = 1;
        txtDocumento = new JTextField(22);
        add(txtDocumento, gc);

        gc.gridx = 0; gc.gridy++;
        add(new JLabel("Telefone:"), gc);
        gc.gridx = 1;
        txtTelefone = new JTextField(22);
        add(txtTelefone, gc);

        gc.gridx = 0; gc.gridy++;
        add(new JLabel("Email:"), gc);
        gc.gridx = 1;
        txtEmail = new JTextField(22);
        add(txtEmail, gc);

        // Botões
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnSalvar  = new JButton("Salvar");
        JButton btnExcluir = new JButton("Excluir");
        JButton btnCancelar= new JButton("Cancelar");
        buttons.add(btnSalvar);
        buttons.add(btnExcluir);
        buttons.add(btnCancelar);

        gc.gridx = 0; gc.gridy++;
        gc.gridwidth = 2;
        add(buttons, gc);

        // Ações
        btnSalvar.addActionListener(e -> salvar());
        btnSalvar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExcluir.addActionListener(e -> excluir());
        btnExcluir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelar.addActionListener(e -> dispose());
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        carregarDados();

        pack();
        setMinimumSize(new Dimension(420, getHeight()));
        setResizable(false);
    }

    private void carregarDados() {
        String sql = "SELECT nome, sexo, documento, telefone, email FROM hospede WHERE id = ?";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idHospede);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtNome.setText(rs.getString("nome"));
                    setSexoFromDB(rs.getString("sexo"));
                    txtDocumento.setText(rs.getString("documento"));
                    txtTelefone.setText(rs.getString("telefone"));
                    txtEmail.setText(rs.getString("email"));
                } else {
                    JOptionPane.showMessageDialog(this, "Registro não encontrado.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    dispose();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar dados.", "Erro", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void setSexoFromDB(String db) {
        String v = db == null ? "" : db.trim().toUpperCase();
        if ("M".equals(v)) comboSexo.setSelectedItem("Masculino");
        else if ("F".equals(v)) comboSexo.setSelectedItem("Feminino");
        else comboSexo.setSelectedIndex(0);
    }

    private String getSexoForDB() {
        String sel = (String) comboSexo.getSelectedItem();
        if ("Masculino".equalsIgnoreCase(sel)) return "M";
        if ("Feminino".equalsIgnoreCase(sel))  return "F";
        return "";
    }

    private void salvar() {
        String sql = "UPDATE hospede SET nome = ?, sexo = ?, documento = ?, telefone = ?, email = ? WHERE id = ?";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, txtNome.getText().trim());
            ps.setString(2, getSexoForDB());
            ps.setString(3, txtDocumento.getText().trim());
            ps.setString(4, txtTelefone.getText().trim());
            ps.setString(5, txtEmail.getText().trim());
            ps.setInt(6, idHospede);

            int n = ps.executeUpdate();
            if (n > 0) {
                JOptionPane.showMessageDialog(this, "Registro atualizado com sucesso.", "OK", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Nada foi alterado.", "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao salvar.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void excluir() {
        int op = JOptionPane.showConfirmDialog(this, "Confirma excluir este hóspede?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (op != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM hospede WHERE id = ?";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idHospede);
            int n = ps.executeUpdate();
            if (n > 0) {
                JOptionPane.showMessageDialog(this, "Registro excluído.", "OK", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Registro não encontrado para exclusão.", "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            // Provável FK impedindo exclusão (ex.: hospedagem vinculada)
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Não foi possível excluir. Verifique se há vínculos (hospedagens) associados a este hóspede.",
                "Erro ao excluir", JOptionPane.ERROR_MESSAGE);
        }
    }
}
