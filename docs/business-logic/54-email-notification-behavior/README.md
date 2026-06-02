# 54. Email Notification Behavior

Status: Current business baseline for BA / QA / client review

## Purpose
Explain when system emails are sent and who receives them.

## Business Summary
Email notifications are used for approval visibility and operational alerts. Recipients are selected from business context such as company, role, and configured recipient lists.

## Main Business Rules
- Dependent pending approval emails are sent when an employee adds dependent records from the app.
- Civil status pending approval emails are sent when marriage-related data needs approval.
- Dependent and civil status emails are sent to active web users assigned to the employee company.
- Current dependent/civil status recipient role codes are HRADMIN, DevTest, SUPERADMIN, APPROVER, ADMIN, CLAIMS_APPROVER, W_CSA, and HR_ADMIN.
- Current dependent/civil status email selection checks role code and company assignment; it does not check DPNM page privilege.
- Claim approval and rejection emails should show only L2 or L3 final remarks where final approval or rejection has passed L1.
- Monitoring emails from Uptime Kuma are operational alerts and are outside application claim workflow.

## BA Review Points
- Confirm final recipient roles for dependent and civil status approval emails.
- Confirm whether page privilege should replace or supplement role-code based routing.
- Confirm final remark visibility for approval emails.

## QA Checkpoints
- Add dependent and verify only same-company HR/admin recipients receive email.
- Verify inactive web users and empty emails are ignored.
- Verify duplicate recipient email addresses are not mailed twice.
- Verify L1 remarks do not appear where only L2 or L3 final remarks are required.

## Client View
- Approval users for the correct company should receive actionable emails without exposing unrelated company data.

## Related Topics
- 23. Dependent Approval/Rejection
- 45. Insurance Claim Approval Workflow
- 77. Email Service Integration
