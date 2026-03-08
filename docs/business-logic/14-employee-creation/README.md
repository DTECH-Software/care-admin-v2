# 14. Employee Creation

Status: Current business baseline for BA / QA / client review

## Purpose
Explain the current business understanding of employee creation so BA, QA, and client-side reviewers can validate the rule in the same language.

## Business Summary
Explains how employee creation employee or dependent data affects claim behavior.

## Main Business Rules
- The data should come from one clear profile source.
- Only dependent business areas should change after profile updates.
- Historical claims should remain understandable after updates.
- Invalid profile data should not silently drive claim decisions.

## BA Review Points
- Confirm who can create or update the data.
- Confirm which claim rules depend on this data.
- Confirm what should happen to historical records after profile updates.

## QA Checkpoints
- Test create, update, and invalid-data scenarios.
- Verify claim behavior changes only where expected.
- Verify historical claims remain readable.

## Client View
- Profile changes should affect claims in a predictable way.
- Users should understand why a profile change affects a balance or claim.

## Related Topics
- 13. Insurance Quarter Setup
- 15. Employee Update
