# 85. Daily Task Report Rules

Status: Current business baseline for BA / QA / client review

## Purpose
Explain how the Daily Task Report summarizes medical and DDF operational work.

## Business Summary
Daily Task Report is an operational workload report. It shows work by business stage for Medical claims and DDF claims, with different grouping rules for each table.

## Main Business Rules
- The report has Medical and DDF sections.
- Medical rows are grouped staff-category wise where the layout requires staff categories.
- DDF rows are total-based where the latest business rule does not require staff-category breakdown.
- Medical Not Yet Processed ignores date and shows all applicable under-review work not past first check.
- Medical Have to complete final check ignores date and shows all applicable claims waiting final check.
- Medical Have to Prepare Payment attachments ignores date and shows all applicable finalized-decision claims without finalized payment attachment.
- Draft payment attachments do not reduce Have to Prepare Payment attachments.
- Finalized payment attachments reduce Have to Prepare Payment attachments.
- DDF Claim Received, Not Yet Processed, and Have to complete Final check are total based.
- DDF Have to Prepare Payment shows approved pending payment items only.
- DDF rejected pending items are counted under Payments Completed as Reject Claim Total.
- DDF Payments Completed shows payment advice total and reject claim total.
- Other Works text is taken from payload and displayed in the relevant section.

## BA Review Points
- Confirm exact workflow state for first check complete and final check complete.
- Confirm whether date should be ignored for each pending-work column.
- Confirm rejected DDF items should stay under Payments Completed, not Have to Prepare Payment.

## QA Checkpoints
- Test Medical staff-category counts.
- Test DDF total counts without staff-category grouping.
- Create draft payment attachment and confirm pending count remains.
- Finalize payment attachment and confirm pending count reduces.
- Verify payload Other Works text appears in Excel.

## Client View
- The report should show what the team must work on now, not only what was created today.

## Related Topics
- 45. Insurance Claim Approval Workflow
- 46. Death Claim Approval Workflow
- 61. Payment Attachment Creation
