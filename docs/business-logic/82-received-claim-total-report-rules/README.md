# 82. Received Claim Total Report Rules

Status: Current business baseline for BA / QA / client review

## Purpose
Explain how the Received Claim Total Report counts received, pending, settled, rejected, and processing claims.

## Business Summary
This report summarizes third-party medical claims, WeCare medical claims, and DDF claims for a selected period. Different tables have different claim sources and staff grouping rules.

## Main Business Rules
- Medical third-party table is based on third-party claim import data where applicable.
- WeCare table is based on system-created medical claims.
- DDF table is based on system-created death donation claims.
- Normal Staff is included in the report where business requires it.
- DDF All Staff includes Normal Staff and other staff categories.
- Settled claims means final APPROVED or REJECTED claims at L2 or L3 decision level.
- Rejected claim count includes fully rejected claims and partially rejected claims where approved amount is less than requested amount.
- Not Yet Processed means UNDER_REVIEW claims that have not completed the first check business stage.
- Still Processing means UNDER_REVIEW claims that are already past the first check stage but not finally approved or rejected.
- Removed columns and removed tables should not appear in the current Excel output.

## BA Review Points
- Confirm the exact definition of first check complete.
- Confirm whether partially approved claims should always be counted as rejected for this report.
- Confirm which table sources are third-party only and which are WeCare only.

## QA Checkpoints
- Test received, settled, rejected, partially rejected, not-yet-processed, and still-processing counts.
- Compare report totals with claim workflow status and approval workflow level.
- Verify removed columns/tables are not shown in Excel.

## Client View
- The report should clearly show how many claims were received, completed, rejected, and still pending for each business group.

## Related Topics
- 45. Insurance Claim Approval Workflow
- 66. Medical Claims Report Rules
- 84. Rejected Claim Report Rules
