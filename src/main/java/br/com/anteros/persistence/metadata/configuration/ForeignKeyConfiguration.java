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
package br.com.anteros.persistence.metadata.configuration;

import br.com.anteros.persistence.metadata.annotation.ForeignKey;
import br.com.anteros.persistence.metadata.annotation.ManyToOne;
import br.com.anteros.persistence.metadata.annotation.OneToOne;
import br.com.anteros.persistence.metadata.annotation.type.FetchMode;
import br.com.anteros.persistence.metadata.annotation.type.FetchType;

public class ForeignKeyConfiguration {

	private String statement = "";
	private FetchType type = FetchType.EAGER;
	private FetchMode mode = FetchMode.FOREIGN_KEY;
	private String mappedBy = "";
	private String name;
	private boolean optional = true;
	private boolean orphanRemoval = false;

	public ForeignKeyConfiguration() {
	}

	public ForeignKeyConfiguration(String statement, FetchType type, FetchMode mode, String mappedBy) {
		this.statement = statement;
		this.type = type;
		this.mode = mode;
		this.mappedBy = mappedBy;
	}

	public ForeignKeyConfiguration(ForeignKey foreignKey) {
		this.statement = foreignKey.statement();
		this.type = foreignKey.type();
		this.mode = foreignKey.mode();
		this.mappedBy = foreignKey.mappedBy();
		this.name = foreignKey.name();
		this.optional = foreignKey.optional();
		this.orphanRemoval = foreignKey.orphanRemoval();
	}

	public ForeignKeyConfiguration(ManyToOne manyToOne) {
		this.type = manyToOne.fetch();
		this.mode = FetchMode.FOREIGN_KEY;
		this.optional = manyToOne.optional();
	}

	public ForeignKeyConfiguration(OneToOne oneToOne) {
		this.type = oneToOne.fetch();
		this.mode = FetchMode.FOREIGN_KEY;
		this.optional = oneToOne.optional();
	}

	public String getStatement() {
		return statement;
	}

	public FetchType getType() {
		return type;
	}

	public FetchMode getMode() {
		return mode;
	}

	public String getMappedBy() {
		return mappedBy;
	}

	public ForeignKeyConfiguration statement(String statement) {
		this.statement = statement;
		return this;
	}

	public ForeignKeyConfiguration type(FetchType type) {
		this.type = type;
		return this;
	}

	public ForeignKeyConfiguration mode(FetchMode mode) {
		this.mode = mode;
		return this;
	}

	public ForeignKeyConfiguration mappedBy(String mappedBy) {
		this.mappedBy = mappedBy;
		return this;
	}

	public String getName() {
		return name;
	}

	public ForeignKeyConfiguration name(String name) {
		this.name = name;
		return this;
	}

	public boolean isOptional() {
		return optional;
	}

	public ForeignKeyConfiguration optional(boolean optional) {
		this.optional = optional;
		return this;
	}

	public boolean isOrphanRemoval() {
		return orphanRemoval;
	}

	public ForeignKeyConfiguration orphanRemoval(boolean orphanRemoval) {
		this.orphanRemoval = orphanRemoval;
		return this;
	}
}
