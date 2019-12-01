package br.com.anteros.persistence.metadata.annotation;


import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Especifica as classes de ouvinte de retorno de chamada a serem usadas para um
 * entidade ou superclasse mapeada. Esta anotação pode ser aplicada
 * para uma classe de entidade ou superclasse mapeada.
 *
 * @author edsonmartins
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface EntityListeners {

    Class<?>[] value();
}