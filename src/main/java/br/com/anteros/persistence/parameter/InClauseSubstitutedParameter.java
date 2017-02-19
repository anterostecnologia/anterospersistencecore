package br.com.anteros.persistence.parameter;

import java.io.InputStream;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class InClauseSubstitutedParameter extends SubstitutedParameter {
	
	protected final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
	protected final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
	protected final DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("HH:mm:ss");

	public InClauseSubstitutedParameter(String name, List<Object> values) {
		super(name, values);
		this.value = convertValues(values.toArray(new Object[]{}));
	}
	
	public InClauseSubstitutedParameter(String name, Object... values) {
		super(name, values);
		this.value = convertValues(values);
	}
	
	protected String convertValues(Object... values){
		
		boolean appendDelimiter = false;
		StringBuilder result = new StringBuilder("");
		for (Object o : values){
			if (appendDelimiter)
				result.append(",");
			result.append(convertObjectToLiteral(o));
			appendDelimiter = true;
		}
		return result.toString();
	}
	
	protected String convertObjectToLiteral(Object value) {
		if (value instanceof Calendar) {
			return dateTimeFormatter.print(((Calendar) value).getTimeInMillis());
		} else if (value instanceof DateTime) {
			return dateTimeFormatter.print((DateTime) value);
		} else if (value instanceof Date) {
			return dateFormatter.print(((Date) value).getTime());
		} else if (value instanceof java.sql.Date) {
			return dateFormatter.print(((java.sql.Date) value).getTime());
		} else if (value instanceof InputStream) {
			return value.toString();
		} else if (value instanceof Timestamp) {
			return dateTimeFormatter.print(((Timestamp) value).getTime());
		} else if (value instanceof Time) {
			return timeFormatter.print(((Time) value).getTime());
		} else if (value instanceof String) {
			return "'"+value.toString()+"'";
		}
		return value.toString();
	}

}
