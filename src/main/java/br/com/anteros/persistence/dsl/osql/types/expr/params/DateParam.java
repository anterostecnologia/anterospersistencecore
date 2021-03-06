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

import java.util.Date;

import br.com.anteros.persistence.dsl.osql.types.ConstantImpl;
import br.com.anteros.persistence.dsl.osql.types.Expression;
import br.com.anteros.persistence.dsl.osql.types.Ops;
import br.com.anteros.persistence.dsl.osql.types.expr.BooleanExpression;
import br.com.anteros.persistence.dsl.osql.types.expr.BooleanOperation;
import br.com.anteros.persistence.dsl.osql.types.expr.Param;

/**
 * Permite uso de parâmetros Date sem a necessidade do uso de genérics.
 * 
 * @author Maiko Antonio Cunha
 *
 */
public class DateParam extends Param<Date> {

	private static final long serialVersionUID = 1L;

	public DateParam(String name) {
		super(Date.class, name);
	}

	public DateParam() {
		super(Date.class);
	}
	
	
}