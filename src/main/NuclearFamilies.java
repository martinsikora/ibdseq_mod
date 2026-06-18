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

import beagleutil.SampleIds;
import beagleutil.Samples;
import blbutil.FileIterator;
import blbutil.InputStreamIterator;
import blbutil.StringUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* Code Review on 03 Apr 2013 */

/**
 * Class <code>NuclearFamilies</code> has lists of unrelated individuals,
 * parent-offspring duos, and parent-offspring trios in sample.
 *
 * @author Brian L. Browning <browning@uw.edu>
 */
public class NuclearFamilies {

    private static final String MISSING_PARENT = "0";

    private final File pedFile;
    private final Samples samples;

    private final int[] unrelated;

    private final int[] duoParent;
    private final int[] duoOffspring;

    private final int[] trioFather;
    private final int[] trioMother;
    private final int[] trioOffspring;

    private final boolean[] hasFather;
    private final boolean[] hasMother;

    /**
     * Constructs a <code>NuclearFamilies</code> instance representing the
     * specified samples and pedigree file.
     *
     * @param samples the samples.
     * @param pedFile a linkage-format pedigree file, or <code>null</code>
     * if no pedigree relationships are known.  A pedigree file must have
     * at least 4 white-space delimited columns.  The first column of the
     * pedigree file (family ID) is ignored.  The second, third, and fourth
     * columns are the individual ID, father's ID, and mother's ID respectively.
     *
     * @throws NullPointerException if <code>samples==null</code>.
     * @throws IllegalArgumentException if the specified <code>pedFile</code>
     * is not <code>null</code> and represents a file with either a non-blank
     * row having less than 4 white-space delimited fields or a file with
     * duplicate individual identifiers in the second column.
     */
    public NuclearFamilies(Samples samples, File pedFile) {
        List<String> offspringList = new ArrayList<String>(5000);
        List<String> fatherList = new ArrayList<String>(5000);
        List<String> motherList = new ArrayList<String>(5000);
        this.hasFather = new boolean[samples.nSamples()];
        this.hasMother = new boolean[samples.nSamples()];
        if (pedFile != null) {
            fillLists(samples, pedFile, offspringList, fatherList, motherList,
                    hasFather, hasMother);
        }

        int twoParentCnt = twoParentCnt(fatherList, motherList);
        int relatedCnt = relatedCnt(offspringList, fatherList, motherList);
        int nUnrelateds = samples.nSamples() - relatedCnt;
        int nDuos = offspringList.size() - twoParentCnt;
        int nTrios = twoParentCnt;

        this.pedFile = pedFile;
        this.samples = samples;
        this.unrelated = new int[nUnrelateds];

        this.duoParent = new int[nDuos];
        this.duoOffspring = new int[nDuos];

        this.trioFather = new int[nTrios];
        this.trioMother = new int[nTrios];
        this.trioOffspring = new int[nTrios];

        fillArrays(samples, offspringList, fatherList, motherList, unrelated,
                duoParent, duoOffspring, trioFather, trioMother, trioOffspring);
    }

    private int twoParentCnt(List<String> fatherList, List<String> motherList) {
        assert fatherList.size()==motherList.size();
        int twoParentCnt = 0;
        for (int j=0, n=fatherList.size(); j<n; ++j) {
            if (fatherList.get(j)!=null && motherList.get(j)!=null) {
                ++twoParentCnt;
            }
        }
        return twoParentCnt;
    }

    private int relatedCnt(List<String> offspringList,
            List<String> fatherList, List<String> motherList) {
        assert offspringList.size()==fatherList.size();
        assert offspringList.size()==motherList.size();
        Set<String> relatedSet = new HashSet<String>(3*offspringList.size());
        for (int j=0, n=offspringList.size(); j<n; ++j) {
            String offspring = offspringList.get(j);
            String father = fatherList.get(j);
            String mother = motherList.get(j);
            assert father!=null || mother!=null;
            relatedSet.add(offspring);
            if (father!=null) {
                relatedSet.add(father);
            }
            if (mother!=null) {
                relatedSet.add(mother);
            }
        }
        int relatedCnt = relatedSet.size();
        relatedSet.clear();
        return relatedCnt;
    }

    @SuppressWarnings("RedundantStringConstructorCall")
    private static void fillLists(Samples samples, File pedFile,
            List<String> offspringList, List<String> fatherList,
            List<String> motherList, boolean[] hasFather, boolean[] hasMother) {
        Set<String> offspringSet = new HashSet<String>(5000);
        Set<String> idSet = new HashSet<String>(Arrays.asList(samples.ids()));
        FileIterator<String> pedIt = InputStreamIterator.fromGzipFile(pedFile);
        while (pedIt.hasNext()) {
            String line = pedIt.next().trim();
            if (line.length() > 0) {
                String[] fields = StringUtil.getFields(line, 5);
                if (fields.length < 4) {
                    String s = "invalid line in ped file: " + line;
                    throw new IllegalArgumentException(s);
                }
                String offspring = fields[1];
                String father = fields[2];
                String mother = fields[3];
                if (idSet.contains(offspring)) {
                    if (offspringSet.add(offspring)==false) {
                        String s = "duplicate ID in ped file: " + offspring;
                        throw new IllegalArgumentException(s);
                    }
                    int sIndex = samples.index(SampleIds.indexOf(offspring));
                    hasFather[sIndex] = father.equals(MISSING_PARENT)==false
                            && idSet.contains(father);
                    hasMother[sIndex] = mother.equals(MISSING_PARENT)==false
                            && idSet.contains(mother);
                    if (hasFather[sIndex] || hasMother[sIndex]) {
                        offspringList.add(new String(offspring));
                        fatherList.add(hasFather[sIndex] ? new String(father) : null);
                        motherList.add(hasMother[sIndex] ? new String(mother) : null);
                    }
                }
            }
        }
        pedIt.close();
    }

    private static void fillArrays(Samples samples, List<String> offspringList,
            List<String> fatherList, List<String> motherList,
            int[] unrelated, int[] duoParent, int[] duoOffspring,
            int[] trioFather, int[] trioMother, int[] trioOffspring) {
        Set<String> relatedSet = new HashSet<String>(3*offspringList.size());
        int duoIndex = 0;
        int trioIndex = 0;
        for (int j=0, n=offspringList.size(); j<n; ++j) {
            if (fatherList.get(j)==null) {
                duoParent[duoIndex] = index(samples, motherList.get(j));
                duoOffspring[duoIndex++] = index(samples, offspringList.get(j));
                relatedSet.add(motherList.get(j));
            }
            else if (motherList.get(j)==null) {
                duoParent[duoIndex] = index(samples, fatherList.get(j));
                duoOffspring[duoIndex++] = index(samples, offspringList.get(j));
                relatedSet.add(fatherList.get(j));
            }
            else {
                trioFather[trioIndex] = index(samples, fatherList.get(j));
                trioMother[trioIndex] = index(samples, motherList.get(j));
                trioOffspring[trioIndex++] = index(samples, offspringList.get(j));
                relatedSet.add(fatherList.get(j));
                relatedSet.add(motherList.get(j));
            }
            relatedSet.add(offspringList.get(j));
        }
        assert duoIndex==duoOffspring.length && trioIndex==trioOffspring.length;
        int unrelatedIndex = 0;
        for (int j=0, n=samples.nSamples(); j<n; ++j) {
            if (relatedSet.contains(samples.id(j))==false) {
                unrelated[unrelatedIndex++] = j;
            }
        }
        assert unrelatedIndex==unrelated.length;
    }

    private static int index(Samples samples, String id) {
        int idIndex = SampleIds.indexOf(id);
        int index = samples.index(idIndex);
        if (index < 0) {
            String s = "ID not found: " + id;
            throw new IllegalArgumentException(s);
        }
        return index;
    }

    /**
     * Returns the samples represented by <code>this</code>.
     * @return the samples represented by <code>this</code>.
     */
    public Samples samples() {
        return samples;
    }

    /**
     * Returns the pedigree file, or returns <code>null</code> if no
     * pedigree file was specified.
     * @return the pedigree file, or returns <code>null</code> if no
     * pedigree file was specified.
     */
    public File pedFile() {
        return pedFile;
    }

    /**
     * Returns the number of unrelated individuals in
     * <code>this.samples()</code>.  An individual in the sample is
     * considered unrelated if the individual has no first-degree relatives
     * in the sample.
     * @return the number of \"unrelated\" individuals in
     * <code>this.samples()</code>.
     */
    public int nSingles() {
        return unrelated.length;
    }

    /**
     * Returns the number of parent-offspring duos in
     * <code>this.samples()</code>.
     * @return the number of parent-offspring duos in
     * <code>this.samples()</code>.
     */
    public int nDuos() {
        return duoOffspring.length;
    }

    /**
     * Returns the number of parent-offspring trios in
     * <code>this.samples()</code>.
     * @return the number of parent-offspring trios in
     * <code>this.samples()</code>.
     */
    public int nTrios() {
        return trioOffspring.length;
    }

    /**
     * Returns the sample index of the specified unrelated individual.
     * @param index the index of an unrelated individual.
     * @return the sample index of the specified unrelated individual.
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.nUnrelateds()</code>.
     */
    public int single(int index) {
        return unrelated[index];
    }

    /**
     * Returns the sample index of the parent of the specified
     * parent-offspring duo.
     * @param index the index of a parent-offspring duo.
     * @return the sample index of the parent of the specified
     * parent-offspring duo.
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.nDuos()</code>.
     */
    public int duoParent(int index) {
        return duoParent[index];
    }

    /**
     * Returns the sample index of the offspring of the specified
     * parent-offspring duo.
     * @param index the index of a parent-offspring duo.
     * @return the sample index of the offspring of the specified
     * parent-offspring duo.
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.nDuos()</code>.
     */
    public int duoOffspring(int index) {
        return duoOffspring[index];
    }

    /**
     * Returns the sample index of the father of the specified
     * parent-offspring trio.
     * @param index the index of a parent-offspring trio.
     * @return the sample index of the father of the specified
     * parent-offspring trio.
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.nTrios()</code>.
     */
    public int trioFather(int index) {
        return trioFather[index];
    }

    /**
     * Returns the sample index of the mother of the specified
     * parent-offspring trio.
     * @param index the index of a parent-offspring trio.
     * @return the sample index of the mother of the specified
     * parent-offspring trio.
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.nTrios()</code>.
     */
    public int trioMother(int index) {
        return trioMother[index];
    }

    /**
     * Returns the sample index of the offspring of the specified
     * parent-offspring trio.
     * @param index the index of a parent-offspring trio.
     * @return the sample index of the offspring of the specified
     * parent-offspring trio.
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.nTrios()</code>.
     */
    public int trioOffspring(int index) {
        return trioOffspring[index];
    }

    /**
     * Returns <code>true</code> if the specified sample has a father in
     * <code>this.samples()</code>, and returns <code>false</code>
     * otherwise.
     * @param index the sample index.
     * @return <code>true</code> if the specified sample has a father in
     * <code>this.samples()</code>, and returns <code>false</code>
     * otherwise.
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.samples().size()</code>.
     */
    public boolean hasFather(int index) {
        return hasFather[index];
    }

    /**
     * Returns <code>true</code> if the specified sample has a mother in
     * <code>this.samples()</code>, and returns <code>false</code>
     * otherwise.
     * @param index the sample index.
     * @return <code>true</code> if the specified sample has a mother in
     * <code>this.samples()</code>, and returns <code>false</code>
     * otherwise.
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.samples().size()</code>.
     */
    public boolean hasMother(int index) {
        return hasMother[index];
    }
}
