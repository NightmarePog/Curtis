# Skill Observation Log

Observations captured during task-oriented work.

**Status key:** OPEN = not yet actioned | ACTIONED (YYYY-MM-DD) = skill updated/created | DECLINED (YYYY-MM-DD) = user decided not to pursue

---

## 2026-08-02

### Observation 1: Check New Collection Column Names Against Test Databases

**Status:** OPEN
**Date:** 2026-08-02
**Session context:** Added a JPA element collection for matching-question pairs.
**Skill:** Existing implementation workflow
**Type:** open-source
**Phase/Area:** Persistence and verification

**Issue:** H2 did not create the new matching-pairs table because the embedded field names `left` and `right` conflicted with SQL keywords; the failure only appeared when an endpoint flushed the collection.

**Suggested improvement:** When adding embedded collections, use explicit non-keyword database column names and run an endpoint-level persistence test rather than relying only on entity construction/import assertions.

**Principle:** Persistence mappings must be verified against the actual test database, including reserved-word handling and flush-time behavior.

### Observation 2: Preserve Per-Question Grading State In API Responses

**Status:** OPEN
**Date:** 2026-08-02
**Session context:** Extended a frontend for discriminated quiz questions and free-text grading.
**Skill:** Existing implementation workflow
**Type:** open-source
**Phase/Area:** API contract review

**Issue:** The student results response exposes question metadata and aggregate score, but not each question's awarded points or review status, so the frontend can only label free-text questions as pending by type.

**Suggested improvement:** Include per-question awarded points and review status in result DTOs when a UI must distinguish pending free-text answers from graded or unanswered ones.

**Principle:** UI states that affect correctness should be represented explicitly in the API contract rather than inferred from question type.

- 2026-08-02 checkpoint: no observations
- 2026-08-02 checkpoint: no observations
