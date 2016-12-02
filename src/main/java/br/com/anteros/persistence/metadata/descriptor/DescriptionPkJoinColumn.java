package br.com.anteros.persistence.metadata.descriptor;

public class DescriptionPkJoinColumn {
	
	private String name;
	private String referencedColumnName;
	
	
	public DescriptionPkJoinColumn(String name, String referencedColumnName) {
		super();
		this.name = name;
		this.referencedColumnName = referencedColumnName;
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

	@Override
	public String toString() {
		return "DescriptionPkJoinColumn [name=" + name + ", referencedColumnName=" + referencedColumnName + "]";
	}
}
