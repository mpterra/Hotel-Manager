package view.dialogs;

import db.DatabaseConnector;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Diálogo para editar ou excluir uma cama.
 * - Edita: descrição, nº do quarto e status (0=Desocupado, 1=Ocupado).
 * - Exclui: com confirmação. Se houver vínculos (hospedagem), mostra erro amigável.
 */
public class EditCamaDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final int idCama;

    private JTextField txtDescricao;
    private JComboBox<Integer> comboNumeroQuarto;
    private JComboBox<String> comboStatus;

    public EditCamaDialog(Window owner, int idCama) {
        super(owner, "Editar Cama", ModalityType.APPLICATION_MODAL);
        this.idCama = idCama;

        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 10, 6, 10);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0;

        // Descrição
        add(new JLabel("Descrição:"), gc);
        gc.gridx = 1;
        txtDescricao = new JTextField(22);
        add(txtDescricao, gc);

        // Nº Quarto
        gc.gridx = 0; gc.gridy++;
        add(new JLabel("Nº do Quarto:"), gc);
        gc.gridx = 1;
        comboNumeroQuarto = new JComboBox<>();
        add(comboNumeroQuarto, gc);

        // Status
        gc.gridx = 0; gc.gridy++;
        add(new JLabel("Status:"), gc);
        gc.gridx = 1;
        comboStatus = new JComboBox<>(new String[] { "Desocupado", "Ocupado" });
        add(comboStatus, gc);

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

        carregarQuartos();
        carregarDados();

        pack();
        setMinimumSize(new Dimension(420, getHeight()));
        setResizable(false);
    }

    private void carregarQuartos() {
        comboNumeroQuarto.removeAllItems();
        String sql = "SELECT numero FROM quarto ORDER BY numero";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                comboNumeroQuarto.addItem(rs.getInt("numero"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar quartos.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregarDados() {
        String sql = "SELECT descricao, quarto_numero, status FROM cama WHERE id = ?";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCama);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtDescricao.setText(rs.getString("descricao"));
                    Integer quarto = rs.getInt("quarto_numero");
                    comboNumeroQuarto.setSelectedItem(quarto);
                    int status = rs.getInt("status");
                    comboStatus.setSelectedItem(status == 0 ? "Desocupado" : "Ocupado");
                } else {
                    JOptionPane.showMessageDialog(this, "Cama não encontrada.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    dispose();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar dados da cama.", "Erro", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private int statusToInt(String s) { return "Ocupado".equalsIgnoreCase(s) ? 1 : 0; }

    private void salvar() {
        String descricao = txtDescricao.getText().trim();
        Integer quarto   = (Integer) comboNumeroQuarto.getSelectedItem();
        int status       = statusToInt((String) comboStatus.getSelectedItem());

        if (descricao.isEmpty() || quarto == null) {
            JOptionPane.showMessageDialog(this, "Preencha descrição e selecione um quarto.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "UPDATE cama SET descricao = ?, quarto_numero = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, descricao);
            ps.setInt(2, quarto);
            ps.setInt(3, status);
            ps.setInt(4, idCama);

            int n = ps.executeUpdate();
            if (n > 0) {
                JOptionPane.showMessageDialog(this, "Cama atualizada com sucesso.", "OK", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Nada foi alterado.", "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao salvar alterações.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void excluir() {
        int op = JOptionPane.showConfirmDialog(this, "Confirma excluir esta cama?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (op != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM cama WHERE id = ?";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCama);
            int n = ps.executeUpdate();
            if (n > 0) {
                JOptionPane.showMessageDialog(this, "Cama excluída.", "OK", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Registro não encontrado para exclusão.", "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            // Provável vínculo com hospedagens (FK)
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Não foi possível excluir. Verifique se há hospedagens vinculadas a esta cama.",
                "Erro ao excluir", JOptionPane.ERROR_MESSAGE);
        }
    }
}
