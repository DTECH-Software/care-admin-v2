# 84. Rejected Claim Report Rules

Status: Current business baseline for BA / QA / client review

## Purpose
Explain how rejected and partially rejected claims are counted and grouped.

## Business Summary
Rejected Claim Report groups rejected claim outcomes by company, staff group, policy period, and return reason. It includes both full rejections and partial rejections.

## Main Business Rules
- Fully rejected claims are claims with final status REJECTED.
- Partially rejected claims are approved claims where approved amount is less than requested amount.
- Both full rejected and partially rejected claims are included in rejected claim totals.
- Report can be filtered by policy period where period is provided.
- If period is not provided, the default period selection follows report default period logic.
- Return reasons are grouped from final remarks/rejection reasons.
- Mismatched or unrecognized reasons should be grouped under Other.
- Staff-wise and All Staff views are supported where the report layout requires them.

## BA Review Points
- Confirm approved amount less than request amount should always count as partial rejection.
- Confirm final source of return reason for partial approvals.
- Confirm policy period default behavior.

## QA Checkpoints
- Test fully rejected claim count.
- Test partially rejected approved claim count.
- Test reason grouping, including Other.
- Test period filter in filter-list and export.

## Client View
- Rejected Claim Report should show both full rejected and partially rejected business outcomes.

## Related Topics
- 49. Partial Approval Logic
- 50. Rejection Logic
- 82. Received Claim Total Report Rules
