/*******************************************************************************
 * Copyright 2011, Mysema Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 *******************************************************************************/
package br.com.anteros.persistence.dsl.osql;

import static br.com.anteros.persistence.dsl.osql.util.CollectionUtils.add;
import static br.com.anteros.persistence.dsl.osql.util.CollectionUtils.addSorted;
import static br.com.anteros.persistence.dsl.osql.util.CollectionUtils.copyOf;
import static br.com.anteros.persistence.dsl.osql.util.CollectionUtils.copyOfSorted;
import static br.com.anteros.persistence.dsl.osql.util.CollectionUtils.put;
import static br.com.anteros.persistence.dsl.osql.util.CollectionUtils.removeSorted;

import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.anteros.core.utils.ListUtils;
import br.com.anteros.core.utils.MapUtils;
import br.com.anteros.core.utils.ObjectUtils;
import br.com.anteros.core.utils.SetUtils;
import br.com.anteros.persistence.dsl.osql.types.Expression;
import br.com.anteros.persistence.dsl.osql.types.ExpressionUtils;
import br.com.anteros.persistence.dsl.osql.types.IndexHint;
import br.com.anteros.persistence.dsl.osql.types.OrderSpecifier;
import br.com.anteros.persistence.dsl.osql.types.ParamExpression;
import br.com.anteros.persistence.dsl.osql.types.ParamsVisitor;
import br.com.anteros.persistence.dsl.osql.types.Path;
import br.com.anteros.persistence.dsl.osql.types.Predicate;
import br.com.anteros.persistence.dsl.osql.types.ValidatingVisitor;

/**
 * DefaultQueryMetadata is the default implementation of the {@link QueryMetadata} interface
 *
 * @author tiwe
 */
public class DefaultQueryMetadata implements QueryMetadata, Cloneable {

	private static final long serialVersionUID = 317736313966701232L;

	private boolean distinct;

	private Set<Expression<?>> exprInJoins = SetUtils.of();

	private List<Expression<?>> groupBy = ListUtils.of();

	private Predicate having;

	private List<JoinExpression> joins = ListUtils.of();

	private Expression<?> joinTarget;

	private JoinType joinType;

	private Predicate joinCondition;

	private Set<JoinFlag> joinFlags = SetUtils.of();

	private QueryModifiers modifiers = QueryModifiers.EMPTY;

	private List<OrderSpecifier<?>> orderBy = ListUtils.of();

	private List<Expression<?>> projection = ListUtils.of();

	// NOTE : this is not necessarily serializable
	private Map<ParamExpression<?>, Object> params = MapUtils.<ParamExpression<?>, Object> of();

	private boolean unique;

	private Predicate where;

	private Set<QueryFlag> flags = SetUtils.of();

	private boolean extractParams = true;

	private boolean validate = true;

	private ValidatingVisitor validatingVisitor = ValidatingVisitor.DEFAULT;

	private List<IndexHint> indexHints;

	private static Predicate and(Predicate lhs, Predicate rhs) {
		if (lhs == null) {
			return rhs;
		} else {
			return ExpressionUtils.and(lhs, rhs);
		}
	}

	/**
	 * Create an empty DefaultQueryMetadata instance
	 */
	public DefaultQueryMetadata() {
	}

	public void setIndexHints(List<IndexHint> indexes) {
		this.indexHints = ListUtils.copyOf(indexes);
	}

	/**
	 * Disable validation
	 *
	 * @return
	 */
	public DefaultQueryMetadata noValidate() {
		validate = false;
		return this;
	}

	@Override
	public void addFlag(QueryFlag flag) {
		flags = addSorted(flags, flag);
	}

	@Override
	public void addJoinFlag(JoinFlag flag) {
		joinFlags = addSorted(joinFlags, flag);
	}

	@Override
	public void addGroupBy(Expression<?> o) {
		addLastJoin();
		validate(o);
		groupBy = add(groupBy, o);
	}

	@Override
	public void addHaving(Predicate e) {
		addLastJoin();
		if (e == null) {
			return;
		}
		e = (Predicate) ExpressionUtils.extract(e);
		if (e != null) {
			validate(e);
			having = and(having, e);
		}
	}

	private void addLastJoin() {
		if (joinTarget == null) {
			return;
		}
		joins = add(joins, new JoinExpression(joinType, joinTarget, joinCondition, joinFlags));

		joinType = null;
		joinTarget = null;
		joinCondition = null;
		joinFlags = SetUtils.of();
	}

	@Override
	public void addJoin(JoinType joinType, Expression<?> expr) {
		addLastJoin();
		if (!exprInJoins.contains(expr)) {
			if (expr instanceof Path && ((Path<?>) expr).getMetadata().isRoot()) {
				exprInJoins = add(exprInJoins, expr);
			} else {
				validate(expr);
			}
			this.joinType = joinType;
			this.joinTarget = expr;
		} else if (validate) {
			throw new IllegalStateException(expr + " is already used");
		}
	}

	@Override
	public void addJoinCondition(Predicate o) {
		validate(o);
		joinCondition = and(joinCondition, o);
	}

	@Override
	public void addOrderBy(OrderSpecifier<?> o) {
		addLastJoin();
		// order specifiers can't be validated, since they can refer to projection elements
		// that are declared later
		orderBy = add(orderBy, o);
	}

	@Override
	public void addProjection(Expression<?> o) {
		addLastJoin();
		validate(o);
		projection = add(projection, o);
	}

	@Override
	public void addWhere(Predicate e) {
		if (e == null) {
			return;
		}
		addLastJoin();
		e = (Predicate) ExpressionUtils.extract(e);
		if (e != null) {
			validate(e);
			where = and(where, e);
		}
	}

	@Override
	public void clearOrderBy() {
		orderBy = ListUtils.of();
	}

	@Override
	public void clearProjection() {
		projection = ListUtils.of();
	}

	@Override
	public void clearWhere() {
		where = new BooleanBuilder();
	}

	@Override
	public QueryMetadata clone() {
		try {
			DefaultQueryMetadata clone = (DefaultQueryMetadata) super.clone();
			clone.exprInJoins = copyOf(exprInJoins);
			clone.groupBy = copyOf(groupBy);
			clone.having = having;
			clone.joins = copyOf(joins);
			clone.modifiers = modifiers;
			clone.orderBy = copyOf(orderBy);
			clone.projection = copyOf(projection);
			clone.params = copyOf(params);
			clone.where = where;
			clone.flags = copyOfSorted(flags);
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new FilterException(e);
		}
	}

	@Override
	public List<Expression<?>> getGroupBy() {
		return groupBy;
	}

	@Override
	public Predicate getHaving() {
		return having;
	}

	@Override
	public List<JoinExpression> getJoins() {
		addLastJoin();
		return joins;
	}

	@Override
	public QueryModifiers getModifiers() {
		return modifiers;
	}

	@Override
	public Map<ParamExpression<?>, Object> getParams() {
		return params;
	}

	@Override
	public List<OrderSpecifier<?>> getOrderBy() {
		return orderBy;
	}

	@Override
	public List<Expression<?>> getProjection() {
		return projection;
	}

	@Override
	public Predicate getWhere() {
		return where;
	}

	@Override
	public boolean isDistinct() {
		return distinct;
	}

	@Override
	public boolean isUnique() {
		return unique;
	}

	@Override
	public void reset() {
		clearProjection();
		params = MapUtils.of();
		modifiers = QueryModifiers.EMPTY;
	}

	@Override
	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	@Override
	public void setLimit(Long limit) {
		if (modifiers == null || modifiers.getOffset() == null) {
			modifiers = QueryModifiers.limit(limit);
		} else {
			modifiers = new QueryModifiers(limit, modifiers.getOffset());
		}
	}

	@Override
	public void setModifiers(QueryModifiers restriction) {
		this.modifiers = restriction;
	}

	@Override
	public void setOffset(Long offset) {
		if (modifiers == null || modifiers.getLimit() == null) {
			modifiers = QueryModifiers.offset(offset);
		} else {
			modifiers = new QueryModifiers(modifiers.getLimit(), offset);
		}
	}

	@Override
	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	@Override
	public <T> void setParam(ParamExpression<T> param, T value) {
		params = put(params, param, value);
	}

	@Override
	public Set<QueryFlag> getFlags() {
		return flags;
	}

	@Override
	public boolean hasFlag(QueryFlag flag) {
		return flags.contains(flag);
	}

	@Override
	public void removeFlag(QueryFlag flag) {
		flags = removeSorted(flags, flag);
	}

	private void validate(Expression<?> expr) {
		if (extractParams) {
			expr.accept(ParamsVisitor.DEFAULT, this);
		}
		if (validate) {
			exprInJoins = expr.accept(validatingVisitor, exprInJoins);
		}
	}

	@Override
	public void setValidate(boolean v) {
		this.validate = v;
	}

	public void setValidatingVisitor(ValidatingVisitor visitor) {
		this.validatingVisitor = visitor;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof QueryMetadata) {
			QueryMetadata q = (QueryMetadata) o;
			return q.getFlags().equals(flags) && q.getGroupBy().equals(groupBy) && ObjectUtils.equal(q.getHaving(), having) && q.isDistinct() == distinct
					&& q.isUnique() == unique && q.getJoins().equals(joins) && ObjectUtils.equal(q.getModifiers(), modifiers) && q.getOrderBy().equals(orderBy)
					&& q.getParams().equals(params) && q.getProjection().equals(projection) && ObjectUtils.equal(q.getWhere(), where);

		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return ObjectUtils.hashCode(flags, groupBy, having, joins, modifiers, orderBy, params, projection, unique, where);
	}

	@Override
	public List<IndexHint> getIndexHints() {
		return indexHints;
	}

}
