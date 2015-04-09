/*******************************************************************************
 * Copyright 2011, Mysema Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 *******************************************************************************/
package br.com.anteros.persistence.dsl.osql.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.anteros.core.utils.ListUtils;

/**
 * QMap represents a projection of type Map
 * 
 * <p>
 * Usage example
 * </p>
 * 
 * <pre>
 * {@code 
 * List<Map<Expression<?>,?> result = query.from(employee).list(new QMap(employee.firstName, employee.lastName));
 * for (Tuple row : result) {
 *     System.out.println("firstName " + row.get(employee.firstName));
 *     System.out.println("lastName " + row.get(employee.lastName)); 
 * }
 * }
 * </pre>
 * 
 * @author tiwe
 *
 */
public class QMap extends ExpressionBase<Map<Expression<?>, ?>> implements FactoryExpression<Map<Expression<?>, ?>> {

	private static final long serialVersionUID = -7545994090073480810L;

	private final List<Expression<?>> args;

	/**
	 * Create a new QMap instance
	 * 
	 * @param args
	 */
	public QMap(Expression<?>... args) {
		super((Class) Map.class);
		this.args = ListUtils.copyOf(args);
	}

	/**
	 * Create a new QMap instance
	 * 
	 * @param args
	 */
	public QMap(List<Expression<?>> args) {
		super((Class) Map.class);
		this.args = args;
	}

	/**
	 * Create a new QMap instance
	 * 
	 * @param args
	 */
	public QMap(Expression<?>[]... args) {
		super((Class) Map.class);
		List<Expression<?>> builder = new ArrayList<Expression<?>>();
		for (Expression<?>[] exprs : args) {
			builder.addAll(Arrays.asList(exprs));
		}
		this.args = builder;
	}

	@Override
	public <R, C> R accept(Visitor<R, C> v, C context) {
		return v.visit(this, context);
	}

	@Override
	public List<Expression<?>> getArgs() {
		return args;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof FactoryExpression) {
			FactoryExpression<?> c = (FactoryExpression<?>) obj;
			return args.equals(c.getArgs()) && getType().equals(c.getType());
		} else {
			return false;
		}
	}

	@Override
	public Map<Expression<?>, ?> newInstance(Object... args) {
		Map<Expression<?>, Object> map = new HashMap<Expression<?>, Object>();
		for (int i = 0; i < args.length; i++) {
			map.put(this.args.get(i), args[i]);
		}
		return map;
	}

}
