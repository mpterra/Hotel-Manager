package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.MessageDigest;

import db.DatabaseConnector;

public class Usuario {
    private int id;
    private String login;
    private String senha; // senha em texto puro, sem hash aqui

    // Constructor
    public Usuario(String login, String senha) {
        this.login = login;
        this.senha = senha;
    }

    // Getters e setters
    public int getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getSenha() {
        return senha;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Método para autenticar com comparação de hash
    public boolean autenticar() {
        String sql = "SELECT senha FROM usuario WHERE login = ?";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String senhaDoBanco = rs.getString("senha");          // hash salva no banco
                String senhaHashed = hashSenha(this.senha);           // hash da senha informada

                System.out.println("Senha do banco: " + senhaDoBanco);
                System.out.println("Senha informada (hash): " + senhaHashed);

                boolean resultado = senhaDoBanco.equals(senhaHashed);
                System.out.println("Senha bate? " + resultado);
                return resultado;
            } else {
                System.out.println("Usuário não encontrado no banco.");
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Função para gerar hash SHA-256 da senha
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
