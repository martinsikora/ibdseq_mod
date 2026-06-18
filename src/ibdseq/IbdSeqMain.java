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

import blbutil.Const;
import blbutil.FileIterator;
import blbutil.FileUtil;
import blbutil.InputStreamIterator;
import blbutil.IntPair;
import blbutil.StringUtil;
import blbutil.Utilities;
import ibd.IbdTract2;
import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import main.Marker;
import vcf.VcfMarkerData;

/**
 * See <code>IbdPhasePar</code> for constraints on the <code>args</code>
 * parameter.
 *
 * @author Brian L. Browning <browning@uw.edu>
 */
public class IbdSeqMain {

    public static final String version = "ibdseq r1206";

    private static final DecimalFormat df0 = new DecimalFormat("0");
    private static final DecimalFormat df1 = new DecimalFormat("0.0");
    private static final DecimalFormat df2 = new DecimalFormat("0.00");

    private final IbdSeqPar par;
    private final PrintWriter log;
    private final VcfMarkerData data;
    private final int nSamples;
    private final IbdScorer scorer;
    private final IbdScores ibdScores;

    private final long startNanoTime;
    private final long midNanoTime;

    /**
     * Entry point to the ibdseq program.  See the IbdSeqPar class
     * details of the program arguments.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
	Locale.setDefault(Locale.US);
	IbdSeqMain main = new IbdSeqMain(args);
    }

    /*
     * Constructs a <code>IbdSeqMain</code> instance that runs
     * the ibdseq program using the specified arguments.
     * </p>
     * Private constructor prevents public instantiation
     *
     * @param args command line arguments
     */
    private IbdSeqMain(String[] args) {
        if (args.length==0) {
            System.out.println(IbdSeqPar.usage());
            System.exit(0);
        }
        this.startNanoTime = System.nanoTime();
        this.par = new IbdSeqPar(args);
        this.log = FileUtil.printWriter(par.out() + ".log");
        this.scorer = new IbdScorer(par.errormax(), par.errorprop());
        Utilities.duoPrintln(log, beginJobSummary(par));

        boolean usePhase = false;
        this.data = new VcfMarkerData(par.gt(), usePhase, par.excludemarkers(),
                par.excludesamples(), par.chrom(), par.minalleles(),
                par.r2window(), par.r2max(), par.scorefreq());
        printScoreFrequencies(data, par.out());
        this.nSamples = data.nSamples();
        this.ibdScores = new IbdScores(scorer, data);
        this.midNanoTime = System.nanoTime();
        printR2Exclusions(data, par.out());

        ConsumeIbd ibdConsumer = multiThreadedDetect();
        Utilities.duoPrintln(log, endJobSummary(ibdConsumer));
        this.log.close();
    }

    private static void printFrequencyExclusions(VcfMarkerData vcfData,
            String outPrefix) {
        File f = new File(outPrefix + ".freq.filtered");
        PrintWriter out = FileUtil.printWriter(f);
        for (int j=0, n=vcfData.nFrequencyExcludedMarkers(); j<n; ++j) {
            out.println(vcfData.frequencyExcludedMarkers(j));
        }
        out.close();
    }

    private static void printR2Exclusions(VcfMarkerData vcfData, String outPrefix) {
        File f = new File(outPrefix + ".r2.filtered");
        PrintWriter out = FileUtil.printWriter(f);
        for (int j=0, n=vcfData.nMarkers(); j<n; ++j) {
            if (vcfData.isCorrelated(j)) {
                out.println(vcfData.get(j).marker());
            }
        }
        out.close();
    }

    private static void printScoreFrequencies(VcfMarkerData vcfData,
            String outPrefix) {
        File f = new File(outPrefix + ".scorefreq");
        PrintWriter out = FileUtil.printWriter(f);
        out.print("CHROM");
        out.print(Const.tab);
        out.print("POS");
        out.print(Const.tab);
        out.print("ID");
        out.print(Const.tab);
        out.print("REF");
        out.print(Const.tab);
        out.print("ALT");
        out.print(Const.tab);
        out.print("ALLELE");
        out.print(Const.tab);
        out.println("FREQ");
        for (int j=0, n=vcfData.nMarkers(); j<n; ++j) {
            if (vcfData.isCorrelated(j)==false) {
                Marker marker = vcfData.get(j).marker();
                byte allele = vcfData.scoreAllele(j);
                out.print(marker);
                out.print(Const.tab);
                out.print(marker.allele(allele));
                out.print(Const.tab);
                out.println(vcfData.scoreFrequency(j));
            }
        }
        out.close();
    }

    private ConsumeIbd multiThreadedDetect() {
        final BlockingQueue<IntPair> qIn = new ArrayBlockingQueue<IntPair>(1000);
        final BlockingQueue<IbdTract2> qOut = new ArrayBlockingQueue<IbdTract2>(1000);
        final int nThreads = par.nthreads();
        final File ibdFile = new File(par.out() + ".ibd");
        final File hbdFile = new File(par.out() + ".hbd");
        final PairSelector pairSelector = PairSelector.create(par.focussamples(),
                data);
        final long nPairCount = pairSelector.nPairs();

        final ConsumeIbd ibdConsumer = new ConsumeIbd(data, qOut, nThreads,
                ibdFile, hbdFile);

        ExecutorService es = Executors.newFixedThreadPool(nThreads+1);
        es.submit(ibdConsumer);
        for (int j=0; j<nThreads; ++j) {
            es.submit(new ProduceIbd(qIn, qOut, data, ibdScores, par.ibdlod(),
                    par.ibdtrim()));
        }
        try {
            long milestone = Math.max(1L, nPairCount/10L);
            long submittedCnt = 0;
            boolean printedMidJobSummary = false;
            for (int j=0; j<nSamples; ++j) {
                for (int k=j; k<nSamples; ++k) {
                    if (pairSelector.accept(j, k)) {
                        qIn.put(new IntPair(j, k));
                        ++submittedCnt;
                        if (printedMidJobSummary==false
                                && submittedCnt >= milestone) {
                            long finishedCnt = submittedCnt - qIn.size();
                            printEstimatedFinishTime(finishedCnt, nPairCount);
                            Utilities.duoPrintln(log, midJobSummary());
                            printedMidJobSummary = true;
                        }
                    }
                }
            }
            for (int j=0; j<nThreads; ++j) {
                qIn.put(ProduceIbd.POISON_PAIR);
            }
            es.shutdown();
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }
        catch (Exception ex) {
            Utilities.exit("", ex);
        }
        return ibdConsumer;
    }

    private void printEstimatedFinishTime(long finishedCnt, long nPairs) {
        long elapsedNano = System.nanoTime() - midNanoTime;
        double scale = nPairs / (double) Math.max(finishedCnt, 1L);
        long remainingNano = (long) (scale*elapsedNano) - elapsedNano;
        remainingNano = Math.max(remainingNano, 0L);
        long remainingMillis = remainingNano/Const.mega;
        Date finish = new Date(System.currentTimeMillis() + remainingMillis);
        SimpleDateFormat sdf =
                new SimpleDateFormat("hh:mm a z 'on' dd MMM yyyy");
        System.out.println("Estimated finish  : " + sdf.format(finish) + Const.nl);
    }

    private static String beginJobSummary(IbdSeqPar par) {
        StringBuilder sb = new StringBuilder(300);
        long maxMemory = Runtime.getRuntime().maxMemory();
        sb.append("Program           :  ");
        sb.append(version);
        sb.append("  (max memory: ");
        if (maxMemory != Long.MAX_VALUE) {
            long maxMb = maxMemory / (1024*1024);
            sb.append(maxMb);
            sb.append(" MB)");
        }
        else {
            sb.append("[no limit])");
        }
        sb.append(Const.nl);
        sb.append("Start Time        :  ");
        sb.append(Utilities.timeStamp());

        sb.append(Const.nl);
        sb.append(Const.nl);
        sb.append(currentParameters(par));
        return sb.toString();
    }

    public static String currentParameters(IbdSeqPar par) {
        StringBuilder sb = new StringBuilder(150);
        sb.append("Parameters");
        sb.append(Const.nl);
        sb.append("  gt              :  ");
        sb.append(par.gt());
        sb.append(Const.nl);
        sb.append("  out             :  ");
        sb.append(par.out());
        if (par.excludesamples()!=null) {
            sb.append(Const.nl);
            sb.append("  excludesamples  :  ");
            sb.append(par.excludesamples());
        }
        if (par.excludemarkers()!=null) {
            sb.append(Const.nl);
            sb.append("  excludemarkers  :  ");
            sb.append(par.excludemarkers());
        }
        if (par.focussamples()!=null) {
            sb.append(Const.nl);
            sb.append("  focussamples    :  ");
            sb.append(par.focussamples());
        }
        if (par.scorefreq()!=null) {
            sb.append(Const.nl);
            sb.append("  scorefreq       :  ");
            sb.append(par.scorefreq());
        }
        if (par.chrom() != null) {
            sb.append(Const.nl);
            sb.append("  chrom           :  ");
            sb.append(par.chrom());
        }
        sb.append(Const.nl);
        sb.append("  minalleles      :  ");
        sb.append(par.minalleles());
        sb.append(Const.nl);
        sb.append("  ibdlod          :  ");
        sb.append(par.ibdlod());
        sb.append(Const.nl);
        sb.append("  ibdtrim         :  ");
        sb.append(par.ibdtrim());
        sb.append(Const.nl);
        sb.append("  errormax        :  ");
        sb.append(par.errormax());
        sb.append(Const.nl);
        sb.append("  errorprop       :  ");
        sb.append(par.errorprop());
        sb.append(Const.nl);
        sb.append("  r2window        :  ");
        sb.append(par.r2window());
        sb.append(Const.nl);
        sb.append("  r2max           :  ");
        sb.append(par.r2max());
        sb.append(Const.nl);
        sb.append("  nthreads        :  ");
        sb.append(par.nthreads());
        sb.append(Const.nl);
        return sb.toString();
    }

    private static final class PairSelector {

        private final boolean[] focus;
        private final long nPairs;

        private PairSelector(boolean[] focus, long nPairs) {
            this.focus = focus;
            this.nPairs = nPairs;
        }

        private static PairSelector create(File focusFile, VcfMarkerData data) {
            if (focusFile==null) {
                long nSamples = data.nSamples();
                long nPairs = nSamples*(nSamples + 1L)/2L;
                return new PairSelector(null, nPairs);
            }
            Set<String> focusIds = readFocusIds(focusFile);
            if (focusIds.isEmpty()) {
                String s = "focussamples file contains no sample IDs: "
                        + focusFile;
                throw new IllegalArgumentException(s);
            }
            boolean[] focus = new boolean[data.nSamples()];
            for (String id : focusIds) {
                int sample = sampleIndex(data, id);
                if (sample < 0) {
                    String s = "Focus sample not found after excludesamples "
                            + "filtering: " + id;
                    throw new IllegalArgumentException(s);
                }
                focus[sample] = true;
            }
            long nFocus = focusIds.size();
            long nSamples = data.nSamples();
            long nPairs = nFocus + nFocus*(nFocus - 1L)/2L
                    + nFocus*(nSamples - nFocus);
            return new PairSelector(focus, nPairs);
        }

        private boolean accept(int sampleA, int sampleB) {
            return focus==null || focus[sampleA] || focus[sampleB];
        }

        private long nPairs() {
            return nPairs;
        }

        private static Set<String> readFocusIds(File focusFile) {
            Set<String> ids = new HashSet<String>();
            FileIterator<String> it = InputStreamIterator.fromGzipFile(focusFile);
            while (it.hasNext()) {
                String line = it.next().trim();
                if (line.length() > 0) {
                    if (StringUtil.countFields(line) != 1) {
                        String s = "focussamples line has more than one field: "
                                + line;
                        throw new IllegalArgumentException(s);
                    }
                    if (ids.add(line)==false) {
                        String s = "Duplicate focus sample ID: " + line;
                        throw new IllegalArgumentException(s);
                    }
                }
            }
            it.close();
            return ids;
        }

        private static int sampleIndex(VcfMarkerData data, String sampleId) {
            for (int j=0, n=data.nSamples(); j<n; ++j) {
                if (data.samples().id(j).equals(sampleId)) {
                    return j;
                }
            }
            return -1;
        }
    }

    private String midJobSummary() {
        boolean includeDataStats = true;
        StringBuilder sb = new StringBuilder(300);
        if (includeDataStats) {
            int nFilteredMarkers = data.nMarkers();
            int nThinnedMarkers = data.nMarkers()-data.nCorrelatedMarkers();
            double thinnedProp =  (100.0 * nThinnedMarkers) / data.nInitialMarkers();
            sb.append("Data Statistics");
            sb.append(Const.nl);
            sb.append("  samples         :  ");
            sb.append(nSamples);
            sb.append(Const.nl);
            sb.append("  markers         :  ");
            sb.append(data.nInitialMarkers());
            sb.append(Const.nl);
            sb.append("   >= minalleles  :  ");
            sb.append(nFilteredMarkers);
            sb.append(Const.nl);
            sb.append("   LD-thinned     :  ");
            sb.append(nThinnedMarkers);
            sb.append("  (");
            sb.append( df0.format(thinnedProp) );
            sb.append("% of markers)");
        }
        return sb.toString();
    }

    private String endJobSummary(ConsumeIbd ibdConsumer) {
        boolean includeDataStats = true;
        StringBuilder sb = new StringBuilder(300);
        if (includeDataStats) {
            double count = ibdConsumer.count();
            double sumBases = ibdConsumer.sumBases();
            double sumMarkers = ibdConsumer.sumMarkers();
            double nGenotypes  = (double) nSamples * (double) data.nMarkers();

            double tractsPerSample = count / (double) nSamples;
            double meanMbLength = (count==0) ? 0.0 : sumBases / (count*Const.mega);
            double meanDepth = 2.0*sumMarkers / nGenotypes;

            sb.append("  segments/sample :  ");
            sb.append( df1.format(tractsPerSample) );
            sb.append(Const.nl);
            sb.append("  mean IBD length :  ");
            sb.append( df2.format(meanMbLength) );
            sb.append(" Mb");
            sb.append(Const.nl);
            sb.append("  mean IBD depth  :  ");
            sb.append(df1.format(meanDepth));
            sb.append(Const.nl);
            sb.append(Const.nl);
        }
        long elapsedNanoTime = System.nanoTime() - startNanoTime;
        sb.append("Wallclock Time:   :  ");
        sb.append(Utilities.elapsedNanos(elapsedNanoTime));
        sb.append(Const.nl);
        sb.append("End Time          :  ");
        sb.append(Utilities.timeStamp());
        return sb.toString();
    }
}
