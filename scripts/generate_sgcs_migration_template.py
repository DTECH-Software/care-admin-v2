from pathlib import Path

import xlsxwriter


OUTPUT = Path(__file__).resolve().parents[1] / "docs" / "migration" / "SGCS_Legacy_Data_Migration_Template.xlsx"

LISTS = {
    "Title": ["MR", "MS", "MRS", "MISS"],
    "Gender": ["MALE", "FEMALE"],
    "Marital Status": ["MARRIED", "UNMARRIED", "SINGLE", "DIVORCE"],
    "Facility": ["INSURANCE", "DEATH", "BOTH"],
    "User Status": ["ACTIVE", "INACTIVE", "DELETE"],
    "Login Status": ["ACTIVE", "INACTIVE", "DELETE"],
    "Is Temporary": ["YES", "NO"],
    "Dependent Category": ["PARENTS", "CHILDREN", "SPOUSE", "SIBLING"],
    "Relation Category": ["MOTHER", "FATHER", "CHILD", "WIFE", "HUSBAND", "FATHER_IN_LAW", "MOTHER_IN_LAW", "BROTHER", "SISTER"],
    "Eligible Facility": ["INSURANCE", "DEATH", "BOTH"],
    "Approval Status": ["UNDER_REVIEW", "APPROVED", "REJECTED", "ACTIVE"],
    "Live Status": ["YES", "NO"],
    "Claim Status": ["UNDER_REVIEW", "APPROVED", "REJECTED", "ACTIVE"],
    "Treatment Type": ["INDOOR", "OUTDOOR"],
    "Treatment Category": ["DENTAL", "SPECTACLE", "OTHER"],
    "Payment Type": ["FULL", "HALF"],
    "Migration Status": ["PENDING", "VALIDATED", "IMPORTED", "FAILED", "SKIPPED"],
}

SHEETS = {
    "Employee Details": "Company Code*|Legacy Employee ID*|EPF No*|Username|Title*|Initials*|First Name*|Last Name*|NIC*|Email*|Mobile No*|Gender*|Marital Status*|Date of Birth*|Address Line 1*|Address Line 2|City*|Staff Category Code*|Staff Type Code*|Designation*|Permanent Date*|Previous Permanent Date|Transfer Date|Previous Staff Category Code|Insurance Policy Code|Previous Insurance Policy Code|Payment Company Code|DDF Payment Company Code|Facility*|User Status*|Login Status*|Is Temporary*|Profile Image Reference|Birth Document Reference|Source File / Row|Migration Status|Migration Error",
    "Dependent Details": "Company Code*|Legacy Dependent ID*|Employee EPF No*|Employee NIC*|Dependent Category*|Relation Category*|Initials*|First Name*|Last Name*|Date of Birth*|Gender*|NIC|Job Title|Eligible Facility*|Approval Status*|Live Status*|Approved Date|Approved User|Remark|Document Folder / Reference|Source File / Row|Migration Status|Migration Error",
    "Spectacle Claims": "Company Code*|Legacy Claim ID*|Request ID|Employee EPF No*|Employee NIC*|Dependent Legacy ID|Dependent NIC|From Treatment Date|To Treatment Date*|Disease / Diagnosis|Requested Amount*|Approved Amount|Claim Status*|Policy Period ID / Code|Insurance Policy Code|Reject Reason Code|Reject Remark|Created Date*|Approved Date|Approved User|Document Folder / Reference|Source File / Row|Migration Status|Migration Error",
    "Critical Illness": "Company Code*|Legacy Claim ID*|Request ID|Employee EPF No*|Employee NIC*|Dependent Legacy ID|Dependent NIC|Treatment Category*|From Treatment Date|To Treatment Date*|Disease / Diagnosis|Requested Amount*|Approved Amount|Claim Status*|Policy Period ID / Code|Insurance Policy Code|Reject Reason Code|Reject Remark|Created Date*|Approved Date|Approved User|Document Folder / Reference|Source File / Row|Migration Status|Migration Error",
    "Indoor Outdoor": "Company Code*|Legacy Claim ID*|Request ID|Employee EPF No*|Employee NIC*|Dependent Legacy ID|Dependent NIC|Treatment Type*|Treatment Category*|From Treatment Date|To Treatment Date*|Disease / Diagnosis|Requested Amount*|Approved Amount|Claim Status*|Policy Period ID / Code|Insurance Policy Code|Reject Reason Code|Reject Remark|Created Date*|Approved Date|Approved User|Document Folder / Reference|Source File / Row|Migration Status|Migration Error",
    "DDF Claims": "Company Code*|Legacy DDF Claim ID*|Request ID|Employee EPF No*|Employee NIC*|Dependent Legacy ID*|Dependent NIC|Death Date*|Payment Type*|Utilized Amount*|Approved Amount|Claim Status*|Beneficiary Name|Beneficiary NIC|Beneficiary Relationship|Remark|Reject Reason Code|Created Date*|Approved Date|Approved User|Document Folder / Reference|Source File / Row|Migration Status|Migration Error",
}

EXAMPLES = {
    "Company Code": "SGCS", "Legacy Employee ID": "LEG-EMP-0001", "Legacy Dependent ID": "LEG-DEP-0001",
    "Legacy Claim ID": "LEG-CLM-0001", "Legacy DDF Claim ID": "LEG-DDF-0001", "EPF No": "001234",
    "Employee EPF No": "001234", "Username": "001234", "NIC": "199012345V", "Employee NIC": "199012345V",
    "Title": "MR", "Initials": "A.B.", "First Name": "Nimal", "Last Name": "Perera", "Email": "nimal@example.com",
    "Mobile No": "0771234567", "Gender": "MALE", "Marital Status": "MARRIED", "Date of Birth": "1990-05-14",
    "Address Line 1": "No. 10 Main Road", "City": "Colombo", "Staff Category Code": "EX-OP1", "Staff Type Code": "PERM",
    "Designation": "Executive", "Permanent Date": "2020-01-01", "Facility": "BOTH", "Eligible Facility": "BOTH",
    "User Status": "ACTIVE", "Login Status": "ACTIVE", "Is Temporary": "NO", "Dependent Category": "SPOUSE",
    "Relation Category": "WIFE", "Approval Status": "APPROVED", "Live Status": "YES", "Treatment Type": "OUTDOOR",
    "Treatment Category": "OTHER", "Payment Type": "FULL", "Claim Status": "APPROVED", "Requested Amount": 5000,
    "Approved Amount": 4500, "Utilized Amount": 500000, "Migration Status": "PENDING", "Source File / Row": "legacy.xlsx / row 2",
}


def main():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    workbook = xlsxwriter.Workbook(OUTPUT)
    required = workbook.add_format({"bold": True, "font_color": "white", "bg_color": "#F4B183", "border": 1, "align": "center", "valign": "vcenter", "text_wrap": True})
    optional = workbook.add_format({"bold": True, "font_color": "white", "bg_color": "#4472C4", "border": 1, "align": "center", "valign": "vcenter", "text_wrap": True})
    example = workbook.add_format({"bg_color": "#D9EAF7"})
    duplicate = workbook.add_format({"bg_color": "#F4CCCC"})
    title = workbook.add_format({"bold": True, "font_color": "white", "bg_color": "#17365D", "font_size": 16, "align": "center"})

    instructions = workbook.add_worksheet("Instructions")
    instructions.hide_gridlines(2)
    instructions.merge_range("A1:F1", "SGCS Legacy Data Migration Template", title)
    instructions.set_column("A:A", 12)
    instructions.set_column("B:F", 24)
    notes = [
        "Import Employee Details, then Dependent Details, then claim sheets.",
        "Orange headers are mandatory. Delete example row 2 before submitting live data.",
        "NIC accepts 9 digits plus V/X or exactly 12 digits. Optional NIC fields may be blank.",
        "Use YYYY-MM-DD dates and numeric amounts without Rs. or other text.",
        "Use SGCS as Company Code and identical EPF/NIC values across linked sheets.",
        "Legacy IDs must be unique. Leave Migration Status=PENDING and Migration Error blank.",
        "Use file/folder references for documents; do not embed confidential documents.",
        "Encrypt and restrict this workbook because it contains personal and medical data.",
    ]
    for row, note in enumerate(notes, 2):
        instructions.write(row, 0, row - 1)
        instructions.merge_range(row, 1, row, 5, note)

    for sheet_name, header_text in SHEETS.items():
        headers = header_text.split("|")
        sheet = workbook.add_worksheet(sheet_name)
        sheet.hide_gridlines(2)
        sheet.freeze_panes(1, 0)
        sheet.autofilter(0, 0, 999, len(headers) - 1)
        sheet.set_row(0, 42)
        for column, raw_header in enumerate(headers):
            is_required = raw_header.endswith("*")
            header = raw_header.rstrip("*")
            sheet.write(0, column, header, required if is_required else optional)
            sheet.write(1, column, EXAMPLES.get(header, ""), example)
            sheet.set_column(column, column, max(14, min(30, len(header) + 3)))
            if header == "Company Code":
                sheet.data_validation(1, column, 999, column, {"validate": "list", "source": ["SGCS"]})
            elif header in LISTS:
                sheet.data_validation(1, column, 999, column, {"validate": "list", "source": LISTS[header], "ignore_blank": not is_required})
            if header in {"NIC", "Employee NIC", "Dependent NIC", "Beneficiary NIC"}:
                cell = f"{xlsxwriter.utility.xl_col_to_name(column)}2"
                blank = f'{cell}="",' if not is_required else ""
                formula = f'=OR({blank}AND(LEN({cell})=10,ISNUMBER(--LEFT({cell},9)),OR(UPPER(RIGHT({cell},1))="V",UPPER(RIGHT({cell},1))="X")),AND(LEN({cell})=12,ISNUMBER(--{cell})))'
                sheet.data_validation(1, column, 999, column, {"validate": "custom", "value": formula, "ignore_blank": not is_required, "error_message": "Use 9 digits + V/X or 12 digits."})
            elif "Date" in header:
                sheet.data_validation(1, column, 999, column, {"validate": "date", "criteria": "between", "minimum": "1900-01-01", "maximum": "2100-12-31", "ignore_blank": not is_required})
            elif "Amount" in header:
                sheet.data_validation(1, column, 999, column, {"validate": "decimal", "criteria": ">=", "value": 0, "ignore_blank": not is_required})
        sheet.conditional_format(1, 1, 999, 1, {"type": "formula", "criteria": '=AND($B2<>"",COUNTIF($B:$B,$B2)>1)', "format": duplicate})

    workbook.close()
    print(OUTPUT)


if __name__ == "__main__":
    main()
