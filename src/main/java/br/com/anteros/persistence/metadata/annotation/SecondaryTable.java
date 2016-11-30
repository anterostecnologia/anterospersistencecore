package br.com.anteros.persistence.metadata.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface SecondaryTable {

	String catalog() default "";

	String schema() default "";

	PrimaryKeyJoinColumn[] pkJoinColumns();
	

	@Target({ FIELD, TYPE })
	@Retention(RUNTIME)
	@Documented
	public @interface List {
		SecondaryTable[] value();
	}
}
