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
		String parts[]=field.split("[ ]+");
		return parts[0];
	}

	public void setField(String field) {
		this.field = field;
	}
	
	public String getOrder() {
		String parts[]=field.split("[ ]+");
		if (parts.length>1)
			return parts[1];
		return "ASC";
	}
	

}
