/*
 * Copyright 2010-13 Brian L. Browning
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

import beagleutil.ChromInterval;
import blbutil.SampleFileIterator;
import beagleutil.Samples;
import blbutil.Const;
import blbutil.FileIterator;
import blbutil.Filter;
import blbutil.InputStreamIterator;
import java.io.File;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import main.Marker;

/* Code review on 04 Dec 2013 */

/**
 * Class <code>VcfIterator</code> iterates over VCF records in a VCF file.
 */
public class VcfIterator implements SampleFileIterator<VcfRecord> {

    private static final Filter<String> ACCEPT_ALL_FILTER = new Filter<String>() {
        @Override
        public boolean accept(String e) {
            return true;
        }
    };

    private final VcfHeader vcfHeader;
    private final FileIterator<String> it;
    private final Set<String> chromSet = new HashSet<String>();

    private VcfRecord current;
    private VcfRecord next;

    /**
     * Constructs a <code>SampleFileIterator<VcfRecord></code> instance.
     * @param vcf a VCF file.
     * @param sampleFilter a sample filter.
     * @param markerFilter a marker filter.
     * @param chromInterval the the chromosome interval to read, or
     * <code>null</code> if there is no interval restriction.
     */
    public static SampleFileIterator<VcfRecord> filteredIterator(File file,
            Filter<String> sampleFilter, Filter<Marker> markerFilter,
            ChromInterval chromInterval) {
        SampleFileIterator<VcfRecord> it = new VcfIterator(file, sampleFilter);
        if (chromInterval != null) {
            it = new IntervalVcfIterator(it, chromInterval);
        }
        if (markerFilter != null) {
            it = new FilteredVcfIterator(it, markerFilter);
        }
        return it;
    }

    /**
     * Constructs a <code>VcfIteratorr</code> with no sample or marker
     * exclusions.
     *
     * @param vcfFile a file in VCF format.  The VCF records for
     * each chromosome must be contiguous and sorted in order of
     * increasing position.
     *
     * @throws NullPointerException if <code>vcfFile==null</code>.
     *
     * @throws IllegalArgumentException if any of the header lines
     * does not conform to the VCF specification, or if the first
     * VCF record does not conform to the VCF specification.
     */
    public VcfIterator(File vcfFile) {
        this(vcfFile, ACCEPT_ALL_FILTER);
    }

    /**
     * Constructs a <code>VcfIteratorr</code> with no sample or marker
     * exclusions.
     *
     * @param vcfFile a file in VCF format.  The VCF records for
     * each chromosome must be contiguous and sorted in order of
     * increasing position.
     * @param sampleFilter a sample filter.
     * @throws NullPointerException if
     * <code>vcfFile==null || sampleExclusions==null</code>.
     *
     * @throws IllegalArgumentException if any of the header lines
     * does not conform to the VCF specification, or if the first
     * VCF record does not conform to the VCF specification.
     */
    public VcfIterator(File vcfFile, Filter<String> sampleFilter) {
        this(InputStreamIterator.fromGzipFile(vcfFile), sampleFilter);
    }

    /**
     * Constructs a <code>VcfIterator</code> instance with no sample or marker
     * exclusions.
     *
     * @param it a <code>FileIterator<String></code> whose
     * <code>next()</code> method will return lines of a file in
     * VCF format.  The VCF records for each chromosome must be contiguous
     * and sorted in order of increasing position.
     *
     * @throws NullPointerException if <code>it==null</code>.
     *
     * @throws IllegalArgumentException if any of the header lines
     * does not conform to the VCF specification, or if the first
     * VCF record does not conform to the VCF specification.
     */
    public VcfIterator(FileIterator<String> it) {
        this(it, ACCEPT_ALL_FILTER);
    }

    /**
     * Constructs a <code>VcfIterator</code> instance.
     *
     * @param it a <code>FileIterator<String></code> whose
     * <code>next()</code> method will return lines of a file in
     * VCF format.  The VCF records for each chromosome must be contiguous
     * and sorted in order of increasing position.
     * @param sampleFilter a sample filter.
     *
     * @throws NullPointerException if
     * <code>it==null || sampleExclusions==null</code>.
     * @throws IllegalArgumentException if any of the header lines
     * does not conform to the VCF specification, or if the first
     * VCF record does not conform to the VCF specification.
     */
    private VcfIterator(FileIterator<String> it, Filter<String> sampleFilter) {
        if (sampleFilter==null) {
            throw new NullPointerException("sampleFilter==null");
        }
        this.it = it;
        this.vcfHeader = new VcfHeader(it, sampleFilter);
        this.current = null;
        this.next = readData();
    }

    /**
     * Constructs a <code>VcfIterator</code> instance that reads from
     * standard input  with no sample or marker exclusions.
     *
     * @throws IllegalArgumentException if any of the header lines
     * does not conform to the VCF specification, or if the first
     * VCF record does not conform to the VCF specification.
     */
    public static VcfIterator fromStdin() {
        FileIterator<String> it = new InputStreamIterator(System.in);
        return new VcfIterator(it, ACCEPT_ALL_FILTER);
    }

    /**
     * Constructs a <code>VcfIterator</code> instance that reads from
     * standard input and includes data only for samples passing the
     * specified filter.
     * @param sampleFilter a sample filter.
     *
     * @throws IllegalArgumentException if any of the header lines
     * does not conform to the VCF specification, or if the first
     * VCF record does not conform to the VCF specification.
     * @throws NullPointerException if <code>sampleFilter==null</code>.
     */
    public static VcfIterator fromStdin(Filter<String> sampleFilter) {
        FileIterator<String> it = new InputStreamIterator(System.in);
        return new VcfIterator(it, sampleFilter);
    }

    @Override
    public void close() {
        it.close();
        next = null;
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
        current = next;
        next = readData();
        checkMarkerPosOrder(current, next);
        return current;
    }

    @Override
    public void remove() {
        String s = "remove() is not supported by VcfIterator";
        throw new UnsupportedOperationException(s);
    }

    @Override
    public File file() {
        return it.file();
    }

    @Override
    public Samples samples() {
        return vcfHeader.sample();
    }

    private VcfRecord readData() {
        VcfRecord vcfRecord = null;
        while (it.hasNext() && vcfRecord==null) {
            String line = it.next().trim();
            if (line.isEmpty()==false) {
                vcfRecord = new VcfRecord(line, vcfHeader);
            }
        }
        return vcfRecord;
    }

    private void checkMarkerPosOrder(VcfRecord current, VcfRecord next) {
        if (next!=null) {
            Marker m1 = current.marker();
            Marker m2 = next.marker();
            if (m1.chromIndex()==m2.chromIndex()) {
                if (m1.pos() > m2.pos()) {
                    String s = "["
                            + (it.file()==null ? "stdin" : it.file().toString())
                            + "] markers not in chromosomal order: "
                            + Const.nl + m1 + Const.nl + m2;
                    throw new IllegalArgumentException(s);
                }
            }
            else {
                boolean newChrom = chromSet.add(m2.chrom());
                if (newChrom == false) {
                    String s = "["
                            + (it.file()==null ? "stdin" : it.file().toString())
                            + "] non-contiguous markers for chromosome "
                            + m2.chrom();
                    throw new IllegalArgumentException(s);
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("file=");
        sb.append(it.file());
        sb.append(" next=");
        sb.append(next);
        return sb.toString();
    }
}
