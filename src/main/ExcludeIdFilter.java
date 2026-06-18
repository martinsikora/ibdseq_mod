/*
 * Copyright 2013 Brian L. Browning
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
package main;

import blbutil.FileIterator;
import blbutil.Filter;
import blbutil.InputStreamIterator;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/* Code Review on 10 May 2013 */

/**
 * Class <code>IncludeFilter</code> is a marker filter that
 * accepts only markers that are not in a list of excluded markers.
 */
public final class ExcludeIdFilter implements Filter<Marker> {

    private final Set<String> excludedIds;

    /**
     * Constructs a <code>MarkerFilter</code> that accepts only markers that
     * are not present in the specified VCF file.
     * @param vcfFile a VCF file.
     *
     * @throws IllegalArgumentException if <code>start > end</code>.
     * @throws NullPointerException if <code>chrom==null</code>
     */
    public ExcludeIdFilter(File idFile) {
        this.excludedIds = new HashSet<String>(10000);
        FileIterator<String> it = InputStreamIterator.fromGzipFile(idFile);
        while (it.hasNext()) {
            excludedIds.add(it.next());
        }
        it.close();
    }

    @Override
    public boolean accept(Marker marker) {
        boolean accept = true;
        int n = marker.nIds();
        for (int j=0; j<n && accept==true; ++j) {
            if (excludedIds.contains(marker.id(j))) {
                accept = false;
            }
        }
        if (accept==true) {
            String posId = marker.chrom() + ':' + marker.pos();
            if (excludedIds.contains(posId)) {
                accept = false;
            }
        }
        return accept;
    }


    /**
     * Returns a string representation of <code>this</code>.  The exact
     * details of the specification are unspecified and subject to change.
     * @return a string representation of <code>this</code>.
     */
    @Override
    public String toString() {
        return "ExcludeIdFilter: " + excludedIds.size() + " markers.";
    }
}
