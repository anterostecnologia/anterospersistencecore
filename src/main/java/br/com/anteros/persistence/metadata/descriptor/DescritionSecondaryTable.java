package br.com.anteros.persistence.metadata.descriptor;

import java.util.LinkedList;
import java.util.List;

public class DescritionSecondaryTable {
	
	private String tableName;
	private String catalog;
	private String schema;
	private List<DescriptionPkJoinColumn> pkJoinColumns = new LinkedList<DescriptionPkJoinColumn>();
	private String foreignKeyName;

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
	
	
	public void addPrimaryKey(DescriptionPkJoinColumn configuration){
		this.pkJoinColumns.add(configuration);
	}

	public List<DescriptionPkJoinColumn> getPkJoinColumns() {
		return pkJoinColumns;
	}

	public void setPkJoinColumns(List<DescriptionPkJoinColumn> pkJoinColumns) {
		this.pkJoinColumns = pkJoinColumns;
	}

	public String getForeignKeyName() {
		return foreignKeyName;
	}

	public void setForeignKeyName(String foreignKeyName) {
		this.foreignKeyName = foreignKeyName;
	}

	@Override
	public String toString() {
		return "DescritionSecondaryTable [tableName=" + tableName + ", catalog=" + catalog + ", schema=" + schema
				+ ", pkJoinColumns=" + pkJoinColumns + ", foreignKeyName=" + foreignKeyName + "]";
	}

}
