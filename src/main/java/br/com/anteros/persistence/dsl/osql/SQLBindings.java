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

import java.util.List;

/**
 * SQLBindings provides the SQL query string and bindings
 *
 * @author tiwe
 *
 */
public class SQLBindings {

    private final String sql;

    private final List<Object> bindings;

    public SQLBindings(String sql, List<Object> bindings) {
        this.sql = sql;
        this.bindings = bindings;
    }

    @Deprecated
    public String getSql() {
        return sql;
    }

    public String getSQL() {
        return sql;
    }

    public List<Object> getBindings() {
        return bindings;
    }

}
