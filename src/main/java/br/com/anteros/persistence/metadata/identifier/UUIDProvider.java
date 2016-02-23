package br.com.anteros.persistence.metadata.identifier;

import java.io.Serializable;

public interface UUIDProvider {
	
	public Serializable generateValue(Class<?> uuidType);

}
