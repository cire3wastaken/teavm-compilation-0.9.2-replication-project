/*
 * Copyright (C) 2009 The Guava Authors
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
 */

package com.google.common.collect;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

/**
 * An immutable {@link SetMultimap} with reliable user-specified key and value
 * iteration order. Does not permit null keys or values.
 *
 * <p>
 * Unlike {@link Multimaps#unmodifiableSetMultimap(SetMultimap)}, which is a
 * <i>view</i> of a separate multimap which can still change, an instance of
 * {@code ImmutableSetMultimap} contains its own data and will <i>never</i>
 * change. {@code ImmutableSetMultimap} is convenient for
 * {@code public static final} multimaps ("constant multimaps") and also lets
 * you easily make a "defensive copy" of a multimap provided to your class by a
 * caller.
 *
 * <p>
 * <b>Note:</b> Although this class is not final, it cannot be subclassed as it
 * has no public or protected constructors. Thus, instances of this class are
 * guaranteed to be immutable.
 *
 * <p>
 * See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/ImmutableCollectionsExplained">
 * immutable collections</a>.
 *
 * @author Mike Ward
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible(serializable = true, emulated = true)
public class ImmutableSetMultimap<K, V> extends ImmutableMultimap<K, V> implements SetMultimap<K, V> {

	/** Returns the empty multimap. */
	// Casting is safe because the multimap will never hold any elements.
	@SuppressWarnings("unchecked")
	public static <K, V> ImmutableSetMultimap<K, V> of() {
		return (ImmutableSetMultimap<K, V>) EmptyImmutableSetMultimap.INSTANCE;
	}

	/**
	 * Returns an immutable multimap containing a single entry.
	 */
	public static <K, V> ImmutableSetMultimap<K, V> of(K k1, V v1) {
		Builder<K, V> builder = ImmutableSetMultimap.builder();
		builder.put(k1, v1);
		return builder.build();
	}

	/**
	 * Returns an immutable multimap containing the given entries, in order.
	 * Repeated occurrences of an entry (according to {@link Object#equals}) after
	 * the first are ignored.
	 */
	public static <K, V> ImmutableSetMultimap<K, V> of(K k1, V v1, K k2, V v2) {
		Builder<K, V> builder = ImmutableSetMultimap.builder();
		builder.put(k1, v1);
		builder.put(k2, v2);
		return builder.build();
	}

	/**
	 * Returns an immutable multimap containing the given entries, in order.
	 * Repeated occurrences of an entry (according to {@link Object#equals}) after
	 * the first are ignored.
	 */
	public static <K, V> ImmutableSetMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
		Builder<K, V> builder = ImmutableSetMultimap.builder();
		builder.put(k1, v1);
		builder.put(k2, v2);
		builder.put(k3, v3);
		return builder.build();
	}

	/**
	 * Returns an immutable multimap containing the given entries, in order.
	 * Repeated occurrences of an entry (according to {@link Object#equals}) after
	 * the first are ignored.
	 */
	public static <K, V> ImmutableSetMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
		Builder<K, V> builder = ImmutableSetMultimap.builder();
		builder.put(k1, v1);
		builder.put(k2, v2);
		builder.put(k3, v3);
		builder.put(k4, v4);
		return builder.build();
	}

	/**
	 * Returns an immutable multimap containing the given entries, in order.
	 * Repeated occurrences of an entry (according to {@link Object#equals}) after
	 * the first are ignored.
	 */
	public static <K, V> ImmutableSetMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
		Builder<K, V> builder = ImmutableSetMultimap.builder();
		builder.put(k1, v1);
		builder.put(k2, v2);
		builder.put(k3, v3);
		builder.put(k4, v4);
		builder.put(k5, v5);
		return builder.build();
	}

	// looking for of() with > 5 entries? Use the builder instead.

	/**
	 * Returns a new {@link Builder}.
	 */
	public static <K, V> Builder<K, V> builder() {
		return new Builder<K, V>();
	}

	/**
	 * Multimap for {@link Builder} that maintains key and
	 * value orderings and performs better than {@link LinkedHashMultimap}.
	 */
	private static class BuilderMultimap<K, V> extends AbstractMapBasedMultimap<K, V> {
		BuilderMultimap() {
			super(new LinkedHashMap<K, Collection<V>>());
		}

		@Override
		Collection<V> createCollection() {
			return Sets.newLinkedHashSet();
		}

		private static final long serialVersionUID = 0;
	}

	/**
	 * A builder for creating immutable {@code SetMultimap} instances, especially
	 * {@code public static final} multimaps ("constant multimaps"). Example:
	 * 
	 * <pre>
	 * {
	 * 	&#64;code
	 *
	 * 	static final Multimap<String, Integer> STRING_TO_INTEGER_MULTIMAP = new ImmutableSetMultimap.Builder<String, Integer>()
	 * 			.put("one", 1).putAll("several", 1, 2, 3).putAll("many", 1, 2, 3, 4, 5).build();
	 * }
	 * </pre>
	 *
	 * <p>
	 * Builder instances can be reused; it is safe to call {@link #build} multiple
	 * times to build multiple multimaps in series. Each multimap contains the
	 * key-value mappings in the previously created multimaps.
	 *
	 * @since 2.0 (imported from Google Collections Library)
	 */
	public static final class Builder<K, V> extends ImmutableMultimap.Builder<K, V> {
		/**
		 * Creates a new builder. The returned builder is equivalent to the builder
		 * generated by {@link ImmutableSetMultimap#builder}.
		 */
		public Builder() {
			builderMultimap = new BuilderMultimap<K, V>();
		}

		/**
		 * Adds a key-value mapping to the built multimap if it is not already present.
		 */
		@Override
		public Builder<K, V> put(K key, V value) {
			builderMultimap.put(checkNotNull(key), checkNotNull(value));
			return this;
		}

		/**
		 * Adds an entry to the built multimap if it is not already present.
		 *
		 * @since 11.0
		 */
		@Override
		public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
			builderMultimap.put(checkNotNull(entry.getKey()), checkNotNull(entry.getValue()));
			return this;
		}

		@Override
		public Builder<K, V> putAll(K key, Iterable<? extends V> values) {
			Collection<V> collection = builderMultimap.get(checkNotNull(key));
			for (V value : values) {
				collection.add(checkNotNull(value));
			}
			return this;
		}

		@Override
		public Builder<K, V> putAll(K key, V... values) {
			return putAll(key, Arrays.asList(values));
		}

		@Override
		public Builder<K, V> putAll(Multimap<? extends K, ? extends V> multimap) {
			for (Entry<? extends K, ? extends Collection<? extends V>> entry : multimap.asMap().entrySet()) {
				putAll(entry.getKey(), entry.getValue());
			}
			return this;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @since 8.0
		 */
		@Override
		public Builder<K, V> orderKeysBy(Comparator<? super K> keyComparator) {
			this.keyComparator = checkNotNull(keyComparator);
			return this;
		}

		/**
		 * Specifies the ordering of the generated multimap's values for each key.
		 *
		 * <p>
		 * If this method is called, the sets returned by the {@code get()} method of
		 * the generated multimap and its {@link Multimap#asMap()} view are
		 * {@link ImmutableSortedSet} instances. However, serialization does not
		 * preserve that property, though it does maintain the key and value ordering.
		 *
		 * @since 8.0
		 */
		// TODO: Make serialization behavior consistent.
		@Override
		public Builder<K, V> orderValuesBy(Comparator<? super V> valueComparator) {
			super.orderValuesBy(valueComparator);
			return this;
		}

		/**
		 * Returns a newly-created immutable set multimap.
		 */
		@Override
		public ImmutableSetMultimap<K, V> build() {
			if (keyComparator != null) {
				Multimap<K, V> sortedCopy = new BuilderMultimap<K, V>();
				List<Entry<K, Collection<V>>> entries = Lists.newArrayList(builderMultimap.asMap().entrySet());
				Collections.sort(entries, Ordering.from(keyComparator).<K>onKeys());
				for (Entry<K, Collection<V>> entry : entries) {
					sortedCopy.putAll(entry.getKey(), entry.getValue());
				}
				builderMultimap = sortedCopy;
			}
			return copyOf(builderMultimap, valueComparator);
		}
	}

	/**
	 * Returns an immutable set multimap containing the same mappings as
	 * {@code multimap}. The generated multimap's key and value orderings correspond
	 * to the iteration ordering of the {@code multimap.asMap()} view. Repeated
	 * occurrences of an entry in the multimap after the first are ignored.
	 *
	 * <p>
	 * Despite the method name, this method attempts to avoid actually copying the
	 * data when it is safe to do so. The exact circumstances under which a copy
	 * will or will not be performed are undocumented and subject to change.
	 *
	 * @throws NullPointerException if any key or value in {@code multimap} is null
	 */
	public static <K, V> ImmutableSetMultimap<K, V> copyOf(Multimap<? extends K, ? extends V> multimap) {
		return copyOf(multimap, null);
	}

	private static <K, V> ImmutableSetMultimap<K, V> copyOf(Multimap<? extends K, ? extends V> multimap,
			Comparator<? super V> valueComparator) {
		checkNotNull(multimap); // eager for GWT
		if (multimap.isEmpty() && valueComparator == null) {
			return of();
		}

		if (multimap instanceof ImmutableSetMultimap) {
			@SuppressWarnings("unchecked") // safe since multimap is not writable
			ImmutableSetMultimap<K, V> kvMultimap = (ImmutableSetMultimap<K, V>) multimap;
			if (!kvMultimap.isPartialView()) {
				return kvMultimap;
			}
		}

		ImmutableMap.Builder<K, ImmutableSet<V>> builder = ImmutableMap.builder();
		int size = 0;

		for (Entry<? extends K, ? extends Collection<? extends V>> entry : multimap.asMap().entrySet()) {
			K key = entry.getKey();
			Collection<? extends V> values = entry.getValue();
			ImmutableSet<V> set = valueSet(valueComparator, values);
			if (!set.isEmpty()) {
				builder.put(key, set);
				size += set.size();
			}
		}

		return new ImmutableSetMultimap<K, V>(builder.build(), size, valueComparator);
	}

	/**
	 * Returned by get() when a missing key is provided. Also holds the comparator,
	 * if any, used for values.
	 */
	private final transient ImmutableSet<V> emptySet;

	ImmutableSetMultimap(ImmutableMap<K, ImmutableSet<V>> map, int size,
			@Nullable Comparator<? super V> valueComparator) {
		super(map, size);
		this.emptySet = emptySet(valueComparator);
	}

	// views

	/**
	 * Returns an immutable set of the values for the given key. If no mappings in
	 * the multimap have the provided key, an empty immutable set is returned. The
	 * values are in the same order as the parameters used to build this multimap.
	 */
	@Override
	public ImmutableSet<V> get(@Nullable K key) {
		// This cast is safe as its type is known in constructor.
		ImmutableSet<V> set = (ImmutableSet<V>) map.get(key);
		return firstNonNull(set, emptySet);
	}

	private transient ImmutableSetMultimap<V, K> inverse;

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Because an inverse of a set multimap cannot contain multiple pairs with the
	 * same key and value, this method returns an {@code ImmutableSetMultimap}
	 * rather than the {@code ImmutableMultimap} specified in the {@code
	 * ImmutableMultimap} class.
	 *
	 * @since 11.0
	 */
	public ImmutableSetMultimap<V, K> inverse() {
		ImmutableSetMultimap<V, K> result = inverse;
		return (result == null) ? (inverse = invert()) : result;
	}

	private ImmutableSetMultimap<V, K> invert() {
		Builder<V, K> builder = builder();
		for (Entry<K, V> entry : entries()) {
			builder.put(entry.getValue(), entry.getKey());
		}
		ImmutableSetMultimap<V, K> invertedMultimap = builder.build();
		invertedMultimap.inverse = this;
		return invertedMultimap;
	}

	/**
	 * Guaranteed to throw an exception and leave the multimap unmodified.
	 *
	 * @throws UnsupportedOperationException always
	 * @deprecated Unsupported operation.
	 */
	@Deprecated
	@Override
	public ImmutableSet<V> removeAll(Object key) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Guaranteed to throw an exception and leave the multimap unmodified.
	 *
	 * @throws UnsupportedOperationException always
	 * @deprecated Unsupported operation.
	 */
	@Deprecated
	@Override
	public ImmutableSet<V> replaceValues(K key, Iterable<? extends V> values) {
		throw new UnsupportedOperationException();
	}

	private transient ImmutableSet<Entry<K, V>> entries;

	/**
	 * Returns an immutable collection of all key-value pairs in the multimap. Its
	 * iterator traverses the values for the first key, the values for the second
	 * key, and so on.
	 */
	@Override
	public ImmutableSet<Entry<K, V>> entries() {
		ImmutableSet<Entry<K, V>> result = entries;
		return (result == null) ? (entries = new EntrySet<K, V>(this)) : result;
	}

	private static final class EntrySet<K, V> extends ImmutableSet<Entry<K, V>> {
		private transient final ImmutableSetMultimap<K, V> multimap;

		EntrySet(ImmutableSetMultimap<K, V> multimap) {
			this.multimap = multimap;
		}

		@Override
		public boolean contains(@Nullable Object object) {
			if (object instanceof Entry) {
				Entry<?, ?> entry = (Entry<?, ?>) object;
				return multimap.containsEntry(entry.getKey(), entry.getValue());
			}
			return false;
		}

		@Override
		public int size() {
			return multimap.size();
		}

		@Override
		public UnmodifiableIterator<Entry<K, V>> iterator() {
			return multimap.entryIterator();
		}

		@Override
		boolean isPartialView() {
			return false;
		}
	}

	private static <V> ImmutableSet<V> valueSet(@Nullable Comparator<? super V> valueComparator,
			Collection<? extends V> values) {
		return (valueComparator == null) ? ImmutableSet.copyOf(values)
				: ImmutableSortedSet.copyOf(valueComparator, values);
	}

	private static <V> ImmutableSet<V> emptySet(@Nullable Comparator<? super V> valueComparator) {
		return (valueComparator == null) ? ImmutableSet.<V>of() : ImmutableSortedSet.<V>emptySet(valueComparator);
	}

	/**
	 * @serialData number of distinct keys, and then for each distinct key: the key,
	 *             the number of values for that key, and the key's values
	 */
	@GwtIncompatible("java.io.ObjectOutputStream")
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		stream.writeObject(valueComparator());
		Serialization.writeMultimap(this, stream);
	}

	@Nullable
	Comparator<? super V> valueComparator() {
		return emptySet instanceof ImmutableSortedSet ? ((ImmutableSortedSet<V>) emptySet).comparator() : null;
	}

	@GwtIncompatible("java.io.ObjectInputStream")
	// Serialization type safety is at the caller's mercy.
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		Comparator<Object> valueComparator = (Comparator<Object>) stream.readObject();
		int keyCount = stream.readInt();
		if (keyCount < 0) {
			throw new InvalidObjectException("Invalid key count " + keyCount);
		}
		ImmutableMap.Builder<Object, ImmutableSet<Object>> builder = ImmutableMap.builder();
		int tmpSize = 0;

		for (int i = 0; i < keyCount; i++) {
			Object key = stream.readObject();
			int valueCount = stream.readInt();
			if (valueCount <= 0) {
				throw new InvalidObjectException("Invalid value count " + valueCount);
			}

			Object[] array = new Object[valueCount];
			for (int j = 0; j < valueCount; j++) {
				array[j] = stream.readObject();
			}
			ImmutableSet<Object> valueSet = valueSet(valueComparator, asList(array));
			if (valueSet.size() != array.length) {
				throw new InvalidObjectException("Duplicate key-value pairs exist for key " + key);
			}
			builder.put(key, valueSet);
			tmpSize += valueCount;
		}

		ImmutableMap<Object, ImmutableSet<Object>> tmpMap;
		try {
			tmpMap = builder.build();
		} catch (IllegalArgumentException e) {
			throw (InvalidObjectException) new InvalidObjectException(e.getMessage()).initCause(e);
		}

		FieldSettersHolder.MAP_FIELD_SETTER.set(this, tmpMap);
		FieldSettersHolder.SIZE_FIELD_SETTER.set(this, tmpSize);
		FieldSettersHolder.EMPTY_SET_FIELD_SETTER.set(this, emptySet(valueComparator));
	}

	@GwtIncompatible("not needed in emulated source.")
	private static final long serialVersionUID = 0;
}
