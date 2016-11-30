package br.com.anteros.persistence.metadata.descriptor;

import java.util.LinkedList;
import java.util.List;

public class DescritionSecondaryTable {
	
	private String tableName;
	private String catalog;
	private String schema;
	private List<DescriptionPkJoinColumnConfiguration> pkJoinColumns = new LinkedList<DescriptionPkJoinColumnConfiguration>();

	public DescritionSecondaryTable(String catalog, String schema, String tableName) {
		this.catalog = catalog;
		this.schema = schema;
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}
	
	
	public void addPrimaryKey(DescriptionPkJoinColumnConfiguration configuration){
		this.pkJoinColumns.add(configuration);
	}
	
	

}
