package br.com.anteros.persistence.metadata.configuration;

public class PrimaryKeyJoinColumnConfiguration {

	private String columnDefinition;
	private String name;
	private String referencedColumnName;
	
	public PrimaryKeyJoinColumnConfiguration(String columnDefinition, String name, String referencedColumnName) {
		super();
		this.columnDefinition = columnDefinition;
		this.name = name;
		this.referencedColumnName = referencedColumnName;
	}

	public String getColumnDefinition() {
		return columnDefinition;
	}

	public PrimaryKeyJoinColumnConfiguration columnDefinition(String columnDefinition) {
		this.columnDefinition = columnDefinition;
		return this;
	}

	public String getName() {
		return name;
	}

	public PrimaryKeyJoinColumnConfiguration name(String name) {
		this.name = name;
		return this;
	}

	public String getReferencedColumnName() {
		return referencedColumnName;
	}

	public PrimaryKeyJoinColumnConfiguration referencedColumnName(String referencedColumnName) {
		this.referencedColumnName = referencedColumnName;
		return this;
	}

	@Override
	public String toString() {
		return "PrimaryKeyJoinColumnConfiguration [columnDefinition=" + columnDefinition + ", name=" + name
				+ ", referencedColumnName=" + referencedColumnName + "]";
	}
	
	

}
