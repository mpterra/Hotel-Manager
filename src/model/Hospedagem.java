package model;

import java.util.Date;

public class Hospedagem {
	
	private int id;
	private Hospede hospede;
	private Quarto quarto;
	private Date data_entrada;
	private Date data_saida;
	
	//Constructor
	public Hospedagem(int id, Hospede hospede, Quarto quarto, Date data_entrada, Date data_saida) {
		super();
		this.id = id;
		this.hospede = hospede;
		this.quarto = quarto;
		this.data_entrada = data_entrada;
		this.data_saida = data_saida;
	}
	
	//Getters and Setters

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Hospede getHospede() {
		return hospede;
	}

	public void setHospede(Hospede hospede) {
		this.hospede = hospede;
	}

	public Quarto getQuarto() {
		return quarto;
	}

	public void setQuarto(Quarto quarto) {
		this.quarto = quarto;
	}

	public Date getData_entrada() {
		return data_entrada;
	}

	public void setData_entrada(Date data_entrada) {
		this.data_entrada = data_entrada;
	}

	public Date getData_saida() {
		return data_saida;
	}

	public void setData_saida(Date data_saida) {
		this.data_saida = data_saida;
	}
	

}
