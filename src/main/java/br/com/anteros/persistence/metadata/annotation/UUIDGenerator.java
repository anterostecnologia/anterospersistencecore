package br.com.anteros.persistence.metadata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import br.com.anteros.persistence.metadata.identifier.UUIDProviderImpl;

@Target(value = {ElementType.FIELD, ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface UUIDGenerator {
	
	Class<?> value() default UUIDProviderImpl.class;
	
}
