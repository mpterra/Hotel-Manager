package model;

import java.sql.Connection;
import java.sql.PreparedStatement;

import db.DatabaseConnector;

public class Hospede {
	
	private int id;
	private String nome;
	private String sexo;
	private String documento;
	private String telefone;
	private String email;
	
	
	//Constructors
	
	@SuppressWarnings("unused")
	public Hospede() {
		
	}	
	
	public Hospede(String nome, String sexo, String documento, String telefone, String email) {
		this.nome = nome;
		this.documento = documento;
		this.email = email;
		this.telefone = telefone;
		this.sexo = sexo;
	}


	//Getters and Setters

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public String getDocumento() {
		return documento;
	}
	public void setDocumento(String documento) {
		this.documento = documento;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getWhatsapp() {
		return telefone;
	}
	public void setWhatsapp(String whatsapp) {
		this.telefone = whatsapp;
	}
	public String getSexo() {
		return sexo;
	}
	public void setSexo(String sexo) {
		this.sexo = sexo;
	}

	public void inserirNoBanco() {
		
        String sql = "INSERT INTO hospede (nome, sexo, documento, telefone, email) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnector.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nome);
            stmt.setString(2, sexo);
            stmt.setString(3, documento);
            stmt.setString(4, telefone);
            stmt.setString(5, email);

            stmt.executeUpdate();
            System.out.println("Hóspede inserido com sucesso.");

        } catch (Exception e) {
            System.err.println("Erro ao inserir hóspede:");
            e.printStackTrace();
        }
    }
	
	@Override
	public String toString() {
		return nome;
	}
	

}
