package br.com.anteros.persistence.session.query;

import java.util.HashSet;
import java.util.Set;

public enum ShowSQLType {

	ALL, NONE, SELECT, INSERT, UPDATE, DELETE;

	public static boolean contains(ShowSQLType[] list, ShowSQLType... items) {
		if ((list==null)||(items==null))
			return false;
		for (ShowSQLType t : list) {
			for (ShowSQLType item : items) {
				if (t.equals(item))
					return true;
			}
		}
		return false;
	}
	
	public static ShowSQLType[] parse(String showSql) {
		String[] splitShowSql = showSql.split("\\,");
		return ShowSQLType.parse(splitShowSql);
	}

	public static ShowSQLType[] parse(String[] splitShowSql) {
		Set<ShowSQLType> result = new HashSet<ShowSQLType>();
		for (String s : splitShowSql){
			if (s.trim().toLowerCase().equals("true")){
				result.add(ShowSQLType.ALL);
			} else if (s.trim().toLowerCase().equals("false")){
				result.add(ShowSQLType.NONE);
			} else if (s.trim().toLowerCase().equals("select")){
				result.add(ShowSQLType.SELECT);
			} else if (s.trim().toLowerCase().equals("insert")){
				result.add(ShowSQLType.INSERT);
			} else if (s.trim().toLowerCase().equals("delete")){
				result.add(ShowSQLType.DELETE);
			} else if (s.trim().toLowerCase().equals("update")){
				result.add(ShowSQLType.UPDATE);
			}
		}
		return result.toArray(new ShowSQLType[]{});
	}
	
	public static String parse(ShowSQLType[] showSql) {
		String result = "";
		boolean appendDelimiter = false;
		for (ShowSQLType s : showSql){
			if (appendDelimiter)
				result += ",";
			result += s.toString();
		}
		return result;
	}
	
	@Override
	public String toString() {
		if (this.equals(ALL))
		return "true";
		else if (this.equals(DELETE))
			return  "delete";
		else if (this.equals(INSERT))
			return "insert";
		else if (this.equals(NONE))
			return "false";
		else if (this.equals(SELECT))
			return "select";
		else if (this.equals(UPDATE))
			return "update";
		return "false";
	}
}
