# 10. Insurance Policy Master Data

Status: Current business baseline for BA / QA / client review

## Purpose
Explain the current business understanding of insurance policy master data so BA, QA, and client-side reviewers can validate the rule in the same language.

## Business Summary
Explains how insurance policy master data setup is maintained and reused across the system.

## Main Business Rules
- Setup should be controlled centrally and used consistently in downstream flows.
- Active/inactive status should control new usage.
- Historical records should stay readable even after setup changes.
- The same setup should drive screens, APIs, and reports consistently.

## BA Review Points
- Confirm required fields and active/inactive behavior.
- Confirm which modules depend on this setup.
- Confirm whether changes affect only future transactions or also historical displays.

## QA Checkpoints
- Test create, update, inactivate, and reference usage scenarios.
- Verify inactive values are blocked where required.
- Verify historical records still display meaningful values.

## Client View
- Configured values should be stable and consistent.
- Business users should not see contradictory setup values.

## Related Topics
- 09. Treatment Category Master Data
- 11. Insurance Period Master Data
