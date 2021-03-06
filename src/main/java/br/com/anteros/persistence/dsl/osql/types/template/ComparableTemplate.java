/*******************************************************************************
 * Copyright 2011, Mysema Ltd
 *  
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 *******************************************************************************/
package br.com.anteros.persistence.dsl.osql.types.template;

import java.util.List;

import br.com.anteros.core.utils.ListUtils;
import br.com.anteros.persistence.dsl.osql.types.Template;
import br.com.anteros.persistence.dsl.osql.types.TemplateExpression;
import br.com.anteros.persistence.dsl.osql.types.TemplateExpressionImpl;
import br.com.anteros.persistence.dsl.osql.types.TemplateFactory;
import br.com.anteros.persistence.dsl.osql.types.Visitor;
import br.com.anteros.persistence.dsl.osql.types.expr.ComparableExpression;

/**
 * ComparableTemplate defines custom comparable expressions
 *
 * @author tiwe
 *
 * @param <T> expression type
 */
public class ComparableTemplate<T extends Comparable<?>> extends ComparableExpression<T> implements TemplateExpression<T> {

    private static final long serialVersionUID = -6292853402028813007L;

    public static <T extends Comparable<?>> ComparableExpression<T> create(Class<T> type, String template) {
        return new ComparableTemplate<T>(type, TemplateFactory.DEFAULT.create(template), ListUtils.of());
    }
    
    public static <T extends Comparable<?>> ComparableExpression<T> create(Class<T> type, String template, Object one) {
        return new ComparableTemplate<T>(type, TemplateFactory.DEFAULT.create(template), ListUtils.of(one));
    }
    
    public static <T extends Comparable<?>> ComparableExpression<T> create(Class<T> type, String template, Object one, Object two) {
        return new ComparableTemplate<T>(type, TemplateFactory.DEFAULT.create(template), ListUtils.of(one, two));
    }
    
    public static <T extends Comparable<?>> ComparableExpression<T> create(Class<T> type, String template, Object... args) {
        return new ComparableTemplate<T>(type, TemplateFactory.DEFAULT.create(template), ListUtils.copyOf(args));
    }

    public static <T extends Comparable<?>> ComparableExpression<T> create(Class<T> type, Template template, Object... args) {
        return new ComparableTemplate<T>(type, template, ListUtils.copyOf(args));
    }

    private final TemplateExpressionImpl<T> templateMixin;

    public ComparableTemplate(Class<T> type, Template template, List<?> args) {
        super(new TemplateExpressionImpl<T>(type, template, args));
        templateMixin = (TemplateExpressionImpl<T>)mixin;
    }

    @Override
    public final <R,C> R accept(Visitor<R,C> v, C context) {
        return v.visit(templateMixin, context);
    }
    
    @Override
    public Object getArg(int index) {
        return templateMixin.getArg(index);
    }

    @Override
    public List<?> getArgs() {
        return templateMixin.getArgs();
    }

    @Override
    public Template getTemplate() {
        return templateMixin.getTemplate();
    }

}
