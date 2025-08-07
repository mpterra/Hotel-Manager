package db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnector {

    private static final String DB_FOLDER = "db";
    private static final String DB_NAME = "hotel.db";

    public static Connection conectar() throws Exception {
        // Cria a pasta 'db' se não existir
        File folder = new File(DB_FOLDER);
        if (!folder.exists()) {
            folder.mkdir();
        }

        // Caminho completo do arquivo do banco
        File dbFile = new File(folder, DB_NAME);

        System.out.println("Caminho completo do banco: " + dbFile.getAbsolutePath());
        System.out.println("Existe? " + dbFile.exists());

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        return DriverManager.getConnection(url);
    }
}
