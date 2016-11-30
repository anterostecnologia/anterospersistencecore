package br.com.anteros.persistence.metadata.configuration;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SecondaryTableConfiguration {

	private String catalog;
	private String schema;
	private String tableName;
	private List<PrimaryKeyJoinColumnConfiguration> pkJoinColumns = new LinkedList<PrimaryKeyJoinColumnConfiguration>();

	public SecondaryTableConfiguration(){
		
	}
	
	public SecondaryTableConfiguration(String catalog, String schema, String tableName,
			PrimaryKeyJoinColumnConfiguration[] pkJoinColumns) {
		super();
		this.catalog = catalog;
		this.schema = schema;
		this.tableName = tableName;
		this.pkJoinColumns.addAll(Arrays.asList(pkJoinColumns));
	}
	
	public SecondaryTableConfiguration(String catalog, String schema) {
		super();
		this.catalog = catalog;
		this.schema = schema;
	}

	public String getCatalog() {
		return catalog;
	}

	public SecondaryTableConfiguration catalog(String catalog) {
		this.catalog = catalog;
		return this;
	}

	public String getSchema() {
		return schema;
	}

	public SecondaryTableConfiguration schema(String schema) {
		this.schema = schema;
		return this;
	}

	public List<PrimaryKeyJoinColumnConfiguration> getPkJoinColumns() {
		return pkJoinColumns;
	}

	public  SecondaryTableConfiguration pkJoinColumns(List<PrimaryKeyJoinColumnConfiguration> pkJoinColumns) {
		this.pkJoinColumns = pkJoinColumns;
		return this;
	}
	
	public SecondaryTableConfiguration add(PrimaryKeyJoinColumnConfiguration pkJoinColumn){
		this.pkJoinColumns.add(pkJoinColumn);
		return this;
	}

	public String getTableName() {
		return tableName;
	}

	public SecondaryTableConfiguration tableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public void setPkJoinColumns(List<PrimaryKeyJoinColumnConfiguration> pkJoinColumns) {
		this.pkJoinColumns = pkJoinColumns;
	}

}
