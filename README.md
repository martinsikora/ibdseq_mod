# ibdseq_mod

This repository contains a modified copy of `ibdseq r1206`.

The main additions are intended to support incremental IBD analysis:

- `focussamples=<file>` restricts pairwise analysis to pairs where at least
  one sample is listed in the focus sample file.
- `scorefreq=<file>` reuses the retained marker, scored allele, and scoring
  frequency set from a previous run.
- Every run writes `<out>.scorefreq`, a tab-delimited file with:
  `CHROM POS ID REF ALT ALLELE FREQ`.

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

Incremental run using prior scoring markers and a focus sample list:

```bash
java -cp /tmp/ibdseq-classes ibdseq.IbdSeqMain \
  gt=expanded.vcf \
  out=incremental \
  scorefreq=baseline.scorefreq \
  focussamples=focus_samples.txt
```

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

- focus sample self-pairs, which are written to `.hbd`
- focus-focus pairs, which are written to `.ibd`
- focus-nonfocus pairs, which are written to `.ibd`

It skips nonfocus self-pairs and nonfocus-nonfocus pairs.

### Score Frequency File

The score frequency file should normally be the `<out>.scorefreq` file written
by a previous full IBDSeq run. It is tab-delimited with this header:

```text
CHROM	POS	ID	REF	ALT	ALLELE	FREQ
```

Example:

```text
CHROM	POS	ID	REF	ALT	ALLELE	FREQ
PvP01_01_v1	118404	rs1	A	C	C	0.125
PvP01_01_v1	155336	rs2	G	T	T	0.240
```

Rows are the LD-thinned markers retained by the full run. In focus mode,
IBDSeq matches VCF markers by `CHROM`, `POS`, `REF`, and `ALT`, then reuses:

- `ALLELE` as the scored allele
- `FREQ` as the allele frequency for IBD/HBD scoring

Focus mode does not perform additional LD pruning when `scorefreq` is used.

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
