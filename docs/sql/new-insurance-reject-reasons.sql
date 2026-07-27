-- Adds the non-duplicate insurance reject reasons from Copy of REJECT REASONS.xlsx.
-- Code is based on the workbook number plus 22, preserving traceability while leaving gaps for duplicates.
-- The final column follows the workbook report YES/NO selection.
INSERT INTO remark (
    remark_category, code, description, status,
    created_date, last_modified_date, created_user, last_modified_user,
    include_in_rejected_claim_report
)
SELECT
    'INSURANCE',
    CONCAT('MSMD', source_no + 22),
    description,
    'ACTIVE',
    NOW(), NOW(), 'admin', 'admin', include_in_report
FROM (
    SELECT 3 source_no, 'AYURVEDIC TREATMENT COVERED ACCIDENTAL DAMAGES ONLY' description, 1 include_in_report UNION ALL
    SELECT 4 source_no, 'BILL NOT CLEAR' description, 1 include_in_report UNION ALL
    SELECT 5 source_no, 'BLACKLISTED HOSPITAL NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 9 source_no, 'EMPLOYEE ENTERED TO THE SYSTEM AFTER THE BILL DATE' description, 1 include_in_report UNION ALL
    SELECT 11 source_no, 'DEPENDENT NOT INCLUDED TO THE SYSTEM' description, 1 include_in_report UNION ALL
    SELECT 15 source_no, 'REQUIRED FIRST PAGE OF CLINIC BOOK' description, 1 include_in_report UNION ALL
    SELECT 17 source_no, 'NOT A MEDICINE' description, 1 include_in_report UNION ALL
    SELECT 23 source_no, 'SPECTACLE COVER LIMIT EXCEED' description, 1 include_in_report UNION ALL
    SELECT 24 source_no, 'BILL DATE PRIOR TO THE PRESCRIPTION DATE' description, 1 include_in_report UNION ALL
    SELECT 25 source_no, 'DETAILS NOT CLEAR' description, 1 include_in_report UNION ALL
    SELECT 26 source_no, 'DUPLICATE COPY NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 30 source_no, 'NEED PROPER BILL' description, 1 include_in_report UNION ALL
    SELECT 31 source_no, 'UNDATED BILL' description, 1 include_in_report UNION ALL
    SELECT 32 source_no, 'VITAMIN TREATMENTS FOR BEAUTY CARE TREATMENT NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 33 source_no, 'DEPENDENT NAME DIFFER WITH THE SYSTEM' description, 1 include_in_report UNION ALL
    SELECT 34 source_no, 'SPECTACLE COVER ONCE IN 03 YEARS' description, 1 include_in_report UNION ALL
    SELECT 35 source_no, 'REFUNDED ABST TEST NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 36 source_no, 'SAME BILL NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 37 source_no, 'NEED CLEAR PRESCRIPTION' description, 1 include_in_report UNION ALL
    SELECT 39 source_no, 'EMPLOYEE NAME DIFFER WITH THE SYSTEM' description, 1 include_in_report UNION ALL
    SELECT 40 source_no, 'BILL NAME DIFFER WITH THE PRESCRIPTION NAME' description, 1 include_in_report UNION ALL
    SELECT 41 source_no, 'NAME AND AGE DIFFER WITH THE SYSTEM' description, 1 include_in_report UNION ALL
    SELECT 42 source_no, 'EMPLOYEE NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 43 source_no, 'NAME AND DATE ALTERED' description, 1 include_in_report UNION ALL
    SELECT 44 source_no, 'FRP NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 45 source_no, 'TAXES NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 46 source_no, 'BANK SLIPS NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 47 source_no, 'CREDIT CARD CHARGES NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 48 source_no, 'NEED ORIGINAL BILLS' description, 1 include_in_report UNION ALL
    SELECT 49 source_no, 'NO NAME AND AGE IN THE PRESCRIPTION' description, 1 include_in_report UNION ALL
    SELECT 50 source_no, 'ACCIDENT OCCURRED UNDER ALCOHOLISM NOT COVERD' description, 1 include_in_report UNION ALL
    SELECT 51 source_no, 'EMPLOYEE RESIGN' description, 1 include_in_report UNION ALL
    SELECT 52 source_no, 'LASER TREATMENT NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 53 source_no, 'ABORTIONS AND MISCARRIAGES NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 54 source_no, 'NEED ORIGINAL BILL AND RECEIPT' description, 1 include_in_report UNION ALL
    SELECT 55 source_no, 'RE-PRINTED BILLS NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 56 source_no, 'PARENT NOT COVERED DUE TO MARRIED EMPLOYEE' description, 1 include_in_report UNION ALL
    SELECT 57 source_no, 'RESUBMISSION SHOULD BE SUBMITTED WITHIN 01 MONTH FROM THE FIRST REJECTION DATE' description, 1 include_in_report UNION ALL
    SELECT 58 source_no, 'COMPANY PREMIUM NOT RECEIVED' description, 1 include_in_report UNION ALL
    SELECT 59 source_no, 'EMPLOYEE PREMIUM NOT RECEIVED' description, 1 include_in_report UNION ALL
    SELECT 60 source_no, 'DOSAGE NOT MENTIONED IN THE PRESCRIPTION' description, 1 include_in_report UNION ALL
    SELECT 61 source_no, 'SPECTACLE CLAIM PROCEED WITH INDOOR CATEGORY' description, 1 include_in_report UNION ALL
    SELECT 63 source_no, 'NON-PAYING WARDS COVERED FOR MAXIMUM OF 21 DAYS ONLY' description, 1 include_in_report UNION ALL
    SELECT 64 source_no, 'UNMARRIED EMPLOYEE''S PARENTS ARE COVERED UP TO AGE 65 YEARS ONLY' description, 1 include_in_report UNION ALL
    SELECT 65 source_no, 'ALL EYE TREATMENTS ARE COVERED UNDER THE OPD LIMIT' description, 1 include_in_report UNION ALL
    SELECT 66 source_no, 'BIRTH CONTROL TREATMENT NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 67 source_no, 'EMPLOYEE NOT COVERED PREVIOUS POLICY PERIOD' description, 1 include_in_report UNION ALL
    SELECT 68 source_no, 'UNDATED BILL AND PRESCRIPTION' description, 1 include_in_report UNION ALL
    SELECT 69 source_no, 'BEAUTY CARE TREATMENTS NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 70 source_no, 'COSMATIC SURGERY, COSMATIC TREATMENT AND PLASTIC SURGERY NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 71 source_no, 'ATTEMPTED SUICIDE, VENEREAL DISEASE, PSYCHOTIC MENTAL OR NERVOUS DISORDERS NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 72 source_no, 'PARTICIPATION IN STRIKES AND RIOTS TREATMENTS NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 73 source_no, 'ROUTINE MEDICAL CHECKUPS / BODY HEALTH CHECKUPS NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 74 source_no, 'NON MEDICAL SERVICES NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 75 source_no, 'CONGENITIAL CONDITIONS' description, 1 include_in_report UNION ALL
    SELECT 76 source_no, 'CHANNELLING RECEIPTS AND PRESCRIPTIONS ISSUED THROUGH OPTICIANS NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 77 source_no, 'SPECIAL CLINIC AND PROMOTION PACKAGES CONDUCT BY THIRD PARTY ORGANIZATION NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 78 source_no, 'SPECTACLES, TESTS, INVESTIGATION MEDICAL EXAMINATION DRUGS, TREATMENTS,REPORTS ISSUED THROUGH MOBILE CLINICS NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 79 source_no, 'PLEASE CROSS THE BILLS, MENTION THE CLAIM SUBMISSION DATE ON THE BILLS, PLACE YOUR SIGNATURE & RESUBMIT THE CLAIM' description, 1 include_in_report UNION ALL
    SELECT 80 source_no, 'LENS KIT FOR CATARACT SURGERIES LIMIT EXCEED' description, 1 include_in_report UNION ALL
    SELECT 81 source_no, 'LOCAL ANESTHESIA COVER LIMIT EXCEED' description, 1 include_in_report UNION ALL
    SELECT 83 source_no, 'NOT MBBS DOCTOR' description, 1 include_in_report UNION ALL
    SELECT 84 source_no, 'PROCEED THROUGH INDOOR LIMIT' description, 1 include_in_report UNION ALL
    SELECT 85 source_no, 'PROCEED THROUGH OUTDOOR LIMIT' description, 1 include_in_report UNION ALL
    SELECT 86 source_no, 'SPECTACLE CLAIM APPROVED' description, 0 include_in_report UNION ALL
    SELECT 87 source_no, 'SPECTACLE CLAIM REJECTED' description, 1 include_in_report UNION ALL
    SELECT 88 source_no, 'DENTAL CLAIM APPROVED' description, 0 include_in_report UNION ALL
    SELECT 89 source_no, 'DENTAL CLAIM REJECTED' description, 1 include_in_report UNION ALL
    SELECT 90 source_no, 'INCORRECT STAFF CATEGORY' description, 1 include_in_report UNION ALL
    SELECT 91 source_no, 'REQUIRED FIRST PAGE OF PRESCRIPTION' description, 1 include_in_report UNION ALL
    SELECT 92 source_no, 'ECG TEST COVERED UNDER THE INDOOR LIMIT' description, 1 include_in_report UNION ALL
    SELECT 93 source_no, 'EX-ECG TEST COVERED UNDER THE INDOOR LIMIT' description, 1 include_in_report UNION ALL
    SELECT 94 source_no, 'ECHO TEST COVERED UNDER THE INDOOR LIMIT' description, 1 include_in_report UNION ALL
    SELECT 95 source_no, 'USS TEST COVERED UNDER THE INDOOR LIMIT' description, 1 include_in_report UNION ALL
    SELECT 96 source_no, 'LIPID PROFILE TEST COVERED UNDER THE INDOOR LIMIT' description, 1 include_in_report UNION ALL
    SELECT 97 source_no, 'MRI SCAN COVERED UNDER THE INDOOR LIMIT' description, 1 include_in_report UNION ALL
    SELECT 98 source_no, 'CT SCAN COVERED UNDER THE INDOOR LIMIT' description, 1 include_in_report UNION ALL
    SELECT 99 source_no, 'ANGIOGRAM COVERED UNDER THE INDOOR LIMIT' description, 1 include_in_report UNION ALL
    SELECT 100 source_no, 'PLEASE SUBMIT THE ORIGINAL BILL ALONG WITH THE PRESCRIPTION TO SGCS HEALTHCARE DIVISION' description, 1 include_in_report UNION ALL
    SELECT 101 source_no, 'PLEASE MENTION THE CLAIM SUBMISSION DATE ON THE BILL' description, 1 include_in_report UNION ALL
    SELECT 102 source_no, 'DEPENDENT''S BILLS SHOULD BE SUBMITTED TO UNDER THE DEPENDENT CATEGORY' description, 1 include_in_report UNION ALL
    SELECT 103 source_no, 'EYE TREATMENTS BILLS SUBMITTED TO UNDER SPECTACLE TREATMENT CATEGORY' description, 1 include_in_report UNION ALL
    SELECT 104 source_no, 'DENTAL TREATMENTS BILLS SUBMITTED TO UNDER DENTAL TREATMENT CATEGORY' description, 1 include_in_report UNION ALL
    SELECT 105 source_no, 'OUTDOOR BILLS SUBMITTED UNDER OUTDOOR TREATMENT TYPE' description, 1 include_in_report UNION ALL
    SELECT 106 source_no, 'INDOOR BILLS SUBMITTED UNDER INDOOR TREATMENT TYPE' description, 1 include_in_report UNION ALL
    SELECT 107 source_no, 'CRITICAL ILLNESS BILLS SUBMITTED UNDER CRITICAL TREATMENT TYPE' description, 1 include_in_report UNION ALL
    SELECT 108 source_no, 'HOSPITAL NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 109 source_no, 'ELIGIBLE TO CLAIM THE CRITICAL ILLNESS BENEFIT ONCE IN A LIFETIME' description, 1 include_in_report UNION ALL
    SELECT 110 source_no, 'DISCOUNT NOT COVERED' description, 1 include_in_report UNION ALL
    SELECT 111 source_no, 'ANGIOGRAM APPROVED' description, 0 include_in_report UNION ALL
    SELECT 112 source_no, 'ANGIOGRAM REJECTED' description, 1 include_in_report UNION ALL
    SELECT 113 source_no, 'MRI SCAN APPROVED' description, 0 include_in_report UNION ALL
    SELECT 114 source_no, 'MRI SCAN REJECTED' description, 1 include_in_report UNION ALL
    SELECT 115 source_no, 'ENDOSCOPY APPROVED' description, 0 include_in_report UNION ALL
    SELECT 116 source_no, 'ENDOSCOPY REJECTED' description, 1 include_in_report UNION ALL
    SELECT 117 source_no, 'COLONOSCOPY APPROVED' description, 0 include_in_report UNION ALL
    SELECT 118 source_no, 'COLONOSCOPY REJECTED' description, 1 include_in_report UNION ALL
    SELECT 119 source_no, 'BRONCHOSCOPY APPROVED' description, 0 include_in_report UNION ALL
    SELECT 120 source_no, 'BRONCHOSCOPY REJECTED' description, 1 include_in_report UNION ALL
    SELECT 121 source_no, 'SIGMOIDOSCOPY APPROVED' description, 0 include_in_report UNION ALL
    SELECT 122 source_no, 'SIGMOIDOSCOPY REJECTED' description, 1 include_in_report UNION ALL
    SELECT 123 source_no, 'CT SCAN APPROVED' description, 0 include_in_report UNION ALL
    SELECT 124 source_no, 'CT SCAN REJECTED' description, 1 include_in_report UNION ALL
    SELECT 125 source_no, 'LAPAROSCOPY APPROVED' description, 0 include_in_report UNION ALL
    SELECT 126 source_no, 'LAPAROSCOPY REJECTED' description, 1 include_in_report UNION ALL
    SELECT 127 source_no, 'LOCAL ANESTHESIA APPROVED' description, 0 include_in_report UNION ALL
    SELECT 128 source_no, 'CATARACT LENS KIT APPROVED' description, 0 include_in_report UNION ALL
    SELECT 129 source_no, 'CATARACT LENS KIT REJECTED' description, 1 include_in_report
) new_reason
WHERE NOT EXISTS (
    SELECT 1
    FROM remark existing
    WHERE existing.remark_category = 'INSURANCE'
      AND (UPPER(TRIM(existing.code)) = UPPER(CONCAT('MSMD', new_reason.source_no + 22))
           OR UPPER(TRIM(existing.description)) = UPPER(TRIM(new_reason.description)))
);

-- Expected insertion count on the supplied existing data: 108.
-- Workbook rows identified as duplicates and intentionally not inserted:
-- 1, 2, 6, 7, 8, 10, 12, 13, 14, 16, 18, 19, 20, 21, 22, 27, 28, 29, 38, 62, 82.

-- Verification:
SELECT code, description, include_in_rejected_claim_report
FROM remark
WHERE remark_category = 'INSURANCE'
ORDER BY id;
