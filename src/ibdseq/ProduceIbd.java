/*
 * Copyright 2012-2013 Brian L. Browning
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
package ibdseq;

import blbutil.Utilities;
import ibd.IbdTract2;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class <code>ProduceIbd</code> detects IBD/HBD segments in genotype data.
 * The algorithm details will be published separately.
 *
 * <p>Each worker repeatedly claims an outer sample index <code>s1</code> from a
 * shared {@code AtomicInteger} and scans all accepted partners
 * <code>s2 &ge; s1</code>, reusing <code>s1</code>'s dose row across partners.
 * Scoring is a single branchless lookup per marker into the marker-major
 * {@code IbdScores} cell tables, keyed by the precomputed scored-allele doses.
 * Class <code>ProduceIbd</code> is thread-safe.</p>
 *
 * @author Brian L. Browning <browning@uw.edu>
 */
public class ProduceIbd implements Runnable {

    private final BlockingQueue<IbdTract2> qOut;
    private final byte[][] sampleDoses; // [sample][marker], missing==3
    private final double[] ibdCell;     // (marker<<4) + (doseA<<2) + doseB
    private final double[] hbdCell;     // (marker<<2) + dose
    private final int nMarkers;
    private final int nSamples;
    private final boolean[] focus;      // null => all pairs accepted
    private final AtomicInteger nextS1;
    private final float ibdLod;
    private final float ibdTrim;

    /**
     * Constructs a <code>ProduceIbd</code> worker.
     * @param qOut the output queue for detected IBD/HBD tracts.
     * @param sampleDoses sample-major scored-allele doses ([sample][marker],
     * missing encoded as 3).
     * @param ibdCell the marker-major IBD score table.
     * @param hbdCell the marker-major HBD score table.
     * @param nMarkers the number of markers.
     * @param nSamples the number of samples.
     * @param focus the focus-sample mask, or <code>null</code> to analyze all
     * pairs. When non-null, a pair is analyzed iff at least one sample is a
     * focus sample.
     * @param nextS1 the shared dispenser of outer sample indices.
     * @param ibdLod the min LOD score to declare IBD.
     * @param ibdTrim the LOD score to trim from each end of a segment.
     *
     * @throws IllegalArgumentException if
     * <code>ibdLod &le; 0.0f || Float.isInfinite(ibdLod)
     * || Float.isNaN(ibdLod)</code>, or if
     * <code>ibdTrim &lt; 0.0f || Float.isInfinite(ibdTrim)
     * || Float.isNaN(ibdTrim)</code>.
     */
    public ProduceIbd(BlockingQueue<IbdTract2> qOut, byte[][] sampleDoses,
            double[] ibdCell, double[] hbdCell, int nMarkers, int nSamples,
            boolean[] focus, AtomicInteger nextS1, float ibdLod, float ibdTrim) {
        if (ibdLod <= 0.0f || Float.isInfinite(ibdLod) || Float.isNaN(ibdLod)) {
            throw new IllegalArgumentException("ibdLod: " + ibdLod);
        }
        if (ibdTrim < 0.0f || Float.isInfinite(ibdTrim) || Float.isNaN(ibdTrim)) {
            throw new IllegalArgumentException("ibdTrim: " + ibdTrim);
        }
        this.qOut = qOut;
        this.sampleDoses = sampleDoses;
        this.ibdCell = ibdCell;
        this.hbdCell = hbdCell;
        this.nMarkers = nMarkers;
        this.nSamples = nSamples;
        this.focus = focus;
        this.nextS1 = nextS1;
        this.ibdLod = ibdLod;
        this.ibdTrim = ibdTrim;
    }

    @Override
    public void run() {
        try {
            int s1;
            while ((s1 = nextS1.getAndIncrement()) < nSamples) {
                byte[] d1 = sampleDoses[s1];
                boolean focusS1 = (focus == null) || focus[s1];
                for (int s2=s1; s2<nSamples; ++s2) {
                    if (focusS1 || focus[s2]) {
                        detectPair(s1, s2, d1, sampleDoses[s2]);
                    }
                }
            }
            qOut.put(ConsumeIbd.POISON_TRACT);
        }
        catch (Exception ex) {
            Utilities.exit("", ex);
        }
    }

    private void detectPair(int s1, int s2, byte[] d1, byte[] d2)
            throws InterruptedException {
        boolean hbd = (s1 == s2);
        float thisSum = 0.0f, maxSum = 0.0f;
        int start = 0, end = 0;
        for (int j=0; j<nMarkers; ++j) {
            thisSum += hbd ? hbdCell[(j<<2) + d1[j]]
                    : ibdCell[(j<<4) + (d1[j]<<2) + d2[j]];
            if (thisSum > maxSum) {
                maxSum = thisSum;
                end = j;
            }
            else if (thisSum <= 0.0) {
                if (maxSum >= ibdLod) {
                    start = trimmedStart(d1, d2, hbd, start, end);
                    end = trimmedEnd(d1, d2, hbd, start, end);
                    if (end > start) {
                        qOut.put(new IbdTract2(s1, s2, start, end, maxSum));
                    }
                }
                start = j + 1;
                end = start;
                thisSum = 0.0f;
                maxSum = 0.0f;
            }
        }
        if (maxSum >= ibdLod) {
            start = trimmedStart(d1, d2, hbd, start, end);
            end = trimmedEnd(d1, d2, hbd, start, end);
            if (end > start) {
                qOut.put(new IbdTract2(s1, s2, start, end, maxSum));
            }
        }
    }

    private double score(byte[] d1, byte[] d2, boolean hbd, int j) {
        return hbd ? hbdCell[(j<<2) + d1[j]]
                : ibdCell[(j<<4) + (d1[j]<<2) + d2[j]];
    }

    private int trimmedStart(byte[] d1, byte[] d2, boolean hbd, int start,
            int end) {
        if (ibdTrim <= 0.0f) {
            return start;
        }
        else {
            float sum = 0.0f;
            int index = start;
            while (index <= end && sum < ibdTrim) {
                sum += score(d1, d2, hbd, index);
                ++index;
            }
            return index - 1;
        }
    }

    private int trimmedEnd(byte[] d1, byte[] d2, boolean hbd, int start,
            int end) {
        if (ibdTrim <= 0.0f) {
            return end;
        }
        else {
            float sum = 0.0f;
            int index = end;
            while (index >= start && sum < ibdTrim) {
                sum += score(d1, d2, hbd, index);
                --index;
            }
            return index + 1;
        }
    }
}
