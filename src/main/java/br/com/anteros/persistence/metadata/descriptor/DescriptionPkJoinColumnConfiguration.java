package br.com.anteros.persistence.metadata.descriptor;

public class DescriptionPkJoinColumnConfiguration {
	
	private String columnDefinition;
	private String name;
	private String referencedColumnName;
	
	
	public DescriptionPkJoinColumnConfiguration(String columnDefinition, String name, String referencedColumnName) {
		super();
		this.columnDefinition = columnDefinition;
		this.name = name;
		this.referencedColumnName = referencedColumnName;
	}
	public String getColumnDefinition() {
		return columnDefinition;
	}
	public void setColumnDefinition(String columnDefinition) {
		this.columnDefinition = columnDefinition;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getReferencedColumnName() {
		return referencedColumnName;
	}
	public void setReferencedColumnName(String referencedColumnName) {
		this.referencedColumnName = referencedColumnName;
	}

}
