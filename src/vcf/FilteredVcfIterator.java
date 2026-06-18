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
package vcf;

import blbutil.SampleFileIterator;
import beagleutil.Samples;
import blbutil.Filter;
import java.io.File;
import java.util.NoSuchElementException;
import main.Marker;

/* Code review on 10 May 2013 */

/**
 * Class <code>FilteredVcfIterator</code> is a
 * <code>VcfIteratorInterface</code> whose <code>next()</code> method
 * returns only VCF records that pass a marker filter.
 * @author Brian L. Browning <browning@uw.edu>
 */
public final class FilteredVcfIterator implements SampleFileIterator<VcfRecord> {

    private final SampleFileIterator<VcfRecord> it;
    private final Filter<Marker> filter;
    private VcfRecord next;

    /**
     * Constructs a <code>FilteredVcfIterator</code> instance whose
     * <code>next()</code> method returns only VCF records that pass the
     * specified marker filter.
     * @param it an iterator over a list of <code>VcfRecord</code>.
     * @param filter a marker filter.
     * @throws NullPointerException if <code>it==null || filter==null</code>.
     */
    public FilteredVcfIterator(SampleFileIterator<VcfRecord> it,
            Filter<Marker> filter) {
        if (it==null) {
            throw new IllegalArgumentException("it==null");
        }
        if (filter==null) {
            throw new IllegalArgumentException("filter==null");
        }
        this.it = it;
        this.filter = filter;
        this.next = readNextRecord(it, filter);
    }

    @Override
    public File file() {
        return it.file();
    }

    @Override
    public Samples samples() {
        return it.samples();
    }

    @Override
    public boolean hasNext() {
        return (next != null);
    }

    @Override
    public VcfRecord next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        VcfRecord current = next;
        this.next = readNextRecord(it, filter);
        return current;
    }

    private static VcfRecord readNextRecord(SampleFileIterator<VcfRecord> it,
            Filter<Marker> filter) {
        VcfRecord nextRecord = null;
        while (nextRecord==null && it.hasNext()) {
            VcfRecord candidate = it.next();
            if (filter.accept(candidate.marker())) {
                nextRecord = candidate;
            }
        }
        return nextRecord;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("VcfIteratorInterface.remove()");
    }

    @Override
    public void close() {
        it.close();
    }
}
