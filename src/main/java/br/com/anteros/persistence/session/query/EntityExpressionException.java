package br.com.anteros.persistence.session.query;

public class EntityExpressionException extends Exception {

	public EntityExpressionException() {
	}

	public EntityExpressionException(String message) {
		super(message);
	}

	public EntityExpressionException(Throwable cause) {
		super(cause);
	}

	public EntityExpressionException(String message, Throwable cause) {
		super(message, cause);
	}

	public EntityExpressionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
