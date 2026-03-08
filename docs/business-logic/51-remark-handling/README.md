# 51. Remark Handling

Status: Current business baseline for BA / QA / client review

## Purpose
Explain the current business understanding of remark handling so BA, QA, and client-side reviewers can validate the rule in the same language.

## Business Summary
Explains how remark handling rule appears in approval, summary, or claim review flows.

## Main Business Rules
- Workflow remarks may exist on approved and rejected steps, not only on final rejection.
- Where one output needs one remark, the latest meaningful workflow remark should usually be used first.
- If no workflow remark exists, the base claim remark may be used as fallback.
- The same remark-selection rule should be used consistently unless a screen has a documented exception.

## BA Review Points
- Confirm the business meaning of the result on each screen.
- Confirm which workflow step updates or displays the result.
- Confirm how the same result should appear to admins, employees, and auditors.

## QA Checkpoints
- Test workflow, view, and filter scenarios that depend on this rule.
- Verify detail and summary outputs stay aligned.
- Verify amounts, statuses, and remarks remain consistent.

## Client View
- Approvers should see reliable information for decision making.
- Employees should see final outcomes that match the real workflow result.

## Related Topics
- 50. Rejection Logic
- 52. Final Approval Behavior
