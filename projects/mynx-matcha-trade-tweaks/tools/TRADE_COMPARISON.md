# Matcha trade comparison

The [reconciliation report](trade_reconciliation.tsv) lists all 307 owner decisions and the effective set, item components, quantities, XP, stock limit and definition hash for each eligible result. Empty effective fields identify excluded offers. The [review inputs](review.tsv) preserve row numbers and many-to-one and one-to-many mappings.

Run `python -B tools/validate_trades.py` in this project to compare the overlay and report with the pinned 1.12 Workbench matrix and the official 1.12.5 distribution.
