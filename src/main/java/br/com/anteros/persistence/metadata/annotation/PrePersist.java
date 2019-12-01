package br.com.anteros.persistence.metadata.annotation;


import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Especifica um método de retorno de chamada para o correspondente
 * evento do ciclo de vida. Esta anotação pode ser aplicada a métodos
 * de uma classe de entidade, uma superclasse mapeada ou um retorno de chamada
 * classe ouvinte.
 *
 * @author edsonmartins
 */
@Target({METHOD}) 
@Retention(RUNTIME)

public @interface PrePersist {}