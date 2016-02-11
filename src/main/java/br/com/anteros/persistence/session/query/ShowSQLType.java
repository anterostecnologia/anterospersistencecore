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

	public static ShowSQLType[] parse(String[] splitShowSql) {
		Set<ShowSQLType> result = new HashSet<ShowSQLType>();
		for (String s : splitShowSql){
			if (s.toLowerCase().equals("true")){
				result.add(ShowSQLType.ALL);
			} else if (s.toLowerCase().equals("false")){
				result.add(ShowSQLType.NONE);
			} else if (s.toLowerCase().equals("select")){
				result.add(ShowSQLType.SELECT);
			} else if (s.toLowerCase().equals("insert")){
				result.add(ShowSQLType.INSERT);
			} else if (s.toLowerCase().equals("delete")){
				result.add(ShowSQLType.DELETE);
			} else if (s.toLowerCase().equals("update")){
				result.add(ShowSQLType.UPDATE);
			}
		}
		return result.toArray(new ShowSQLType[]{});
	}
}
