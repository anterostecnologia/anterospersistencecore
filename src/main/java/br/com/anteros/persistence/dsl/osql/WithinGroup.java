/*
 * Copyright 2013, Mysema Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.anteros.persistence.dsl.osql;

import java.util.ArrayList;
import java.util.List;

import br.com.anteros.core.utils.ListUtils;
import br.com.anteros.persistence.dsl.osql.types.Expression;
import br.com.anteros.persistence.dsl.osql.types.ExpressionUtils;
import br.com.anteros.persistence.dsl.osql.types.MutableExpressionBase;
import br.com.anteros.persistence.dsl.osql.types.Operator;
import br.com.anteros.persistence.dsl.osql.types.OrderSpecifier;
import br.com.anteros.persistence.dsl.osql.types.TemplateFactory;
import br.com.anteros.persistence.dsl.osql.types.Visitor;
import br.com.anteros.persistence.dsl.osql.types.expr.ComparableExpressionBase;
import br.com.anteros.persistence.dsl.osql.types.expr.SimpleExpression;
import br.com.anteros.persistence.dsl.osql.types.expr.SimpleOperation;
import br.com.anteros.persistence.dsl.osql.types.template.SimpleTemplate;

/**
 * @author tiwe
 *
 * @param <T>
 */
public class WithinGroup<T> extends SimpleOperation<T> {

    private static final long serialVersionUID = 464583892898579544L;

    private static Expression<?> merge(Expression<?>... args) {
        if (args.length == 1) {
            return args[0];
        } else {
            return ExpressionUtils.list(Object.class, args);
        }
    }

    public class OrderBy extends MutableExpressionBase<T> {

        private static final long serialVersionUID = -4936481493030913621L;

        private static final String ORDER_BY = "order by ";

        private volatile SimpleExpression<T> value;

        private final List<OrderSpecifier<?>> orderBy = new ArrayList<OrderSpecifier<?>>();

        public OrderBy() {
            super(WithinGroup.this.getType());
        }

        public SimpleExpression<T> getValue() {
            if (value == null) {
                int size = 0;
                List<Expression<?>> args = new ArrayList<Expression<?>>();
                StringBuilder builder = new StringBuilder();
                builder.append("{0} within group (");
                args.add(WithinGroup.this);
                size++;
                if (!orderBy.isEmpty()) {
                    builder.append(ORDER_BY);
                    boolean first = true;
                    for (OrderSpecifier<?> expr : orderBy) {
                        if (!first) {
                            builder.append(", ");
                        }
                        builder.append("{" + size + "}");
                        if (!expr.isAscending()) {
                            builder.append(" desc");
                        }
                        args.add(expr.getTarget());
                        size++;
                        first = false;
                    }
                }
                builder.append(")");
                value = new SimpleTemplate<T>(
                        WithinGroup.this.getType(),
                        TemplateFactory.DEFAULT.create(builder.toString()),
                        args);
            }
            return value;
        }

        @Override
        public <R, C> R accept(Visitor<R, C> v, C context) {
            return getValue().accept(v, context);
        }

        public OrderBy orderBy(ComparableExpressionBase<?> orderBy) {
            value = null;
            this.orderBy.add(orderBy.asc());
            return this;
        }

        public OrderBy orderBy(ComparableExpressionBase<?>... orderBy) {
            value = null;
            for (ComparableExpressionBase<?> e : orderBy) {
                this.orderBy.add(e.asc());
            }
            return this;
        }
    }

    public WithinGroup(Class<T> type, Operator op) {
        super(type, op, ListUtils.<Expression<?>>of());
    }

    public WithinGroup(Class<T> type, Operator op, Expression<?> arg) {
        super(type, op, ListUtils.<Expression<?>>of(arg));
    }

    public WithinGroup(Class<T> type, Operator op, Expression<?> arg1, Expression<?> arg2) {
        super(type, op, ListUtils.<Expression<?>>of(arg1, arg2));
    }

    public WithinGroup(Class<T> type, Operator op, Expression<?>... args) {
        super(type, op, merge(args));
    }

    /**
     * @return
     */
    public OrderBy withinGroup() {
        return new OrderBy();
    }

}
