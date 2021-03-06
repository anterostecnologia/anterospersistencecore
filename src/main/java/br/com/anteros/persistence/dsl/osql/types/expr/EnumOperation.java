/*******************************************************************************
 * Copyright 2011, Mysema Ltd
 *  
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 *******************************************************************************/
package br.com.anteros.persistence.dsl.osql.types.expr;

import java.util.List;

import br.com.anteros.core.utils.ListUtils;
import br.com.anteros.persistence.dsl.osql.types.Expression;
import br.com.anteros.persistence.dsl.osql.types.Operation;
import br.com.anteros.persistence.dsl.osql.types.OperationImpl;
import br.com.anteros.persistence.dsl.osql.types.Operator;
import br.com.anteros.persistence.dsl.osql.types.Visitor;

/**
 * EnumOperation represents enum operations
 *
 * @author tiwe
 *
 * @param <T> expression type
 */
public class EnumOperation<T extends Enum<T>> extends EnumExpression<T> implements Operation<T> {

    private static final long serialVersionUID = -3593040852095778453L;

    public static <D extends Enum<D>> EnumExpression<D> create(Class<? extends D> type, Operator<? super D> op, Expression<?> one) {
        return new EnumOperation<D>(type, op, ListUtils.<Expression<?>>of(one));
    }
    
    public static <D extends Enum<D>> EnumExpression<D> create(Class<? extends D> type, Operator<? super D> op, Expression<?> one, Expression<?> two) {
        return new EnumOperation<D>(type, op, ListUtils.<Expression<?>>of(one, two));
    }
    
    public static <D extends Enum<D>> EnumExpression<D> create(Class<? extends D> type, Operator<? super D> op, Expression<?>... args) {
        return new EnumOperation<D>(type, op, args);
    }

    private final OperationImpl<T> opMixin;

    protected EnumOperation(Class<? extends T> type, Operator<? super T> op, Expression<?>... args) {
        this(type, op, ListUtils.copyOf(args));
    }

    protected EnumOperation(Class<? extends T> type, Operator<? super T> op, List<Expression<?>> args) {
        super(new OperationImpl<T>(type, op, args));
        this.opMixin = (OperationImpl<T>)mixin;
    }
    
    @Override
    public final <R,C> R accept(Visitor<R,C> v, C context) {
        return v.visit(opMixin, context);
    }

    @Override
    public Expression<?> getArg(int index) {
        return opMixin.getArg(index);
    }

    @Override
    public List<Expression<?>> getArgs() {
        return opMixin.getArgs();
    }

    @Override
    public Operator<? super T> getOperator() {
        return opMixin.getOperator();
    }

}
