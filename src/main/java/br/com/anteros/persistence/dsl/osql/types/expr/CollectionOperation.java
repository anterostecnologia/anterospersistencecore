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

import java.util.Collection;
import java.util.List;

import br.com.anteros.core.utils.ListUtils;
import br.com.anteros.persistence.dsl.osql.types.Expression;
import br.com.anteros.persistence.dsl.osql.types.OperationImpl;
import br.com.anteros.persistence.dsl.osql.types.Operator;
import br.com.anteros.persistence.dsl.osql.types.Visitor;

/**
 * @author tiwe
 *
 */
public class CollectionOperation<E> extends CollectionExpressionBase<Collection<E>, E> {

    private static final long serialVersionUID = 3154315192589335574L;

    private final Class<E> elementType;

    private final OperationImpl<Collection<E>> opMixin;

    public static <E> CollectionOperation<E> create(Operator<?> op, Class<E> type, Expression<?> one) {
        return new CollectionOperation<E>(op, type, ListUtils.<Expression<?>>of(one));
    }

    @SuppressWarnings("unchecked")
	public static <E> CollectionOperation<E> create(Operator<?> op, Class<E> type, Expression<?> one, Expression<?> two) {
        return new CollectionOperation<E>(op, type, ListUtils.of(one, two));
    }

    public static <E> CollectionOperation<E> create(Operator<?> op, Class<E> type, Expression<?>... args) {
        return new CollectionOperation<E>(op, type, args);
    }

    public CollectionOperation(Operator<?> op, Class<? super E> type, Expression<?>... args) {
        this(op, type, ListUtils.copyOf(args));
    }

    public CollectionOperation(Operator<?> op, Class<? super E> type, List<Expression<?>> args) {
        super(new OperationImpl(Collection.class, op, args));
        this.opMixin = (OperationImpl)super.mixin;
        this.elementType = (Class<E>)type;
    }

    @Override
    public Class<?> getParameter(int index) {
        if (index == 0) {
            return elementType;
        } else {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
    }

    @Override
    
    public <R, C> R accept(Visitor<R, C> v, C context) {
        return opMixin.accept(v, context);
    }

    @Override
    public Class<E> getElementType() {
        return elementType;
    }

}
