/*******************************************************************************
 * Copyright 2011, Mysema Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 *******************************************************************************/
package br.com.anteros.persistence.dsl.osql.types.path;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import br.com.anteros.core.utils.ListUtils;

/**
 * PathInits defines path initializations that can be attached to properties via QueryInit annotations
 *
 * @author tiwe
 *
 */
@SuppressWarnings("unchecked")
public class PathInits implements Serializable {

	private static final long serialVersionUID = -2173980858324141095L;

	public static final PathInits DEFAULT = new PathInits();

	public static final PathInits DIRECT = new PathInits("*");

	public static final PathInits DIRECT2 = new PathInits("*.*");

	private final boolean initAllProps;

	private final PathInits defaultValue;

	private final Map<String, PathInits> propertyToInits = new HashMap<String, PathInits>();

	public PathInits(String... initStrs) {
		boolean _initAllProps = false;
		PathInits _defaultValue = DEFAULT;

		Map<String, Collection<String>> properties = new HashMap<String, Collection<String>>();
		for (String initStr : initStrs) {
			if (initStr.equals("*")) {
				_initAllProps = true;
			} else if (initStr.startsWith("*.")) {
				_initAllProps = true;
				_defaultValue = new PathInits(initStr.substring(2));
			} else {
				String key = initStr;
				List<String> inits = Collections.emptyList();
				if (initStr.contains(".")) {
					key = initStr.substring(0, initStr.indexOf('.'));
					inits = ListUtils.of(initStr.substring(key.length() + 1));
				}
				Collection<String> values = properties.get(key);
				if (values == null) {
					values = new ArrayList<String>();
					properties.put(key, values);
				}
				values.addAll(inits);
			}
		}

		for (Map.Entry<String, Collection<String>> entry : properties.entrySet()) {
			PathInits inits = new PathInits(PathInits.toArray(entry.getValue(), String.class));
			propertyToInits.put(entry.getKey(), inits);
		}

		initAllProps = _initAllProps;
		defaultValue = _defaultValue;
	}

	private static <T> T[] toArray(Iterable<? extends T> iterable, Class<T> type) {
		Collection<? extends T> collection = PathInits.toCollection(iterable);
		T[] array = PathInits.newArray(type, collection.size());
		return collection.toArray(array);
	}

	private static <E> Collection<E> toCollection(Iterable<E> iterable) {
		if (iterable instanceof Collection)
			return (Collection<E>) iterable;
		Collection<E> result = new ArrayList<E>();
		Iterator<E> it = iterable.iterator();
		while (it.hasNext())
			result.add(it.next());
		return result;
	}

	
	private static <T> T[] newArray(Class<T> type, int length) {
		return (T[]) Array.newInstance(type, length);
	}

	public PathInits get(String property) {
		if (propertyToInits.containsKey(property)) {
			return propertyToInits.get(property);
		} else if (initAllProps) {
			return defaultValue;
		} else {
			throw new IllegalArgumentException(property + " is not initialized");
		}
	}

	public boolean isInitialized(String property) {
		return initAllProps || propertyToInits.containsKey(property);
	}

}
