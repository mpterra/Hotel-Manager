package model;

public class Cama {
	
	private int id;
	private String descricao;
	
	//Constructors
	
	public Cama() {
		
	}
	
	public Cama(int id, String desc) {
		this.id = id;
		this.descricao = desc;
	}
	
	
	//Getters and Setters
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
	
	
	@Override
	public String toString() {
	    return "Cama " + descricao;
	}

	

}
