# 81. Third Party Indoor Claim Import Rules

Status: Current business baseline for BA / QA / client review

## Purpose
Explain the Excel upload and validation rules for third-party indoor claim imports.

## Business Summary
Third-party indoor import creates indoor medical claims from a structured Excel file. The file format is controlled so claim amounts, paid amounts, policy period, and company/employee matching stay consistent.

## Main Business Rules
- Upload rows are matched by third-party reference number, company code, and employee EPF number.
- Current Excel format uses Policy No, Policy Period From, Policy Period To, Intimated Date, Paid Date, Non Payable Amount, Non Payable Item, Claim Amount, Paid Amount, and Remark.
- Hospital, disease, and policy year columns are no longer part of the current upload format.
- Paid Amount is entered manually in the upload and maps to the claim approved/paid amount.
- Claim Amount is stored from the uploaded value when it is provided and greater than zero.
- If Claim Amount is blank, null, or zero, the system calculates Claim Amount as Non Payable Amount plus Paid Amount.
- Null numeric values used for calculation are treated as zero.
- Policy period dates must match the applicable employee insurance policy period.
- Duplicate rows and invalid rows are returned in validation before import.

## BA Review Points
- Confirm whether Claim Amount zero should be allowed or recalculated.
- Confirm whether Paid Amount can be greater than Claim Amount when Claim Amount is manually provided.
- Confirm mandatory columns and display names in the downloadable Excel format.

## QA Checkpoints
- Validate upload with manual Claim Amount.
- Validate upload with blank or zero Claim Amount and verify calculated value.
- Validate policy period mismatch and duplicate reference number scenarios.
- Verify downloaded Excel template matches required column names.

## Client View
- Third-party uploads should be predictable and should not require users to calculate values the system can safely calculate.

## Related Topics
- 37. Claim Request Date Validation
- 38. Claim Amount Validation
- 66. Medical Claims Report Rules
