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
package br.com.anteros.persistence.session.impl;

import java.util.HashSet;
import java.util.Set;

import br.com.anteros.core.log.Logger;
import br.com.anteros.core.log.LoggerProvider;
import br.com.anteros.persistence.session.SQLSessionValidator;
import br.com.anteros.persistence.util.AnterosBeanValidationHelper;
import br.com.anteros.validation.api.ConstraintViolation;
import br.com.anteros.validation.api.ConstraintViolationException;
import br.com.anteros.validation.api.Validator;

public class SQLSessionValidatorImpl implements SQLSessionValidator {
	
	private Logger LOG = LoggerProvider.getInstance().getLogger(SQLSessionValidator.class);

	@Override
	public void validateBean(Object object) throws Exception {
		Validator validator = AnterosBeanValidationHelper.getBeanValidator();
		final Set<ConstraintViolation<Object>> constraintViolations = validator.validate(object);
			
		this.validate(constraintViolations);
	}

	@Override
	public void validateBean(Object object, Class<?>... groups) {
		Validator validator = AnterosBeanValidationHelper.getBeanValidator();
		final Set<ConstraintViolation<Object>> constraintViolations = validator.validate(object, groups);

		this.validate(constraintViolations);
	}
		
	private void validate(final Set<ConstraintViolation<Object>> constraintViolations)  {
		if (constraintViolations.size() > 0) {
			Set<ConstraintViolation<?>> propagatedViolations = new HashSet<ConstraintViolation<?>>(
					constraintViolations.size());
			Set<String> classNames = new HashSet<String>();
			for (ConstraintViolation<?> violation : constraintViolations) {
				LOG.debug(violation);
				propagatedViolations.add(violation);
				classNames.add(violation.getLeafBean().getClass().getName());
			}
			StringBuilder builder = new StringBuilder();
			builder.append("Validation failed for classes [");
			builder.append(classNames);
			builder.append("] during saving object.");
			builder.append("\nList of constraint violations:[\n");
			for (ConstraintViolation<?> violation : constraintViolations) {
				builder.append("\t").append(violation.toString()).append("\n");
			}
			builder.append("]");

			throw new ConstraintViolationException(builder.toString(), propagatedViolations);
		}
	}
	


}
