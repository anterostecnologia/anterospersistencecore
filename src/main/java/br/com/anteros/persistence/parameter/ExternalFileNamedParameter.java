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

import br.com.anteros.persistence.sql.command.CommandReturn;
import br.com.anteros.persistence.sql.command.PersisterCommand;

/**
 * 
 * @author Edson Martins - Anteros
 *
 */
public class ExternalFileNamedParameter extends NamedParameter {

	protected String name;
	protected PersisterCommand[] commands;
	protected Object value = null;

	public ExternalFileNamedParameter(String name, PersisterCommand... commands) {
		super(name);
		this.name = name;
		this.commands = commands;
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
