# ibdseq_mod

This repository contains a modified copy of `ibdseq r1206`.

The main additions are intended to support incremental IBD analysis:

- `focussamples=<file>` restricts pairwise analysis to pairs where at least
  one sample is listed in the focus sample file.
- `scorefreq=<file>` reuses the marker set, scored allele, scoring frequency,
  and LD-pruned classification from a previous run, so focus-mode scoring
  matches stock IBDSeq on the merged set.
- IBD and HBD segment output is written as gzip-compressed cM tables:
  `<out>.ibd.gz` and `<out>.hbd.gz`.
- Every run writes `<out>.scorefreq`, a tab-delimited file with:
  `CHROM POS ID REF ALT ALLELE FREQ LD_PRUNED`.

## Build

Compile all Java sources into a temporary class directory:

```bash
mkdir -p /tmp/ibdseq-classes
javac -d /tmp/ibdseq-classes $(find src -name '*.java')
```

## Run

Baseline run:

```bash
java -cp /tmp/ibdseq-classes ibdseq.IbdSeqMain \
  gt=input.vcf \
  out=baseline
```

Baseline run with a recombination map:

```bash
java -cp /tmp/ibdseq-classes ibdseq.IbdSeqMain \
  gt=input.vcf \
  out=baseline \
  map=recombination_map.tsv
```

Without `map=<file>`, cM positions are computed from physical positions using
`cmpermb=<float>`, which defaults to `1.0`.

Incremental run using prior scoring markers and a focus sample list:

```bash
java -cp /tmp/ibdseq-classes ibdseq.IbdSeqMain \
  gt=expanded.vcf \
  out=incremental \
  scorefreq=baseline.scorefreq \
  focussamples=focus_samples.txt
```

## Segment Output

IBDSeq writes two segment files:

- `<out>.ibd.gz`: cross-sample IBD segments.
- `<out>.hbd.gz`: self-pair HBD segments.

Both files use the same tab-delimited, headered format:

```text
sample1	sample2	chromosome	pos_start	pos_end	lod	pos_start_cm	pos_end_cm	l_cm	l_cm_bin
```

This matches the table produced by the previous Python post-processing script.
The haplotype index columns from the legacy IBDSeq output are not emitted.

The cM fields are:

- `pos_start_cm`: cM coordinate for `pos_start`.
- `pos_end_cm`: cM coordinate for `pos_end`.
- `l_cm`: segment length in cM.
- `l_cm_bin`: length bin label.

### Recombination Map

Provide a recombination map with `map=<file>` to interpolate cM coordinates.
The file may be plain text or gzip-compressed and must be tab-delimited with
these header columns:

```text
Position(bp)	Map(cM)
```

Additional columns are ignored. Positions must be strictly increasing, and
`Map(cM)` values must be nondecreasing. Segment endpoints outside the map range
are clamped to the nearest endpoint cM value, matching `numpy.interp`.

Example:

```text
Position(bp)	Map(cM)
1	0.000
1000000	1.200
2000000	2.500
```

### Fixed cM/Mb Conversion

If `map=<file>` is not supplied, IBDSeq converts base-pair positions using:

```text
cM = bp / 1000000 * cmpermb
```

The default is `cmpermb=1.0`.

### Length Bins

Segment length bins are controlled with `bins=<comma-separated floats>`.
The default is:

```text
bins=0,1,2,4,8,20,30,3000
```

Bins are left-closed and right-open, matching `numpy.digitize` with default
settings. For example, a segment with `l_cm=1.0` is assigned to `1.0-2.0`.
Increase the final bin edge if a run reports that a segment length is outside
the configured bins.

## Focus Mode Inputs

Focus mode needs two files:

- `focussamples=<file>`: target samples to analyze.
- `scorefreq=<file>`: retained marker and scoring metadata from a full run.

### Focus Sample File

The focus sample file is a plain text file with one sample ID per nonblank
line:

```text
sample_A
sample_B
sample_C
```

Sample IDs must match IDs in the VCF after applying any `excludesamples`
filter. Duplicate focus sample IDs are invalid.

When `focussamples` is provided, IBDSeq analyzes:

- focus sample self-pairs, which are written to `.hbd.gz`
- focus-focus pairs, which are written to `.ibd.gz`
- focus-nonfocus pairs, which are written to `.ibd.gz`

It skips nonfocus self-pairs and nonfocus-nonfocus pairs.

### Score Frequency File

The score frequency file should normally be the `<out>.scorefreq` file written
by a previous full IBDSeq run. It is tab-delimited with this header:

```text
CHROM	POS	ID	REF	ALT	ALLELE	FREQ	LD_PRUNED
```

Example:

```text
CHROM	POS	ID	REF	ALT	ALLELE	FREQ	LD_PRUNED
PvP01_01_v1	118404	rs1	A	C	C	0.125	0
PvP01_01_v1	155336	rs2	G	T	T	0.240	1
```

Rows are **all** markers from the full run that pass the `minalleles` filter,
in genomic order. In focus mode, IBDSeq matches VCF markers by `CHROM`, `POS`,
`REF`, and `ALT`, then reuses:

- `ALLELE` as the scored allele
- `FREQ` as the allele frequency for IBD/HBD scoring
- `LD_PRUNED` as the marker's role in the LD-pruned scan

The `LD_PRUNED` column reproduces stock IBDSeq scoring on the merged sample set:

- `LD_PRUNED=0` (retained, LD-thinned): the marker contributes full IBD/HBD
  scores.
- `LD_PRUNED=1` (LD-pruned): the marker is **exclusion-only** — it contributes
  only when it provides evidence against IBD/HBD (opposite homozygotes for IBD,
  heterozygotes for HBD), exactly as stock IBDSeq uses correlated markers. This
  preserves segment-boundary resolution from the dense marker set.

The column is optional for backward compatibility: a 7-column scorefreq file
(no `LD_PRUNED`) is read with every marker treated as retained
(`LD_PRUNED=0`), matching the previous behavior.

Focus mode performs no LD pruning or frequency estimation of its own when
`scorefreq` is used: the frozen marker set, scored alleles, frequencies, and
retained/pruned classification all come from the full run, so new samples do
not perturb the scoring model. A pair involving a focus sample is then scored
**bit-identically to stock IBDSeq on the merged set**, given that frozen model.

### Full-to-Focus Workflow

First run IBDSeq on the full baseline sample set:

```bash
java -cp /tmp/ibdseq-classes ibdseq.IbdSeqMain \
  gt=baseline.vcf \
  out=baseline
```

This writes `baseline.scorefreq`.

Create a focus sample file:

```text
new_sample_1
new_sample_2
new_sample_3
```

Then run focus mode on the VCF to analyze:

```bash
java -cp /tmp/ibdseq-classes ibdseq.IbdSeqMain \
  gt=expanded.vcf \
  out=incremental \
  scorefreq=baseline.scorefreq \
  focussamples=focus_samples.txt
```

The focus run uses the same LD-thinned marker set, scored alleles, and scoring
frequencies from the full run.
