package db;

import java.sql.*;
import java.security.MessageDigest;

public class DatabaseInitializer {

    public static void inicializarBanco() {
        String url = "jdbc:sqlite:db/hotel.db";

        String[] sqlStatements = {
            "CREATE TABLE IF NOT EXISTS usuario ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "login TEXT NOT NULL UNIQUE, "
            + "senha TEXT NOT NULL);",

            "CREATE TABLE IF NOT EXISTS hospede ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "nome TEXT NOT NULL, "
            + "sexo CHAR(1) NOT NULL CHECK (sexo IN ('M', 'F')),"
            + "documento TEXT, "
            + "telefone TEXT, "
            + "email TEXT); ",

            "CREATE TABLE IF NOT EXISTS quarto ("
            + "numero INTEGER PRIMARY KEY, "
            + "status TEXT);",

            "CREATE TABLE IF NOT EXISTS cama ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "descricao TEXT, "
            + "quarto_numero INTEGER NOT NULL, "
            + "status INTEGER NOT NULL DEFAULT 0 CHECK (status IN (0, 1)),"
            + "FOREIGN KEY (quarto_numero) REFERENCES quarto(numero));",

            "CREATE TABLE IF NOT EXISTS hospedagem ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "hospede_id INTEGER NOT NULL, "
            + "cama_id INTEGER NOT NULL, "
            + "data_entrada DATE NOT NULL, "
            + "data_saida DATE NOT NULL, "
            + "status INTEGER NOT NULL DEFAULT 1 CHECK (status IN (0, 1)),"
            + "FOREIGN KEY (hospede_id) REFERENCES hospede(id), "
            + "FOREIGN KEY (cama_id) REFERENCES cama(id));"
        };

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            for (String sql : sqlStatements) {
                stmt.executeUpdate(sql);
            }
            System.out.println("Banco inicializado com sucesso!");

            criarUsuarioAdminSeNaoExistir(conn);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void criarUsuarioAdminSeNaoExistir(Connection conn) throws SQLException {
        String sqlConsulta = "SELECT COUNT(*) FROM usuario WHERE login = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlConsulta)) {
            ps.setString(1, "admin");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    if (count == 0) {
                        // Insere usuário admin com senha hasheada
                        String sqlInsert = "INSERT INTO usuario (login, senha) VALUES (?, ?)";
                        try (PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {
                            psInsert.setString(1, "admin");
                            psInsert.setString(2, hashSenha("54321"));
                            psInsert.executeUpdate();
                            System.out.println("Usuário admin criado com sucesso.");
                        }
                    } else {
                        System.out.println("Usuário admin já existe.");
                    }
                }
            }
        }
    }

    public static String hashSenha(String senha) {
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
