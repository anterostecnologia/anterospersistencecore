package br.com.anteros.persistence.metadata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface PrimaryKeyJoinColumns {

	PrimaryKeyJoinColumn[] value();

	String foreignKeyName() default "";

}
