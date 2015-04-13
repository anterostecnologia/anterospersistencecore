package br.com.anteros.persistence.metadata.configuration;

public class FieldConfigurationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public FieldConfigurationException() {
	}

	public FieldConfigurationException(String message) {
		super(message);
	}

	public FieldConfigurationException(Throwable cause) {
		super(cause);
	}

	public FieldConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public FieldConfigurationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
