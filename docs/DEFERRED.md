# Deferred Items

Items from review reports that are explicitly deferred with rationale.

---

## R-01: Merge DailySummaryWorker into AccountabilityWorker

**Status:** Deferred — not actionable.

**Rationale:** Neither `DailySummaryWorker` nor `AccountabilityDispatchWorker` exist in the codebase. They appear only in design documents (`PRDv2.md`, `PRDv3.md`, `Design.md`). There is nothing to merge. This item will become actionable only when those workers are implemented.

**Reference:** Review Report v3, C-04.
