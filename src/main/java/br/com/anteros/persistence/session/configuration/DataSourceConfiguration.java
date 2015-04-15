/*******************************************************************************
 * Copyright 2012 Anteros Tecnologia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package br.com.anteros.persistence.session.configuration;

import java.util.ArrayList;
import java.util.List;

public class DataSourceConfiguration {

	protected String id;

	protected String clazz;
	
	public DataSourceConfiguration(String id, String clazz){
		this.id = id;
		this.clazz = clazz;
	}

	private List<PropertyConfiguration> properties;

	public List<PropertyConfiguration> getProperties() {
		if (properties == null) {
			properties = new ArrayList<PropertyConfiguration>();
		}
		return this.properties;
	}

	public String getClazz() {
		return clazz;
	}

	public void setClazz(String value) {
		this.clazz = value;
	}

	public String getId() {
		return id;
	}

	public void setId(String value) {
		this.id = value;
	}

	public String getProperty(String name) {
		for (PropertyConfiguration prop : properties) {
			if (prop.getName().equals(name))
				return prop.getValue();
		}
		return null;
	}

	@Override
	public String toString() {
		return "DataSourceConfiguration [id=" + id + ", clazz=" + clazz + ", properties=" + properties + "]";
	}

}
