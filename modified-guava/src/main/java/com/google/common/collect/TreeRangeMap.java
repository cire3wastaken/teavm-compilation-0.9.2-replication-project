/*
 * Copyright (C) 2012 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.compose;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;

/**
 * An implementation of {@code RangeMap} based on a {@code TreeMap}, supporting
 * all optional operations.
 *
 * <p>
 * Like all {@code RangeMap} implementations, this supports neither null keys
 * nor null values.
 *
 * @author Louis Wasserman
 * @since 14.0
 */
@Beta
@GwtIncompatible("NavigableMap")
public final class TreeRangeMap<K extends Comparable, V> implements RangeMap<K, V> {

	private final NavigableMap<Cut<K>, RangeMapEntry<K, V>> entriesByLowerBound;

	public static <K extends Comparable, V> TreeRangeMap<K, V> create() {
		return new TreeRangeMap<K, V>();
	}

	private TreeRangeMap() {
		this.entriesByLowerBound = Maps.newTreeMap();
	}

	private static final class RangeMapEntry<K extends Comparable, V> extends AbstractMapEntry<Range<K>, V> {
		private final Range<K> range;
		private final V value;

		RangeMapEntry(Cut<K> lowerBound, Cut<K> upperBound, V value) {
			this(Range.create(lowerBound, upperBound), value);
		}

		RangeMapEntry(Range<K> range, V value) {
			this.range = range;
			this.value = value;
		}

		@Override
		public Range<K> getKey() {
			return range;
		}

		@Override
		public V getValue() {
			return value;
		}

		public boolean contains(K value) {
			return range.contains(value);
		}

		Cut<K> getLowerBound() {
			return range.lowerBound;
		}

		Cut<K> getUpperBound() {
			return range.upperBound;
		}
	}

	@Override
	@Nullable
	public V get(K key) {
		Entry<Range<K>, V> entry = getEntry(key);
		return (entry == null) ? null : entry.getValue();
	}

	@Override
	@Nullable
	public Entry<Range<K>, V> getEntry(K key) {
		Entry<Cut<K>, RangeMapEntry<K, V>> mapEntry = entriesByLowerBound.floorEntry(Cut.belowValue(key));
		if (mapEntry != null && mapEntry.getValue().contains(key)) {
			return mapEntry.getValue();
		} else {
			return null;
		}
	}

	@Override
	public void put(Range<K> range, V value) {
		if (!range.isEmpty()) {
			checkNotNull(value);
			remove(range);
			entriesByLowerBound.put(range.lowerBound, new RangeMapEntry<K, V>(range, value));
		}
	}

	@Override
	public void putAll(RangeMap<K, V> rangeMap) {
		for (Entry<Range<K>, V> entry : rangeMap.asMapOfRanges().entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void clear() {
		entriesByLowerBound.clear();
	}

	@Override
	public Range<K> span() {
		Entry<Cut<K>, RangeMapEntry<K, V>> firstEntry = entriesByLowerBound.firstEntry();
		Entry<Cut<K>, RangeMapEntry<K, V>> lastEntry = entriesByLowerBound.lastEntry();
		if (firstEntry == null) {
			throw new NoSuchElementException();
		}
		return Range.create(firstEntry.getValue().getKey().lowerBound, lastEntry.getValue().getKey().upperBound);
	}

	private void putRangeMapEntry(Cut<K> lowerBound, Cut<K> upperBound, V value) {
		entriesByLowerBound.put(lowerBound, new RangeMapEntry<K, V>(lowerBound, upperBound, value));
	}

	@Override
	public void remove(Range<K> rangeToRemove) {
		if (rangeToRemove.isEmpty()) {
			return;
		}

		/*
		 * The comments for this method will use [ ] to indicate the bounds of
		 * rangeToRemove and ( ) to indicate the bounds of ranges in the range map.
		 */
		Entry<Cut<K>, RangeMapEntry<K, V>> mapEntryBelowToTruncate = entriesByLowerBound
				.lowerEntry(rangeToRemove.lowerBound);
		if (mapEntryBelowToTruncate != null) {
			// we know ( [
			RangeMapEntry<K, V> rangeMapEntry = mapEntryBelowToTruncate.getValue();
			if (rangeMapEntry.getUpperBound().compareTo(rangeToRemove.lowerBound) > 0) {
				// we know ( [ )
				if (rangeMapEntry.getUpperBound().compareTo(rangeToRemove.upperBound) > 0) {
					// we know ( [ ] ), so insert the range ] ) back into the map --
					// it's being split apart
					putRangeMapEntry(rangeToRemove.upperBound, rangeMapEntry.getUpperBound(),
							mapEntryBelowToTruncate.getValue().getValue());
				}
				// overwrite mapEntryToTruncateBelow with a truncated range
				putRangeMapEntry(rangeMapEntry.getLowerBound(), rangeToRemove.lowerBound,
						mapEntryBelowToTruncate.getValue().getValue());
			}
		}

		Entry<Cut<K>, RangeMapEntry<K, V>> mapEntryAboveToTruncate = entriesByLowerBound
				.lowerEntry(rangeToRemove.upperBound);
		if (mapEntryAboveToTruncate != null) {
			// we know ( ]
			RangeMapEntry<K, V> rangeMapEntry = mapEntryAboveToTruncate.getValue();
			if (rangeMapEntry.getUpperBound().compareTo(rangeToRemove.upperBound) > 0) {
				// we know ( ] ), and since we dealt with truncating below already,
				// we know [ ( ] )
				putRangeMapEntry(rangeToRemove.upperBound, rangeMapEntry.getUpperBound(),
						mapEntryAboveToTruncate.getValue().getValue());
				entriesByLowerBound.remove(rangeToRemove.lowerBound);
			}
		}
		entriesByLowerBound.subMap(rangeToRemove.lowerBound, rangeToRemove.upperBound).clear();
	}

	@Override
	public Map<Range<K>, V> asMapOfRanges() {
		return new AsMapOfRanges();
	}

	private final class AsMapOfRanges extends AbstractMap<Range<K>, V> {

		@Override
		public boolean containsKey(@Nullable Object key) {
			return get(key) != null;
		}

		@Override
		public V get(@Nullable Object key) {
			if (key instanceof Range) {
				Range<?> range = (Range<?>) key;
				RangeMapEntry<K, V> rangeMapEntry = entriesByLowerBound.get(range.lowerBound);
				if (rangeMapEntry != null && rangeMapEntry.getKey().equals(range)) {
					return rangeMapEntry.getValue();
				}
			}
			return null;
		}

		@Override
		public Set<Entry<Range<K>, V>> entrySet() {
			return new AbstractSet<Entry<Range<K>, V>>() {

				@SuppressWarnings("unchecked") // it's safe to upcast iterators
				@Override
				public Iterator<Entry<Range<K>, V>> iterator() {
					return (Iterator) entriesByLowerBound.values().iterator();
				}

				@Override
				public int size() {
					return entriesByLowerBound.size();
				}
			};
		}
	}

	@Override
	public RangeMap<K, V> subRangeMap(Range<K> subRange) {
		if (subRange.equals(Range.all())) {
			return this;
		} else {
			return new SubRangeMap(subRange);
		}
	}

	@SuppressWarnings("unchecked")
	private RangeMap<K, V> emptySubRangeMap() {
		return EMPTY_SUB_RANGE_MAP;
	}

	private static final RangeMap EMPTY_SUB_RANGE_MAP = new RangeMap() {
		@Override
		@Nullable
		public Object get(Comparable key) {
			return null;
		}

		@Override
		@Nullable
		public Entry<Range, Object> getEntry(Comparable key) {
			return null;
		}

		@Override
		public Range span() {
			throw new NoSuchElementException();
		}

		@Override
		public void put(Range range, Object value) {
			checkNotNull(range);
			throw new IllegalArgumentException("Cannot insert range " + range + " into an empty subRangeMap");
		}

		@Override
		public void putAll(RangeMap rangeMap) {
			if (!rangeMap.asMapOfRanges().isEmpty()) {
				throw new IllegalArgumentException("Cannot putAll(nonEmptyRangeMap) into an empty " + "subRangeMap");
			}
		}

		@Override
		public void clear() {
		}

		@Override
		public void remove(Range range) {
			checkNotNull(range);
		}

		@Override
		public Map<Range, Object> asMapOfRanges() {
			return Collections.emptyMap();
		}

		@Override
		public RangeMap subRangeMap(Range range) {
			checkNotNull(range);
			return this;
		}
	};

	private class SubRangeMap implements RangeMap<K, V> {

		private final Range<K> subRange;

		SubRangeMap(Range<K> subRange) {
			this.subRange = subRange;
		}

		@Override
		@Nullable
		public V get(K key) {
			return subRange.contains(key) ? TreeRangeMap.this.get(key) : null;
		}

		@Override
		@Nullable
		public Entry<Range<K>, V> getEntry(K key) {
			if (subRange.contains(key)) {
				Entry<Range<K>, V> entry = TreeRangeMap.this.getEntry(key);
				if (entry != null) {
					return Maps.immutableEntry(entry.getKey().intersection(subRange), entry.getValue());
				}
			}
			return null;
		}

		@Override
		public Range<K> span() {
			Cut<K> lowerBound;
			Entry<Cut<K>, RangeMapEntry<K, V>> lowerEntry = entriesByLowerBound.floorEntry(subRange.lowerBound);
			if (lowerEntry != null && lowerEntry.getValue().getUpperBound().compareTo(subRange.lowerBound) > 0) {
				lowerBound = subRange.lowerBound;
			} else {
				lowerBound = entriesByLowerBound.ceilingKey(subRange.lowerBound);
				if (lowerBound == null || lowerBound.compareTo(subRange.upperBound) >= 0) {
					throw new NoSuchElementException();
				}
			}

			Cut<K> upperBound;
			Entry<Cut<K>, RangeMapEntry<K, V>> upperEntry = entriesByLowerBound.lowerEntry(subRange.upperBound);
			if (upperEntry == null) {
				throw new NoSuchElementException();
			} else if (upperEntry.getValue().getUpperBound().compareTo(subRange.upperBound) >= 0) {
				upperBound = subRange.upperBound;
			} else {
				upperBound = upperEntry.getValue().getUpperBound();
			}
			return Range.create(lowerBound, upperBound);
		}

		@Override
		public void put(Range<K> range, V value) {
			checkArgument(subRange.encloses(range), "Cannot put range %s into a subRangeMap(%s)", range, subRange);
			TreeRangeMap.this.put(range, value);
		}

		@Override
		public void putAll(RangeMap<K, V> rangeMap) {
			if (rangeMap.asMapOfRanges().isEmpty()) {
				return;
			}
			Range<K> span = rangeMap.span();
			checkArgument(subRange.encloses(span), "Cannot putAll rangeMap with span %s into a subRangeMap(%s)", span,
					subRange);
			TreeRangeMap.this.putAll(rangeMap);
		}

		@Override
		public void clear() {
			TreeRangeMap.this.remove(subRange);
		}

		@Override
		public void remove(Range<K> range) {
			if (range.isConnected(subRange)) {
				TreeRangeMap.this.remove(range.intersection(subRange));
			}
		}

		@Override
		public RangeMap<K, V> subRangeMap(Range<K> range) {
			if (!range.isConnected(subRange)) {
				return emptySubRangeMap();
			} else {
				return TreeRangeMap.this.subRangeMap(range.intersection(subRange));
			}
		}

		@Override
		public Map<Range<K>, V> asMapOfRanges() {
			return new SubRangeMapAsMap();
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (o instanceof RangeMap) {
				RangeMap<?, ?> rangeMap = (RangeMap<?, ?>) o;
				return asMapOfRanges().equals(rangeMap.asMapOfRanges());
			}
			return false;
		}

		@Override
		public int hashCode() {
			return asMapOfRanges().hashCode();
		}

		@Override
		public String toString() {
			return asMapOfRanges().toString();
		}

		class SubRangeMapAsMap extends AbstractMap<Range<K>, V> {

			@Override
			public boolean containsKey(Object key) {
				return get(key) != null;
			}

			@Override
			public V get(Object key) {
				try {
					if (key instanceof Range) {
						@SuppressWarnings("unchecked") // we catch ClassCastExceptions
						Range<K> r = (Range<K>) key;
						if (!subRange.encloses(r) || r.isEmpty()) {
							return null;
						}
						RangeMapEntry<K, V> candidate = null;
						if (r.lowerBound.compareTo(subRange.lowerBound) == 0) {
							// r could be truncated on the left
							Entry<Cut<K>, RangeMapEntry<K, V>> entry = entriesByLowerBound.floorEntry(r.lowerBound);
							if (entry != null) {
								candidate = entry.getValue();
							}
						} else {
							candidate = entriesByLowerBound.get(r.lowerBound);
						}

						if (candidate != null && candidate.getKey().isConnected(subRange)
								&& candidate.getKey().intersection(subRange).equals(r)) {
							return candidate.getValue();
						}
					}
				} catch (ClassCastException e) {
					return null;
				}
				return null;
			}

			@Override
			public V remove(Object key) {
				V value = get(key);
				if (value != null) {
					@SuppressWarnings("unchecked") // it's definitely in the map, so safe
					Range<K> range = (Range<K>) key;
					TreeRangeMap.this.remove(range);
					return value;
				}
				return null;
			}

			@Override
			public void clear() {
				SubRangeMap.this.clear();
			}

			private boolean removeEntryIf(Predicate<? super Entry<Range<K>, V>> predicate) {
				List<Range<K>> toRemove = Lists.newArrayList();
				for (Entry<Range<K>, V> entry : entrySet()) {
					if (predicate.apply(entry)) {
						toRemove.add(entry.getKey());
					}
				}
				for (Range<K> range : toRemove) {
					TreeRangeMap.this.remove(range);
				}
				return !toRemove.isEmpty();
			}

			@Override
			public Set<Range<K>> keySet() {
				return new Maps.KeySet<Range<K>, V>(SubRangeMapAsMap.this) {
					@Override
					public boolean remove(@Nullable Object o) {
						return SubRangeMapAsMap.this.remove(o) != null;
					}

					@Override
					public boolean retainAll(Collection<?> c) {
						return removeEntryIf(compose(not(in(c)), Maps.<Range<K>>keyFunction()));
					}
				};
			}

			@Override
			public Set<Entry<Range<K>, V>> entrySet() {
				return new Maps.EntrySet<Range<K>, V>() {
					@Override
					Map<Range<K>, V> map() {
						return SubRangeMapAsMap.this;
					}

					@Override
					public Iterator<Entry<Range<K>, V>> iterator() {
						if (subRange.isEmpty()) {
							return Iterators.emptyIterator();
						}
						Cut<K> cutToStart = Objects.firstNonNull(entriesByLowerBound.floorKey(subRange.lowerBound),
								subRange.lowerBound);
						final Iterator<RangeMapEntry<K, V>> backingItr = entriesByLowerBound.tailMap(cutToStart, true)
								.values().iterator();
						return new AbstractIterator<Entry<Range<K>, V>>() {

							@Override
							protected Entry<Range<K>, V> computeNext() {
								while (backingItr.hasNext()) {
									RangeMapEntry<K, V> entry = backingItr.next();
									if (entry.getLowerBound().compareTo(subRange.upperBound) >= 0) {
										break;
									} else if (entry.getUpperBound().compareTo(subRange.lowerBound) > 0) {
										// this might not be true e.g. at the start of the iteration
										return Maps.immutableEntry(entry.getKey().intersection(subRange),
												entry.getValue());
									}
								}
								return endOfData();
							}
						};
					}

					@Override
					public boolean retainAll(Collection<?> c) {
						return removeEntryIf(not(in(c)));
					}

					@Override
					public int size() {
						return Iterators.size(iterator());
					}

					@Override
					public boolean isEmpty() {
						return !iterator().hasNext();
					}
				};
			}

			@Override
			public Collection<V> values() {
				return new Maps.Values<Range<K>, V>(this) {
					@Override
					public boolean removeAll(Collection<?> c) {
						return removeEntryIf(compose(in(c), Maps.<V>valueFunction()));
					}

					@Override
					public boolean retainAll(Collection<?> c) {
						return removeEntryIf(compose(not(in(c)), Maps.<V>valueFunction()));
					}
				};
			}
		}
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (o instanceof RangeMap) {
			RangeMap<?, ?> rangeMap = (RangeMap<?, ?>) o;
			return asMapOfRanges().equals(rangeMap.asMapOfRanges());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return asMapOfRanges().hashCode();
	}

	@Override
	public String toString() {
		return entriesByLowerBound.values().toString();
	}
}
