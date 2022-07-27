/*******************************************************************************
 * Copyright 2012 Anteros Tecnologia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package br.com.anteros.persistence.parameter;

import br.com.anteros.persistence.dsl.osql.util.ReflectionUtils;
import br.com.anteros.persistence.metadata.descriptor.DescriptionField;
import br.com.anteros.persistence.sql.command.CommandReturn;
import br.com.anteros.persistence.sql.command.PersisterCommand;

import java.lang.reflect.Field;

/**
 * 
 * @author Edson Martins - Anteros
 *
 */
public class ExternalFileNamedParameter extends NamedParameter {

	protected String name;
	protected PersisterCommand[] commands;
	protected Object value = null;
	protected Object target = null;
	protected DescriptionField field;

	public ExternalFileNamedParameter(String name, DescriptionField field, Object target, PersisterCommand... commands) {
		super(name);
		this.name = name;
		this.commands = commands;
		this.field = field;
		this.target = target;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getValue() {
		if (value == null) {
			for (PersisterCommand command : commands) {
				if (command != null) {
					try {
						CommandReturn ret = command.execute();
						if (ret != null) {
							value = ret.getSql();
							if (target!=null){
								field.setObjectValue(target,value);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return value;
	}

	@Override
	public String toString() {
		return name + "=" + commands;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExternalFileNamedParameter other = (ExternalFileNamedParameter) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
