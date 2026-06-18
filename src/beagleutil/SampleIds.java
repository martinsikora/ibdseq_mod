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

/* Code Review on 17 Dec 2012 */

/**
 * Class <code>SampleIds</code> is a singleton class that represents all
 * sample identifiers in an analysis.  Class <code>SampleIds</code> is
 * thread-safe.
 */
public final class SampleIds {

    private static final int INIT_CAPACITY  = 5000;
    private static final List<String> idList = new ArrayList<String>(INIT_CAPACITY);
    private static final Map<String, Integer> idMap = new HashMap<String, Integer>(INIT_CAPACITY);

    private SampleIds() {
        // private constructor to restrict instantiation.
    }

    /**
     * Returns the unique index for the specified sample identifier.  If
     * the <code>this.hasIndex(id)==false</code> a unique index will be
     * assigned to the specified sample identifier.
     * @param id a sample identifier.
     * @returns the sample index.
     * @throws IllegalArgumentException if <code>id.isEmpty()</code>.
     * @throws NullPointerException if <code>id==null</code>.
     */
    public static synchronized int indexOf(String id) {
        if (id.isEmpty()) {
            throw new IllegalArgumentException("missing sample ID: " + id);
        }
        if (idMap.keySet().contains(id)) {
            return idMap.get(id);
        }
        else {
            int idIndex = idList.size();
            idList.add(id);
            idMap.put(id, idIndex);
            return idIndex;
        }
    }

    /**
     * Returns <code>true</code> if the specified sample identifier
     * has been assigned a sample index, and returns <code>false</code>
     * otherwise.
     * @param id a sample identifiers.
     * @return <code>true</code> if the specified sample identifier
     * has been assigned a sample index, and returns <code>false</code>
     * otherwise.
     */
    public static synchronized boolean hasIndex(String id) {
        return idMap.keySet().contains(id);
    }

    /**
     * Returns the number of samples.
     * @return the number of samples.
     */
    public static synchronized int size() {
        return idList.size();
    }

    /**
     * Returns the specified sample identifier.
     * @param index a sample identifier index.
     * @return the the specified sample identifier.
     * @throws ArrayIndexOutOfBoundsException if
     * <code> index < 0 || index &ge; this.size()</code>.
     */
    public static synchronized String id(int index) {
        return idList.get(index);
    }

    /**
     * Returns an array of length <code>this.size()</code> such that
     * <code>this.ids()[k].equals(this.id(k))==true</code>
     * for <code> 0 &le; k < this.size()</code>.
     *
     * @return an array of sample identifiers.
     */
    public static synchronized String[] ids() {
        return idList.toArray(new String[0]);
    }
}
