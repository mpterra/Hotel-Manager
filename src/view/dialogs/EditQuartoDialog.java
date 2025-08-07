package view.dialogs;

import db.DatabaseConnector;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class EditQuartoDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final int numeroQuarto;

    private JTextField txtNumero;
    private JTextField txtStatus;

    public EditQuartoDialog(Window owner, int numeroQuarto) {
        super(owner, "Editar Quarto", ModalityType.APPLICATION_MODAL);
        this.numeroQuarto = numeroQuarto;

        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 10, 6, 10);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0;

        // Número (não editável, pois é PK)
        add(new JLabel("Número:"), gc);
        gc.gridx = 1;
        txtNumero = new JTextField(10);
        txtNumero.setEditable(false);
        add(txtNumero, gc);

        // Status
        gc.gridx = 0; gc.gridy++;
        add(new JLabel("Status:"), gc);
        gc.gridx = 1;
        txtStatus = new JTextField(15);
        add(txtStatus, gc);

        // Botões
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnSalvar = new JButton("Salvar");
        JButton btnExcluir = new JButton("Excluir");
        JButton btnCancelar = new JButton("Cancelar");
        buttons.add(btnSalvar);
        buttons.add(btnExcluir);
        buttons.add(btnCancelar);

        gc.gridx = 0; gc.gridy++;
        gc.gridwidth = 2;
        add(buttons, gc);

        // Ações dos botões
        btnSalvar.addActionListener(e -> salvar());
        btnSalvar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExcluir.addActionListener(e -> excluir());
        btnExcluir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelar.addActionListener(e -> dispose());
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        carregarDados();

        pack();
        setMinimumSize(new Dimension(400, getHeight()));
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void carregarDados() {
        String sql = "SELECT numero, status FROM quarto WHERE numero = ?";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numeroQuarto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtNumero.setText(String.valueOf(rs.getInt("numero")));
                    txtStatus.setText(rs.getString("status"));
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Quarto não encontrado.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                    dispose();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erro ao carregar dados.",
                "Erro", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void salvar() {
        String status = txtStatus.getText().trim();

        if (status.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "O campo status não pode ficar vazio.",
                "Validação", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "UPDATE quarto SET status = ? WHERE numero = ?";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, numeroQuarto);

            int n = ps.executeUpdate();
            if (n > 0) {
                JOptionPane.showMessageDialog(this,
                    "Quarto atualizado com sucesso.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                    "Nenhum registro foi alterado.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erro ao salvar o quarto.",
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void excluir() {
        int op = JOptionPane.showConfirmDialog(this,
            "Confirma excluir este quarto?",
            "Confirmar", JOptionPane.YES_NO_OPTION);
        if (op != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM quarto WHERE numero = ?";
        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numeroQuarto);
            int n = ps.executeUpdate();
            if (n > 0) {
                JOptionPane.showMessageDialog(this,
                    "Quarto excluído com sucesso.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                    "Quarto não encontrado para exclusão.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Não foi possível excluir o quarto. Verifique vínculos existentes.",
                "Erro ao excluir", JOptionPane.ERROR_MESSAGE);
        }
    }
}
