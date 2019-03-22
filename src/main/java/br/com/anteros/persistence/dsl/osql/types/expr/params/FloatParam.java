/*******************************************************************************
 * Copyright 2012 Anteros Tecnologia
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package br.com.anteros.persistence.dsl.osql.types.expr.params;

import br.com.anteros.persistence.dsl.osql.types.expr.Param;

/**
 * Permite uso de parâmetros Double sem a necessidade do uso de generics.
 * 
 * @author Maiko Antonio Cunha
 *
 */
public class FloatParam extends Param<Float> {

	private static final long serialVersionUID = 1L;

	public FloatParam(String name) {
		super(Float.class, name);
	}

	public FloatParam() {
		super(Float.class);
	}
}