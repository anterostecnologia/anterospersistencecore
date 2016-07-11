package br.com.anteros.persistence.session.query.filter;

public class FilterException extends Exception {

	public FilterException() {
	}

	public FilterException(String message) {
		super(message);
	}

	public FilterException(Throwable cause) {
		super(cause);
	}

	public FilterException(String message, Throwable cause) {
		super(message, cause);
	}

}
