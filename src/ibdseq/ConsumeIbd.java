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
package ibdseq;

import beagleutil.SampleIds;
import blbutil.Const;
import blbutil.FileUtil;
import blbutil.Utilities;
import ibd.IbdTract2;
import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import main.Marker;
import vcf.VcfMarkerData;

/**
 * Class <code>ConsumeIbd</code> consumes IBD tracts from a
 * <code>BlockingQueue</code> and prints the consumed tracts to an
 * output file.  Class <code>ConsumeIbd</code> is thread-safe.
 *
 * @author Brian L. Browning
 */
public class ConsumeIbd implements Runnable{

    public static final IbdTract2 POISON_TRACT = new IbdTract2(0, 0, 0, 1, 0.0f);
    private static final DecimalFormat df2 = new DecimalFormat("0.00");

    private final PrintWriter ibdOut;
    private final PrintWriter hbdOut;

    private final VcfMarkerData vcfData;
    private final String[] sampleIds;
    private final BlockingQueue<IbdTract2> q;
    private final int nProducerThreads;

    private AtomicLong count = new AtomicLong(0);
    private AtomicLong sumMarkers = new AtomicLong(0);
    private AtomicLong sumBases = new AtomicLong(0);

    private int poisonCnt = 0;  // thread-confined

    /**
     * Constructs a <code>ConsumeIbd</code> instance.
     * @param vcfData the genotype data.
     * @param q a queue from which IBD tracts will be consumed.
     * @paqram nProducers number of producer threads.
     * @param ibdFile the file to which IBD tracts will be printed.
     *
     * @throws IllegalArgumentException if <code>nProducers < 1</code>.
     * @throws NullPointerException if
     * <code>vcfData==null || sampleIds==null || q==null || ibdFile==null</code>.
     */
    public ConsumeIbd(VcfMarkerData vcfData, BlockingQueue<IbdTract2> q,
            int nProducers, File ibdFile, File hbdFile) {
        if (nProducers < 1) {
            throw new IllegalArgumentException("nProducers < 1: " + nProducers);
        }
        this.ibdOut = FileUtil.printWriter(ibdFile);
        this.hbdOut = FileUtil.printWriter(hbdFile);
        this.vcfData = vcfData;
        this.sampleIds = SampleIds.ids();
        this.q = q;
        this.nProducerThreads = nProducers;
    }

    @Override
    public void run() {
        try {
            do {
                IbdTract2 t = q.take();
                if (t==POISON_TRACT) {
                    ++poisonCnt;
                }
                else {
                    print(t);
                }
            } while (poisonCnt < nProducerThreads);
        }
        catch (Exception ex) {
            Utilities.exit("", ex);
        }
        finally {
            ibdOut.close();
            hbdOut.close();
        }
    }

    /**
     * Returns the number of IBD tracts consumed.
     * @return the number of IBD tracts consumed.
     */
    public long count() {
        return count.get();
    }

    /**
     * Returns the sum of the number of markers in consumed IBD tracts.
     * @return the sum of the number of markers in consumed IBD tracts.
     */
    public long sumMarkers() {
        return sumMarkers.get();
    }

    /**
     * Returns the sum of the number of bases in consumed IBD tracts.
     * @return the sum of the number of bases in consumed IBD tracts.
     */
    public long sumBases() {
        return sumBases.get();
    }

    /**
     * Returns the number of markers.
     * @return the number of markers.
     */
    public int nMarkers() {
        return vcfData.nMarkers();
    }

    /**
     * Returns the number of samples.
     * @return the number of samples.
     */
    public int nSamples() {
        return sampleIds.length;
    }

    private void print(IbdTract2 tract) {
        Marker startMarker = vcfData.get(tract.start()).marker();
        String chrom = startMarker.chrom();
        int posStart = startMarker.pos();
        int posEnd = vcfData.get(tract.end()).marker().pos();
        PrintWriter out = ibdOut;
        char hap1 = '0';
        char hap2 = '0';
        if (tract.id1()==tract.id2()) {
            hap1 = '1';
            hap2 = '2';
            out = hbdOut;
        }
        count.incrementAndGet();
        sumMarkers.addAndGet(tract.end() - tract.start() + 1);
        sumBases.addAndGet(posEnd - posStart + 1);

        out.print(sampleIds[tract.id1()]);
        out.print(Const.tab);
        out.print(hap1);
        out.print(Const.tab);
        out.print(sampleIds[tract.id2()]);
        out.print(Const.tab);
        out.print(hap2);
        out.print(Const.tab);
        out.print(chrom);
        out.print(Const.tab);
        out.print(posStart);
        out.print(Const.tab);
        out.print(posEnd);
        out.print(Const.tab);
        out.println(df2.format(tract.score()));
    }
}
