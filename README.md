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

The focus sample file should contain one sample ID per nonblank line.
