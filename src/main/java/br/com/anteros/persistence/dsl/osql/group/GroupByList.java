/*******************************************************************************
 * Copyright 2011, Mysema Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 *******************************************************************************/
package br.com.anteros.persistence.dsl.osql.group;

import java.util.ArrayList;
import java.util.List;

import br.com.anteros.core.utils.ObjectUtils;
import br.com.anteros.persistence.dsl.osql.Projectable;
import br.com.anteros.persistence.dsl.osql.Tuple;
import br.com.anteros.persistence.dsl.osql.lang.CloseableIterator;
import br.com.anteros.persistence.dsl.osql.types.Expression;
import br.com.anteros.persistence.dsl.osql.types.FactoryExpression;
import br.com.anteros.persistence.dsl.osql.types.FactoryExpressionUtils;
import br.com.anteros.persistence.dsl.osql.types.QTuple;

/**
 * Provides aggregated results as a list
 *
 * @author tiwe
 *
 * @param <K>
 * @param <V>
 */
public class GroupByList<K, V> extends AbstractGroupByTransformer<K, List<V>> {

	GroupByList(Expression<K> key, Expression<?>... expressions) {
		super(key, expressions);
	}

	@Override
	public List<V> transform(Projectable projectable) {
		// create groups
		FactoryExpression<Tuple> expr = FactoryExpressionUtils.wrap(new QTuple(expressions));
		boolean hasGroups = false;
		for (Expression<?> e : expr.getArgs()) {
			hasGroups |= e instanceof GroupExpression;
		}
		if (hasGroups) {
			expr = withoutGroupExpressions(expr);
		}
		final CloseableIterator<Tuple> iter = projectable.iterate(expr);

		List<V> list = new ArrayList<V>();
		GroupImpl group = null;
		K groupId = null;
		while (iter.hasNext()) {
			Object[] row = iter.next().toArray();
			if (group == null) {
				group = new GroupImpl(groupExpressions, maps);
				groupId = (K) row[0];
			} else if (!ObjectUtils.equal(groupId, row[0])) {
				list.add(transform(group));
				group = new GroupImpl(groupExpressions, maps);
				groupId = (K) row[0];
			}
			group.add(row);
		}
		if (group != null) {
			list.add(transform(group));
		}
		iter.close();
		return list;
	}

	protected V transform(Group group) {
		return (V) group;
	}
}
