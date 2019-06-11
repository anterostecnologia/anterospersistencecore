package br.com.anteros.persistence.parameter;

import br.com.anteros.persistence.metadata.annotation.type.TemporalType;

public class VersionNamedParameter extends NamedParameter {

	public VersionNamedParameter(String name, Object value, boolean key) {
		super(name, value, key);
	}

	public VersionNamedParameter(String name, Object value, TemporalType temporalType) {
		super(name, value, temporalType);
	}

	public VersionNamedParameter(String name, Object value) {
		super(name, value);
	}

	public VersionNamedParameter(String name) {
		super(name);
	}

}
