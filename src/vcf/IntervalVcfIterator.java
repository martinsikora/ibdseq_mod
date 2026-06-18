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
import beagleutil.ChromInterval;
import beagleutil.Samples;
import java.io.File;
import java.util.NoSuchElementException;
import main.Marker;

/* Code review on 26 Sep 2013 */

/**
 * Class <code>IntervalVcfIterator</code> is a
 * <code>VcfIteratorInterface</code> whose <code>next()</code> method
 * returns only VCF records within a chromosome interval.
 * @author Brian L. Browning <browning@uw.edu>
 */
public final class IntervalVcfIterator implements SampleFileIterator<VcfRecord> {

    private final SampleFileIterator<VcfRecord> it;
    private final ChromInterval interval;
    private VcfRecord next;

    /**
     * Constructs a <code>FilteredVcfIterator</code> instance whose
     * <code>next()</code> method returns only VCF records within a
     * specified chromosome interval.
     * @param it an iterator over a list of <code>VcfRecord</code>.
     * @param interval a chromosome interval
     * @throws NullPointerException if <code>it==null || interval==null</code>.
     */
    public IntervalVcfIterator(SampleFileIterator<VcfRecord> it, ChromInterval interval) {
        if (it==null) {
            throw new IllegalArgumentException("it==null");
        }
        if (interval==null) {
            throw new IllegalArgumentException("interval==null");
        }
        this.it = it;
        this.interval = interval;
        this.next = readFirstRecord(it, interval);
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
        this.next = readNextRecord(it, interval);
        return current;
    }

    private static VcfRecord readFirstRecord(SampleFileIterator<VcfRecord> it,
            ChromInterval interval) {
        boolean enteredTargetChrom = false;
        VcfRecord nextRecord = null;
        boolean finished = false;
        while (finished==false && it.hasNext()) {
            VcfRecord candidate = it.next();
            Marker m = candidate.marker();
            if (enteredTargetChrom==false && interval.chromIndex()==m.chromIndex()) {
                enteredTargetChrom = true;
            }
            if (enteredTargetChrom) {
                if (inInterval(interval, m)) {
                    nextRecord = candidate;
                    finished = true;
                }
                if (interval.chromIndex()!=m.chromIndex() || interval.end() < m.pos()) {
                    finished = true;
                }
            }
        }
        return nextRecord;
    }

    private static VcfRecord readNextRecord(SampleFileIterator<VcfRecord> it,
            ChromInterval interval) {
        VcfRecord nextRecord = null;
        if (it.hasNext()) {
            VcfRecord candidate = it.next();
            if (inInterval(interval, candidate.marker())) {
                nextRecord = candidate;
            }
        }
        return nextRecord;
    }

    private static boolean inInterval(ChromInterval interval, Marker marker) {
        return (marker.chromIndex()==interval.chromIndex()
                && interval.start() <= marker.pos()
                && marker.pos() <= interval.end());
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
