package view;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;

import db.DatabaseConnector;

public class CadastroUsuarioPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JTextField txtLogin;
    private JPasswordField txtSenha;
    private JPasswordField txtConfirmarSenha;

    public CadastroUsuarioPanel() {
        setLayout(null);

        JLabel lblLogin = new JLabel("Login:");
        lblLogin.setBounds(240, 50, 80, 25);
        add(lblLogin);

        txtLogin = new JTextField();
        txtLogin.setBounds(330, 50, 200, 25);
        add(txtLogin);

        JLabel lblSenha = new JLabel("Senha:");
        lblSenha.setBounds(240, 90, 80, 25);
        add(lblSenha);

        txtSenha = new JPasswordField();
        txtSenha.setBounds(330, 90, 200, 25);
        add(txtSenha);

        JLabel lblConfirmarSenha = new JLabel("Confirmar Senha:");
        lblConfirmarSenha.setBounds(200, 130, 130, 25);
        add(lblConfirmarSenha);

        txtConfirmarSenha = new JPasswordField();
        txtConfirmarSenha.setBounds(330, 130, 200, 25);
        add(txtConfirmarSenha);

        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.setBounds(330, 180, 100, 30);
        btnSalvar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(btnSalvar);

        btnSalvar.addActionListener(this::salvarUsuario);
    }

    private void salvarUsuario(ActionEvent e) {
        String login = txtLogin.getText().trim();
        String senha = new String(txtSenha.getPassword());
        String confirmarSenha = new String(txtConfirmarSenha.getPassword());

        if (login.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha todos os campos.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!senha.equals(confirmarSenha)) {
            JOptionPane.showMessageDialog(this, "As senhas não coincidem.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        inserirUsuario(login, senha);
        limpaCampos();
        JOptionPane.showMessageDialog(this, "Usuário salvo com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
    }

    private void inserirUsuario(String login, String senha) {
        String sql = "INSERT INTO usuario (login, senha) VALUES (?, ?)";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            stmt.setString(2, hashSenha(senha)); // salva senha hashada
            stmt.executeUpdate();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao salvar usuário.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpaCampos() {
        txtLogin.setText("");
        txtSenha.setText("");
        txtConfirmarSenha.setText("");
    }

    // Método para gerar hash SHA-256 da senha
    private static String hashSenha(String senha) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(senha.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
