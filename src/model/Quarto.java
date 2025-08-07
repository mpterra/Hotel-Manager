package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import db.DatabaseConnector;

public class Quarto {
	
	private int numero;
	private String status;
	private List<Cama> camas;
	
	
	//Constructors
	
	public Quarto(int numero, String status) {
		this.numero = numero;
		this.status = status;
	}
	
	
	
	//Getters and Setters
	
	public int getNumero() {
		return numero;
	}
	public void setNumero(int numero) {
		this.numero = numero;
	}
	public String getDescricao() {
		return status;
	}
	public void setDescricao(String descricao) {
		this.status = descricao;
	}
	public List<Cama> getCamas() {
		return camas;
	}
	public void setCamas(List<Cama> camas) {
		this.camas = camas;
	}



	public void inserirNoBanco() {
        String sql = "INSERT INTO quarto (numero, status) VALUES (?, ?)";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, numero);
            stmt.setString(2, status);

            stmt.executeUpdate();
            System.out.println("Quarto inserido com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	

}
