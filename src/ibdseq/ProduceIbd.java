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

import blbutil.IntPair;
import blbutil.Utilities;
import ibd.IbdTract2;
import java.util.concurrent.BlockingQueue;
import vcf.MarkerData;
import vcf.VcfMarkerData;

/**
 * Class <code>ProduceIbd</code> detects IBD segments in genotype data.
 * The algorithm details will be published separately.
 * Class <code>ProduceIbd</code> is thread-safe.
 *
 * @author Brian L. Browning <browning@uw.edu>
 */
public class ProduceIbd implements Runnable {

    public static final IntPair POISON_PAIR = new IntPair(-1, -1);

    private final IbdScores ibdScores;
    private final BlockingQueue<IntPair> qIn;
    private final BlockingQueue<IbdTract2> qOut;
    private final VcfMarkerData vcfData;
    private final float ibdLod;
    private final float ibdTrim;

    /**
     * Constructs a <code>ProduceIbd</code> instance which implements
     * the middle consumer/producer in two sequential producer-consumer
     * patterns.
     * @param qIn the input queue.
     * @param qOut the output queue.
     * @param vcfData the genotype data.
     * @param ibdScores the IBD scores.
     * @param ibdLod the min LOD ibdScore to declare IBD.
     * @param ibdTrim the LOD ibdScore of the markers trimmed from
     * each end of the IBD segment.
     *
     * @throws IllegalArgumentException if
     * <code>ibdLod <= 0.0f || Float.isInfinite(ibdLod)
     * || Float.isNaN(ibdLod)</code>, or if
     * <code>ibdTrim < 0.0f || Float.isInfinite(ibdTrim)
     * || Float.isNaN(ibdTrim)</code>.
     *
     * @throws NullPointerException if
     * <code>qIn==null || qOut==null || vcfData==null || scorer==null</code>.
     */
    public ProduceIbd(BlockingQueue<IntPair> qIn,
            BlockingQueue<IbdTract2> qOut, VcfMarkerData vcfData,
            IbdScores ibdScores, float ibdLod, float ibdTrim) {
        if (ibdLod <= 0.0f || Float.isInfinite(ibdLod) || Float.isNaN(ibdLod)) {
            throw new IllegalArgumentException("ibdLod: " + ibdLod);
        }
        if (ibdTrim < 0.0f || Float.isInfinite(ibdTrim) || Float.isNaN(ibdTrim)) {
            throw new IllegalArgumentException("ibdTrim: " + ibdTrim);
        }
        this.qIn = qIn;
        this.qOut = qOut;
        this.vcfData = vcfData;
        this.ibdScores = ibdScores;
        this.ibdLod = ibdLod;
        this.ibdTrim = ibdTrim;
    }

    @Override
    public void run() {
        try {
            IntPair pair = qIn.take();
            while (pair != POISON_PAIR) {
                int s1 = pair.first();
                int s2 = pair.second();
                float thisSum = 0.0f, maxSum = 0.0f;
                int start = 0, end = 0;
                for (int j=0, n=vcfData.nMarkers(); j<n; ++j) {
                    thisSum += score(s1, s2, j, vcfData.isCorrelated(j));
                    if (thisSum > maxSum) {
                        maxSum = thisSum;
                        end = j;
                    }
                    else if (thisSum <= 0.0) {
                        if (maxSum >= ibdLod) {
                            start = trimmedStart(s1, s2, start, end);
                            end = trimmedEnd(s1, s2, start, end);
                            if (end > start) {
                                qOut.put(new IbdTract2(pair, start, end, maxSum));
                            }
                        }
                        start = j + 1;
                        end = start;
                        thisSum = 0.0f;
                        maxSum = 0.0f;
                    }
                }
                if (maxSum >= ibdLod) {
                    start = trimmedStart(s1, s2, start, end);
                    end = trimmedEnd(s1, s2, start, end);
                    if (end > start) {
                        qOut.put(new IbdTract2(pair, start, end, maxSum));
                    }
                }
                pair = qIn.take();
            }
            qOut.put(ConsumeIbd.POISON_TRACT);
        }
        catch (Exception ex) {
            Utilities.exit("", ex);
        }
    }

    private int trimmedStart(int s1, int s2, int start, int end) {
        if (ibdTrim <= 0.0f) {
            return start;
        }
        else {
            float sum = 0.0f;
            int index = start;
            while (index <= end && sum < ibdTrim) {
                sum += score(s1, s2, index, vcfData.isCorrelated(index));
                ++index;
            }
            return index - 1;
        }
    }

    private int trimmedEnd(int s1, int s2, int start, int end) {
        if (ibdTrim <= 0.0f) {
            return end;
        }
        else {
            float sum = 0.0f;
            int index = end;
            while (index >= start && sum < ibdTrim) {
                sum += score(s1, s2, index, vcfData.isCorrelated(index));
                --index;
            }
            return index + 1;
        }
    }

    private double score(int sampleA, int sampleB, int marker, boolean isCor) {
        MarkerData md = vcfData.get(marker);
        byte allele = vcfData.scoreAllele(marker);
        if (sampleA==sampleB) {
            int dose = md.dose(sampleA, allele);
            if (dose>=0 && (isCor==false || dose==1) ) {
                return ibdScores.hbdScore(dose, marker);
            }
            else {
                return 0.0;
            }
        }
        else {
            int doseA = md.dose(sampleA, allele);
            int doseB = md.dose(sampleB, allele);
            boolean notIbs = (doseA==0 && doseB==2) || (doseA==2 && doseB==0);
            if ( (notIbs || isCor==false) && doseA>=0 && doseB>=0 ) {
                return ibdScores.ibdScore(doseA, doseB, marker);
            }
            else {
                return 0.0;
            }
        }
    }
}
