/*
 * Copyright 2011-12 Brian L. Browning
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

import beagleutil.Samples;
import blbutil.Const;
import blbutil.FileIterator;
import blbutil.Filter;
import blbutil.StringUtil;
import java.util.ArrayList;
import java.util.List;

/* Code review on 17 Dec 2013 */

/**
 * Class <code>VcfHeader</code> represents the header lines of a VCF file that
 * precede the first VCF record.
 */
public final class VcfHeader  {

    private static final String SHORT_HEADER_PREFIX= "#CHROM" + Const.tab + "POS"
            + Const.tab + "ID" + Const.tab + "REF" + Const.tab + "ALT"
            + Const.tab + "QUAL" + Const.tab + "FILTER" + Const.tab + "INFO";

    private static final String LONG_HEADER_PREFIX =
            SHORT_HEADER_PREFIX + Const.tab + "FORMAT";
    private static int nFixedFields
            = StringUtil.countFields(LONG_HEADER_PREFIX, Const.tab);

    private final VcfMetaInfo[] metaLines;
    private final String headerLine;
    private final Samples sample;
    private final boolean[] filter;

    /**
     * Constructs a <code>VcfHeader</code> object for the VCF meta-information
     * lines and header line returned by the specified <code>FileIterator</code>.
     * @param it an iterator that returns lines of a VCF file.
     * @param sampleFilter a sample filter.
     *
     * @throws IllegalArgumentException if any of the meta-information and
     * header lines returned by the specified <code>FileIterator</code>
     * do not conform to the VCF specification.
     *
     * @throws NullPointerException if
     * <code>it==null || sampleFilter==null</code>.
     */
    public VcfHeader(FileIterator<String> it, Filter<String> sampleFilter) {
        List<String> filteredIds = new ArrayList<String>(3000);
        List<VcfMetaInfo> metaInfo = new ArrayList<VcfMetaInfo>(50);
        String header = null;
        while (it.hasNext() && header==null) {
            String line = it.next().trim();
            if (line.startsWith(VcfMetaInfo.PREFIX)) {
                metaInfo.add(new VcfMetaInfo(line));
            }
            else if (line.startsWith(SHORT_HEADER_PREFIX)) {
                header = line;
            }
            else {
                String s = "Unrecognized meta-information line ("
                        + (it.file()!=null ? it.file() : "stdin")
                        + "): " + Const.nl + line
                        + Const.nl + "Does a header line (#CHROM ...) "
                        + "immediately following the meta-information lines?";
                throw new IllegalArgumentException(s);
            }
        }
        if (header==null) {
            String s = "Missing header line (#CHROM ...) and data lines in VCF file ("
                        + (it.file()!=null ? it.file() : "stdin");
            throw new IllegalArgumentException(s);
        }
        this.metaLines = metaInfo.toArray(new VcfMetaInfo[0]);
        this.headerLine = header;
        this.filter = getFilter(headerLine, sampleFilter, filteredIds);
        this.sample = Samples.fromIds(filteredIds.toArray(new String[0]));
    }

    private static String[] getSamples(String line) {
        String[] sampleIds = null;
        if (line.equals(SHORT_HEADER_PREFIX)) {
            sampleIds = new String[0];
        }
        else if(line.startsWith(LONG_HEADER_PREFIX)) {
            String[] fields = StringUtil.getFields(line, Const.tab);
            assert fields.length >= nFixedFields;
            sampleIds = new String[fields.length - nFixedFields];
            System.arraycopy(fields, nFixedFields, sampleIds, 0, sampleIds.length);
        }
        else {
            String s = "error in \"#CHROM\" line format: " + line;
            throw new IllegalArgumentException(s);
        }
        return sampleIds;
    }

    /* side effect: fills filteredIds list */
    private static boolean[] getFilter(String headerLine,
            Filter<String> sampleFilter, List<String> filteredIds) {
        String[] sampleIds = getSamples(headerLine);
        boolean[] filter = new boolean[sampleIds.length];
        for (int j=0; j<sampleIds.length; ++j) {
            if (sampleFilter.accept(sampleIds[j])) {
                filteredIds.add(sampleIds[j]);
            }
            else {
                filter[j] = true;
            }
        }
        return filter;
    }

    /**
     * Returns the number of header lines which begin with "##FORMAT"
     *
     * @return the number of header lines which begin with "##FORMAT"
     */
     public int metaInfoLines() {
         return metaLines.length;
     }

    /**
      * Returns the specified header line.

      * @param index a filter line index
      * @return the specified header line.
      *
      * @throws IndexOutOfBoundsException if
      * <code>index < 0 || indexOf &ge; this.headerLines()</code>.
      */
     public VcfMetaInfo metaInfoLine(int index) {
         return metaLines[index];
     }

     /**
      * Returns the header line which begins with "#CHROM".
      * @return the header line which begins with "#CHROM".
      */
     public String headerLine() {
         return headerLine;
     }

     /**
      * Returns <code>true</code> if the specified sample in the VCF
      * file is excluded, and returns <code>false</code> otherwise.
      *
      * @parma index a sample index before sample exclusions are performed.
      *
      * @return if the specified sample in the VCF
      * file is excluded, and returns <code>false</code> otherwise.
      *
      * @throws IndexOutOfBoundsException if
      * <code>index < 0 || index &ge; this.unfilteredSize()</code>.
      */
     public boolean filter(int index) {
         return filter[index];
     }

     /**
      * Returns the number of samples in the VCF file before excluding
      * samples.
      * @return the number of samples in the VCF file before excluding
      * samples.
      */
     public int unfilteredSize() {
         return filter.length;
     }

    /**
     * Return the filtered samples in the VCF file after all sample exclusions.
     * @return the filtered samples in the VCF file after all sample exclusions.
     */
    public Samples sample() {
        return sample;
    }

    /**
     * Returns <code>this.sample().ids()</code>.
     * @return <code>this.sample().ids()</code>.
     */
    public String[] sampleIds() {
        return sample.ids();
    }

    /**
     * Returns the VCF meta-information lines and header line used to
     * construct <code>this</code>.
     * @return the VCF meta-information lines and header line used to
     * construct <code>this</code>.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(400);
        for (int j=0; j<metaLines.length; ++j) {
            sb.append(metaLines[j]);
            sb.append(Const.nl);
        }
        sb.append(headerLine);
        sb.append(Const.nl);
        return sb.toString();
    }
}
