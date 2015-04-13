package br.com.anteros.persistence.metadata.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import br.com.anteros.persistence.metadata.annotation.type.CascadeType;
import br.com.anteros.persistence.metadata.annotation.type.FetchType;

@Target({ FIELD })
@Retention(RUNTIME)
public @interface OneToMany {

	CascadeType[] cascade() default {};

	FetchType fetch() default FetchType.LAZY;

	boolean optional() default true;

	String mappedBy() default "";

	boolean orphanRemoval() default false;
}
