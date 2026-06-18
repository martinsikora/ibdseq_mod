/*
 * Copyright 2012 Brian L. Browning
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
package beagleutil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* Code Review on 13 Dec 2012 */

/**
 * Class <code>ChromIds</code> is a utility class that represents all
 * chromosome identifiers in an analysis.
 */
public final class ChromIds {

    private static final int INIT_CAPACITY  = 4;
    private static final List<String> idList
            = new ArrayList<String>(INIT_CAPACITY);
    private static final Map<String, Integer> idMap
            = new HashMap<String, Integer>(INIT_CAPACITY);

    private ChromIds() {
        // private constructor to restrict instantiation.
    }

    /**
     * Returns the unique index for the specified chromosome identifier.  If
     * the <code>this.hasIndex(id)==false</code> a unique index will be
     * assigned to the specified chromosome identifier.
     * @param id a chromosome identifier.
     * @returns the chromosome index.
     * @throws IllegalArgumentException if <code>id.isEmpty()</code>.
     * @throws NullPointerException if <code>id==null</code>.
     */
    public static int indexOf(String id) {
        if (id.isEmpty()) {
            throw new IllegalArgumentException("missing chromosome ID: " + id);
        }
        if (idMap.keySet().contains(id)) {
            return idMap.get(id);
        }
        else {
            int value = idList.size();
            idList.add(id);
            idMap.put(id, value);
            return value;
        }
    }

    /**
     * Returns <code>true</code> if the specified chromosome identifier
     * has been assigned a chromosome index, and returns <code>false</code>
     * otherwise.
     * @param id a chromosome identifiers.
     * @return <code>true</code> if the specified chromosome identifier
     * has been assigned a chromosome index, and returns <code>false</code>
     * otherwise.
     */
    public static boolean hasIndex(String id) {
        return idMap.keySet().contains(id);
    }

    /**
     * Returns the number of chromosomes identifiers.
     * @return the number of chromosomes identifiers.
     */
    public static int size() {
        return idList.size();
    }

    /**
     * Returns the specified chromosome identifier.
     * @param index a chromosome index.
     * @return the the specified chromosome identifier.
     * @throws IndexOutOfBoundsException if
     * <code> index < 0 || index &ge; this.size()</code>.
     */
    public static String id(int index) {
        return idList.get(index);
    }

    /**
     * Returns an array of length <code>this.size()</code> such that
     * <code>this.ids()[k].equals(this.id(k))==true</code>
     * for <code> 0 &le; k < this.size()</code>.
     *
     * @return an array of chromosome identifiers.
     */
    public static String[] ids() {
        return idList.toArray(new String[0]);
    }

    /**
     * Returns a string representation of <code>this</code>.
     * The exact details of the representation are unspecified and
     * subject to change.
     * @return a string representation of <code>this</code>.
     */
    @Override
    public String toString() {
        return idMap.toString();
    }
}
