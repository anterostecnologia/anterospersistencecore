/*******************************************************************************
 * Copyright 2011, Mysema Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 *******************************************************************************/
package br.com.anteros.persistence.dsl.osql.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.anteros.core.utils.ListUtils;
import br.com.anteros.core.utils.SetUtils;

/**
 * CollectionUtils provides addition operations for Collection types that provide an immutable type for single item
 * collections and after that mutable instances
 *
 * @author tiwe
 *
 */
@SuppressWarnings("unchecked")
public final class CollectionUtils {

	public static <T> List<T> add(List<T> list, T element) {
		final int size = list.size();
		if (size == 0) {
			return ListUtils.of(element);
		}
		list.add(element);
		return list;
	}

	public static <T> List<T> copyOf(List<T> list) {
		return new ArrayList<T>(list);
	}

	public static <T> Set<T> add(Set<T> set, T element) {
		final int size = set.size();
		if (size == 0) {
			return SetUtils.of(element);
		}
		set.add(element);
		return set;
	}

	public static <T> Set<T> copyOf(Set<T> set) {
		return new HashSet<T>(set);
	}

	public static <T> Set<T> addSorted(Set<T> set, T element) {
		final int size = set.size();
		if (size == 0) {
			return SetUtils.of(element);
		}
		set.add(element);
		return set;
	}

	public static <T> Set<T> removeSorted(Set<T> set, T element) {
		final int size = set.size();
		if (size == 0 || (size == 1 && set.contains(element))) {
			return SetUtils.of();
		} else {
			set.remove(element);
		}
		return set;
	}

	public static <T> Set<T> copyOfSorted(Set<T> set) {
		return new HashSet<T>(set);
	}

	public static <K, V> Map<K, V> put(Map<K, V> map, K key, V value) {
		map.put(key, value);
		return map;
	}

	public static <K, V> Map<K, V> copyOf(Map<K, V> map) {
		return new HashMap<K, V>(map);
	}

	private CollectionUtils() {
	}

}
