package br.com.anteros.persistence.session.query.filter;

public class FieldSort {
	
	protected String field;
	
	public FieldSort() {
	
	}

	public FieldSort(String field) {
		super();
		this.field = field;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}
	

}
