/*
 * Copyright 2010-12 Brian L. Browning
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ibd;

import java.util.Arrays;
import java.util.NoSuchElementException;

/* Code review on 17 Dec 2012 */

/**
 * Class <code>IntPairToIntMap</code> maps ordered pairs of integers
 * to integers.
 */
public final class IntPairToIntMap {

    public static final int DEFAULT_INITIAL_CAPACITY = 104729; // a prime
    public static final int MAX_CAPACITY = (1 << 30);
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;
    public static final int NIL = -1;
    private static final int HALF_NUM_INT_BITS = 32/2;

    private final int nullValue; // a value that cannot be associated with a key

    private float loadFactor;
    private int size;
    private int free;   // head of free list

    private int[] head;
    private int[] key1;
    private int[] key2;
    private int[] value;
    private int[] next;

    private int bucket = NIL;       // set by private index() method
    private int lastIndex = NIL;    // set by private index() method

    /* returns number of hash buckets -- each bucket is head of a linked list */
    private static int nBuckets(int capacity, float loadFactor) {
        return ((int) (capacity / loadFactor)) + 1;
    }

    /**
     * Constructs an empty <code>IntPairToIntMap</code> with the specified
     * initial capacity and load factor.
     *
     * @param  initialCapacity the initial capacity.
     * @param  loadFactor      the load factor
     * @param  nullValue a value that is not permitted in this map, and that
     * will be returned by the <code>get()</code> method when a key
     * is not found in this map.
     * @throws IllegalArgumentException if
     * <code>(initialCapacity < 1)
     * || (initialCapacity > MAX_CAPACITY)</code> or if
     * <code>(loadFactor <= 0.0f) || (loadFactor > 2.0f)
     * || (Float.isNaN(loadFactor))</code>.
     */
    public IntPairToIntMap(int initialCapacity, float loadFactor, int nullValue) {
        checkCapacity(initialCapacity);
        checkLoadFactor(loadFactor);
        this.loadFactor = loadFactor;
        this.nullValue = nullValue;
        allocateArrays(initialCapacity, nBuckets(initialCapacity, loadFactor));
        initializeArrays(nullValue);
    }

    /**
     * Constructs an empty <tt>IntPairToIntMap</tt> with the default
     * initial capacity <code>this.DEFAULT_INITIAL_CAPACITY</code>
     * and the default load factor <code>this.DEFAULT_LOAD_FACTOR</code>.
     *
     * @param  initialCapacity the initial capacity.
     * @param  loadFactor      the load factor
     * @param  nullValue a value that is not permitted in this map, and that
     * will be returned by the <code>get()</code> method when a key
     * is not found in this map.
     * @throws IllegalArgumentException if
     * <code>(initialCapacity < 1)
     * || (initialCapacity > MAX_CAPACITY)</code> or if
     * <code>(loadFactor <= 0.0f) || (Float.isNaN(loadFactor))</code>.
     */
    public IntPairToIntMap(int nullValue) {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, nullValue);
    }

    private void checkCapacity(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity < 1: " + capacity);
        }
        if (capacity > MAX_CAPACITY) {
            String s = "capacity=" + capacity
                    + " > MAX_LOG_CAPACITY=" + MAX_CAPACITY;
            throw new IllegalArgumentException(s);
        }
    }
    private void checkLoadFactor(float loadFactor) {
        if (loadFactor <= 0.0f || loadFactor > 2.0f) {
            String s = "loadFactor: " + loadFactor;
            throw new IllegalArgumentException(s);
        }
        if (Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("loadFactor cannot equal NaN");
        }
    }

    private void allocateArrays(int capacity, int nBuckets) {
        head = new int[nBuckets];
        key1 = new int[capacity];
        key2 = new int[capacity];
        value = new int[capacity];
        next = new int[capacity];
    }

    private void initializeArrays(int nullValue) {
        Arrays.fill(head, NIL);
        Arrays.fill(value, nullValue);
        size = 0;
        free = 0;
        for (int j=1; j<next.length; ++j) {
            next[j-1] = j;
        }
        next[next.length-1] = NIL;
    }
    /*
     * Increases the capacity of this <tt>IntPairToArrayIndexMap</tt> instance.
     * NB: Throws <tt>IllegalArgumentException</tt> if
     * the new capacity exceeds <code>MAX_CAPACITY</code>.
     */
    private void rehash() {
        int newCapacity = 3*value.length/2 + 1;
        if (newCapacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("Hash Table overflow");
        }
        int[] oldHead = head;
        int[] oldKey1 = key1;
        int[] oldKey2 = key2;
        int[] oldValue = value;
        int[] oldNext = next;

        allocateArrays(newCapacity, nBuckets(newCapacity, loadFactor));
        initializeArrays(nullValue);
        for (int j=0; j<oldHead.length; ++j) {
            int x = oldHead[j];
            while (x!=NIL) {
                put(oldKey1[x], oldKey2[x], oldValue[x]);
                x = oldNext[x];
            }
        }
    }

    private int hash(int key1, int key2) {
        if (((key1 + key2) & 1) == 1) {
            return (Integer.rotateLeft(key1, HALF_NUM_INT_BITS) + key2 );
        }
        else {
            return (key1 + Integer.rotateLeft(key2, HALF_NUM_INT_BITS) );
        }
    }

    /**
     * Returns the number of entries in this map.
     * @return the number of entries in this map.
     */
    public int size() {
        return size;
    }

    /**
     * Returns the null value returned by the <code>get()</code> method
     * when a key is not found in the map.
     * @return the null value returned by the <code>get()</code> method
     * when a key is not found in the map.
     */
    public int nullValue() {
        return nullValue;
    }

    /*
     * Return storage index for specified key or NIL if specified
     * key is not in the map.  Calling this method has two side effects:
     * the <code>int bucket</code> field is set to the bucket corresponding
     * to the key, and the <code>int lastIndex</code> field is set to the
     * index of the preceding element in the linked list
     * (or NIL if there is no preceding element).
     * @param key1 first integer in the key.
     * @param key2 second integer in the key.
     * @return storage index for specified key or NIL if specified
     * key is not in map
     */
    private int index(int key1, int key2) {
        bucket = Math.abs(hash(key1, key2) % head.length);
        lastIndex = NIL;
        int x = head[bucket];
        while (x!=NIL && (this.key1[x]!= key1 || this.key2[x] != key2)) {
            lastIndex = x;
            x = next[x];
        }
        return x;
    }

    /**
     * Returns the value corresponding to the specified key or
     * <code>this.nullValue()<code> if the key is not in the map.
     * @param id1 first integer in the key.
     * @param id2 second integer in the key.
     * @return the value corresponding to the specified key or
     * <code>nullValue()<code> if the key is not in the map.
     */
    public int get(int id1, int id2) {
        int x = index(id1, id2);
        return (x==NIL) ? nullValue : value[x];
    }

    /**
     * Adds the specified key and value to this map and returns
     * the existing value associated with the key or <code>nullValue</code>
     * if this map did not already contain the key.
     * @param id1 first integer in the key.
     * @param id2 second integer in the key.
     * @param the value
     * @return the existing value associated with the key or
     * <code>nullValue</code> if this map did not already contain the key.
     */
    public int put(int key1, int key2, int value) {
        if (value == nullValue) {
            throw new IllegalArgumentException("value=" + nullValue);
        }
        if (free==NIL) {
            rehash();
        }
        int x = index(key1, key2);
        if (x!=NIL) {
            int existingValue = this.value[x];
            this.value[x] = value;
            return existingValue;
        }
        else {
            int newIndex = free;
            free = next[free];
            this.key1[newIndex] = key1;
            this.key2[newIndex] = key2;
            this.value[newIndex] = value;
            this.next[newIndex] = head[bucket];
            head[bucket] = newIndex;    // bucket set on last call of index()
            ++size;
            return nullValue;
        }
    }

    /**
     * Removes the specified key from this map and
     * returns <code>true</code> if the key was found in the map
     * and <code>false</code> otherwise.
     * @param id1 first integer in the key.
     * @param id2 second integer in the key.
     * @return <code>true</code> if the key was found in the map
     * and <code>false</code> otherwise.
     */
    public boolean remove(int key1, int key2) {
        int x = index(key1, key2);
        int prevX = this.lastIndex;
        if (x==NIL) {
            return false;
        }
        else {
            assert this.key1[x]==key1;
            assert this.key2[x]==key2;
            assert value[x] != nullValue;
            value[x] = nullValue;
            if (prevX!=NIL) {
                next[prevX] = next[x];
            }
            else {
                assert head[bucket]==x;
                head[bucket] = next[x];
            }
            next[x] = free;
            free = x;
            --size;
            return true;
        }
    }

    /**
     * Returns a string representation of <code>this</code>.  The
     * exact details of the representation are unspecified and
     * subject to change.
     *
     * @return a string representation of <code>this</code>.
     */
    @Override
    public String toString() {
        String nl = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder(10*size);
        int cnt = 0;
        sb.append("[ size=");
        sb.append(size());
        IntPairToIntMap.Iterator it = this.iterator();
        while (it.hasNext()) {
                it.next();
                sb.append(nl);
                sb.append("cnt=");
                sb.append(cnt++);
                sb.append(" key1=");
                sb.append(it.key1());
                sb.append(" key2=");
                sb.append(it.key2());
                sb.append(" value=");
                sb.append(it.value());
        }
        sb.append(nl);
        sb.append("]");
        return sb.toString();
    }

    /**
     * Returns an <code>IntPairToIntMap.Iterator</code> for this map.
     * @return an <code>IntPairToIntMap.Iterator</code> for this map.
     */

    public IntPairToIntMap.Iterator iterator() {
        return new IntPairToIntMap.Iterator();
    }


    /**
     * Class <code>IntPairToIntMap.Iterator</code> is an iterator
     * for an <code>IntPairToIntMap</code> object.
     */
    public class Iterator {

        private int bucket = NIL;
        private int index = NIL;
        private int nextIndex = NIL;

        private Iterator() {
            this.bucket = -1;
            advanceBucket();
            if (bucket < head.length) {
                this.nextIndex = head[bucket];
            }
        }

        /**
         * Return <code>true</code> if the iteration has more elements
         * and <code>false</code> otherwise.
         *
         * @return <code>true</code> if the iteration has more elements and
         * <code>false</code> otherwise.
         */
        public boolean hasNext() {
            return (nextIndex != NIL);
        }

        /**
         * Advances to the next element in the iteration.
         *
         * @throws NoSuchElementException if the iteration has no more
         * elements.
         */
        public void next() {
            if (nextIndex==NIL) {
                throw new NoSuchElementException();
            }
            index = nextIndex;
            advanceNextIndex();
        }

        /**
         * Sets the value of the current element of the iteration to
         * the specified value and returns the previous value.  The behavior
         * of this method is unspecified if the backing
         * <code>IntPairToIntMap</code> is modified while the iteration is
         * in progress in any way other than by calling
         * <code>set()</code> or <code>remove()</code>.
         *
         * @returns the value of the current element of the iteration
         * immediately before this method is invoked.
         *
         * @throws IllegalStateException if the <code>value==nullValue</code>,
         * or if <code>next</code> method has not yet been called or if the
         * <code>remove</code> method has already been called after the last
         * call to the <code>next</code> method.
         */
        public int set(int value) {
            if (value == nullValue) {
                throw new IllegalArgumentException("value=" + nullValue);
            }
            if (index==NIL) {
                throw new IllegalStateException();
            }
            int oldValue = IntPairToIntMap.this.value[index];
            IntPairToIntMap.this.value[index] = value;
            return oldValue;
        }

        /**
         * Removes the current element of the iteration.  This method can
         * be called only once per call to <code>next</code>.  The behavior
         * of this method is unspecified if the backing
         * <code>IntPairToIntMap</code> is modified while the iteration is
         * in progress in any way other than by calling
         * <code>set()</code> or <code>remove()</code>.
         *
         * @throws IllegalStateException if the <code>next</code> method
         * has not yet been called or if the <code>remove</code> method
         * has already been called after the last call to the
         * <code>next</code> method.
         */
        public void remove() {
            if (index==NIL) {
                throw new IllegalStateException();
            }
            IntPairToIntMap.this.remove(key1[index], key2[index]);
            index = NIL;
        }

        private void advanceNextIndex() {
            if (next[index] != NIL) {
               nextIndex = next[index];
            }
            else {
                advanceBucket();
                if (bucket < head.length) {
                    assert head[bucket]!=NIL;
                    nextIndex = head[bucket];
                }
                else {
                   nextIndex = NIL;
                }
            }
        }

        private void advanceBucket() {
            ++bucket;
            while (bucket < head.length && head[bucket]==NIL) {
                ++bucket;
            }
        }

        /**
         * Returns the first integer key for the current element of the
         * iteration.
         * @return  the first integer key for the current element of the
         * iteration.
         *
         * @throws IllegalStateException if the <code>next</code> method
         * has not yet been called or if the <code>remove</code> method
         * has been called after the last call of the
         * <code>next</code> method.
         */
        public int key1() {
            if (index==NIL) {
                throw new IllegalStateException();
            }
            return key1[index];
        }

        /**
         * Returns the second integer key for the current element of the
         * iteration.
         * @return  the second integer key for the current element of the
         * iteration.
         *
         * @throws IllegalStateException if the <code>next</code> method
         * has not yet been called or if the <code>remove</code> method
         * has been called after the last call of the <code>next</code> method.
         */
        public int key2() {
            if (index==NIL) {
                throw new IllegalStateException();
            }
            return key2[index];
        }

        /**
         * Returns the value for the current element of the iteration.
         * @return the value for the current element of the iteration.
         *
         * @throws IllegalStateException if the <code>next</code> method
         * has not yet been called or if the <code>remove</code> method
         * has been called after the last call of the <code>next</code> method.
         */
        public int value() {
            if (index==NIL) {
                throw new IllegalStateException();
            }
            return value[index];
        }

        /**
         * Returns a string description of this
         * <code>IntPairToIntMap.Iterator</code>.  The exact details of
         * the description are unspecified and subject to change.
         *
         * @return a string description of this
         * <code>IntPairToIntMap.Iterator</code>.  The exact details of
         * the description are unspecified and subject to change.
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(40);
            sb.append("[bucket=");
            sb.append(bucket);
            sb.append(" index=");
            sb.append(index);
            sb.append(" nextIndex=");
            sb.append(nextIndex);
            sb.append(" key1=");
            sb.append(key1());
            sb.append(" key2=");
            sb.append(key2());
            sb.append(" value=");
            sb.append(value());
            sb.append("]");
            return sb.toString();
        }
    }
}