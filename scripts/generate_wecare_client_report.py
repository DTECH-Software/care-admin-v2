"""Generate the client-facing live insurance and migration report (DOCX + PDF).

The database connection is read-only at application level: this script executes
SELECT statements only and never commits changes.
"""

from __future__ import annotations

import re
from collections import Counter, defaultdict
from contextlib import closing
from datetime import date, datetime
from decimal import Decimal
from pathlib import Path
from xml.sax.saxutils import escape

from docx import Document
from docx.enum.section import WD_ORIENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

from wecare_spectacle import _connect


OUT_DIR = Path(__file__).resolve().parents[1] / "docs" / "reports"
REPORT_STEM = f"WeCare-Live-Insurance-Claims-Detailed-Report-{date.today():%Y-%m-%d}"
DOCX_PATH = OUT_DIR / f"{REPORT_STEM}.docx"
PDF_PATH = OUT_DIR / f"{REPORT_STEM}.pdf"

NAVY = "17365D"
BLUE = "2F75B5"
LIGHT_BLUE = "D9EAF7"
LIGHT_GRAY = "F2F2F2"
RED = "C00000"

BATCH_NEW = {
    "[MIGRATION:SGCS-NS-2025-INDOOR-R1]": 1,
    "[MIGRATION:SGCS-NS-2025-OUTDOOR-R1]": 5,
    "[MIGRATION:SGCS-NS-2026-INDOOR-R1]": 3,
    "[MIGRATION:SGCS-NS-2026-OUTDOOR-R1]": 6,
    "[MIGRATION:SGCS-NS-2026-OUTDOOR-R2]": 1,
    "[MIGRATION:SGCS-NS-2026-OUTDOOR-R3]": 1,
    "[MIGRATION:SGCS-NS-2026-OUTDOOR-R4]": 4,
    "[MIGRATION:SGCS-EXOP1-EXOP2-2025-OUTDOOR-R1]": 4,
    "[MIGRATION:SGCS-MM-2025-OUTDOOR-R1]": 2,
    "[MIGRATION:SGCS-SNR-2025-OUTDOOR-R1]": 3,
    "[MIGRATION:SGCS-SNR-2025-OUTDOOR-R2]": 2,
}

EXCLUSIONS = [
    ["Outdoor.xlsx", "SNR", "251", "SGCS-OUTDOOR-000019", "DENTAL", "Pending", "-", "Not added: pending row; later approved DENTAL row 000020 is used."],
    ["Outdoor.xlsx", "EXOP1", "98", "SGCS-OUTDOOR-000281", "OTHER", "Approved", "0.00", "Not added: approved amount is zero."],
    ["Outdoor.xlsx", "SNR", "110", "SGCS-OUTDOOR-000296", "OTHER", "Approved", "0.00", "Not added: approved amount is zero."],
    ["Outdoor.xlsx", "MM", "252", "SGCS-OUTDOOR-000299", "OTHER", "Pending", "-", "Not added: pending with no approved amount."],
    ["Outdoor.xlsx", "SNR", "197", "SGCS-OUTDOOR-000300", "OTHER", "Pending", "-", "Not added: pending row; later approved row 000303 is used."],
    ["Outdoor.xlsx", "EXOP1", "249", "SGCS-OUTDOOR-000360", "OTHER", "Pending", "-", "Not added: no approved amount and submitted after policy end."],
    ["indor.xlsx", "NS", "199", "SGCS-INDOOR-000014", "OTHER", "Approved", "0.00", "Not added: approved amount is zero."],
    ["Balance sheet", "EXOP1", "230", "No positive source claim", "-", "-", "0.00", "No claim inserted; full policy balance remains available."],
    ["Balance sheet", "EXOP1", "256", "No positive source claim", "-", "-", "0.00", "No claim inserted; full policy balance remains available."],
]

EXCEPTIONS = [
    ["EXOP1", "249", "SGCS-OUTDOOR-000356", "Pending / 1,900", "Approved / 1,900", "Client-confirmed approval; within the 2025-07-01 to 2026-06-30 policy."],
    ["MM", "181", "SGCS-OUTDOOR-000361", "Pending / 1,609; submitted after policy", "Approved / 1,690", "Client-confirmed balance-sheet exception, retained with an explicit audit source marker."],
    ["SNR", "29", "Claim 515", "UNDER_REVIEW / 3,640", "APPROVED / 3,640", "Client-confirmed approval; request and workflow both updated."],
    ["SNR", "29", "Claim 516", "UNDER_REVIEW / 3,000", "APPROVED / 3,000", "Client-confirmed approval; request and workflow both updated."],
]


def fetch(cur, sql: str, params=()) -> list[dict]:
    cur.execute(sql, params)
    return list(cur.fetchall())


def money(value) -> str:
    if value is None:
        return "-"
    return f"{Decimal(value):,.2f}"


def text(value) -> str:
    if value is None:
        return "-"
    if isinstance(value, (datetime, date)):
        return value.strftime("%Y-%m-%d")
    if isinstance(value, Decimal):
        return money(value)
    if isinstance(value, bytes):
        return "Yes" if value != b"\x00" else "No"
    return str(value)


def marker_name(remark: str) -> str:
    match = re.search(r"\[MIGRATION:[^\]]+\]", remark or "")
    return match.group(0) if match else ""


def source_name(remark: str) -> str:
    match = re.search(r"SOURCE:(.*)$", remark or "")
    value = match.group(1) if match else "-"
    return value.replace(" [CORRECTION:PERMANENT-DATE-QUARTER-R1]", "")


def load_data() -> dict:
    with closing(_connect()) as connection:
        connection.start_transaction(readonly=True)
        with closing(connection.cursor(dictionary=True)) as cur:
            snapshot = fetch(cur, "SELECT DATABASE() database_name,NOW() snapshot_time,VERSION() mysql_version")[0]
            staff = fetch(cur, "SELECT code,description,status FROM staff_category ORDER BY code")
            policies = fetch(cur, "SELECT code,description,status FROM insurance_policy ORDER BY code")
            employee_distribution = fetch(cur, """
                SELECT ucd.company_type,ucd.staff_category,ucd.insurance_policy,
                       ucd.facility,COUNT(*) active_employees
                FROM user_personal_details upd
                JOIN user_company_details ucd ON ucd.id=upd.user_company_details
                WHERE upd.user_status='ACTIVE'
                GROUP BY ucd.company_type,ucd.staff_category,ucd.insurance_policy,ucd.facility
                ORDER BY ucd.company_type,ucd.staff_category,ucd.insurance_policy,ucd.facility
            """)
            periods = fetch(cur, """
                SELECT isp.id period_id,isp.staff_category,isp.from_date,isp.to_date,
                       isp.status,GROUP_CONCAT(DISTINCT idl.insurance_policy ORDER BY idl.insurance_policy) policies,
                       COUNT(idl.id) configured_limits
                FROM insurance_staff_category_period isp
                LEFT JOIN insurance_details_limit idl ON idl.insurance_staff_category_period=isp.id
                GROUP BY isp.id,isp.staff_category,isp.from_date,isp.to_date,isp.status
                ORDER BY isp.from_date,isp.staff_category,isp.id
            """)
            limits = fetch(cur, """
                SELECT isp.id period_id,isp.staff_category,isp.from_date,isp.to_date,
                       idl.insurance_policy,idl.id limit_id,idl.treatment,
                       idl.global_limit,idl.is_quarter,idl.status
                FROM insurance_details_limit idl
                JOIN insurance_staff_category_period isp
                  ON isp.id=idl.insurance_staff_category_period
                ORDER BY isp.from_date,isp.staff_category,idl.insurance_policy,idl.treatment,idl.id
            """)
            quarters = fetch(cur, """
                SELECT isp.id period_id,isp.staff_category,isp.from_date policy_from,
                       isp.to_date policy_to,idl.insurance_policy,idl.treatment,
                       idl.id limit_id,iq.id quarter_id,iq.treatment_category_code,
                       iq.quarter_limit,iq.from_date,iq.to_date
                FROM insurance_quarter iq
                JOIN insurance_details_limit idl ON idl.id=iq.insurance_details_id
                JOIN insurance_staff_category_period isp
                  ON isp.id=idl.insurance_staff_category_period
                ORDER BY isp.from_date,isp.staff_category,idl.treatment,
                         iq.treatment_category_code,iq.from_date,iq.id
            """)
            claim_summary = fetch(cur, """
                SELECT cr.request_status,icd.treatment,icd.treatment_category,
                       COUNT(*) claim_count,SUM(cr.request_amount) requested,
                       SUM(COALESCE(cr.approved_amount,0)) approved
                FROM claims_request cr
                JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details
                GROUP BY cr.request_status,icd.treatment,icd.treatment_category
                ORDER BY icd.treatment,icd.treatment_category,cr.request_status
            """)
            usage = fetch(cur, """
                SELECT isp.id period_id,isp.from_date,isp.to_date,
                       upd.epf_no,CONCAT_WS(' ',upd.first_name,upd.last_name) employee_name,
                       ucd.staff_category,idl.insurance_policy,idl.treatment,
                       icd.treatment_category,COUNT(*) approved_claims,
                       SUM(cr.approved_amount) category_approved,
                       totals.total_approved,idl.global_limit,
                       idl.global_limit-totals.total_approved global_remaining
                FROM claims_request cr
                JOIN application_user au ON au.id=cr.employee
                JOIN user_personal_details upd ON upd.id=au.user_personal_details
                JOIN user_company_details ucd ON ucd.id=upd.user_company_details
                JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details
                JOIN insurance_details_limit idl ON idl.id=cr.insurance_details_limit_id
                JOIN insurance_staff_category_period isp
                  ON isp.id=idl.insurance_staff_category_period
                JOIN (
                    SELECT employee,insurance_details_limit_id,SUM(approved_amount) total_approved
                    FROM claims_request WHERE request_status='APPROVED'
                    GROUP BY employee,insurance_details_limit_id
                ) totals ON totals.employee=cr.employee
                        AND totals.insurance_details_limit_id=cr.insurance_details_limit_id
                WHERE cr.request_status='APPROVED'
                GROUP BY isp.id,isp.from_date,isp.to_date,upd.epf_no,upd.first_name,
                         upd.last_name,ucd.staff_category,idl.insurance_policy,
                         idl.treatment,icd.treatment_category,totals.total_approved,
                         idl.global_limit
                ORDER BY isp.from_date,ucd.staff_category,CAST(upd.epf_no AS UNSIGNED),
                         idl.treatment,icd.treatment_category
            """)
            migration_claims = fetch(cur, """
                SELECT cr.id claim_id,cr.request_id,cr.request_status,
                       cr.request_amount,cr.approved_amount,cr.created_date,
                       upd.epf_no,CONCAT_WS(' ',upd.first_name,upd.last_name) employee_name,
                       ucd.company_type,ucd.staff_category,idl.insurance_policy,
                       isp.from_date policy_from,isp.to_date policy_to,
                       icd.treatment,icd.treatment_category,cr.insurance_quarter_id,
                       cr.remark
                FROM claims_request cr
                JOIN application_user au ON au.id=cr.employee
                JOIN user_personal_details upd ON upd.id=au.user_personal_details
                JOIN user_company_details ucd ON ucd.id=upd.user_company_details
                JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details
                JOIN insurance_details_limit idl ON idl.id=cr.insurance_details_limit_id
                JOIN insurance_staff_category_period isp
                  ON isp.id=idl.insurance_staff_category_period
                WHERE cr.remark LIKE '%[MIGRATION:%'
                ORDER BY cr.id
            """)
            quarter_corrections = fetch(cur, """
                SELECT cr.id claim_id,upd.epf_no,cr.request_id,
                       idl.treatment,icd.treatment_category,
                       cr.insurance_quarter_id,icd.insurance_staff_category_period period_id
                FROM claims_request cr
                JOIN application_user au ON au.id=cr.employee
                JOIN user_personal_details upd ON upd.id=au.user_personal_details
                JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details
                JOIN insurance_details_limit idl ON idl.id=cr.insurance_details_limit_id
                WHERE cr.remark LIKE '%[CORRECTION:PERMANENT-DATE-QUARTER-R1]%'
                ORDER BY cr.id
            """)

            checks_sql = {
                "Approved claims missing approved amount": "SELECT COUNT(*) n FROM claims_request WHERE request_status='APPROVED' AND approved_amount IS NULL",
                "Approved claims missing a limit reference": "SELECT COUNT(*) n FROM claims_request WHERE request_status='APPROVED' AND insurance_details_limit_id IS NULL",
                "Approved claims with stored period different from attached limit": "SELECT COUNT(*) n FROM claims_request cr JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details JOIN insurance_details_limit idl ON idl.id=cr.insurance_details_limit_id WHERE cr.request_status='APPROVED' AND icd.insurance_staff_category_period IS NOT NULL AND icd.insurance_staff_category_period<>idl.insurance_staff_category_period",
                "Approved quarter-enabled claims without quarter reference": "SELECT COUNT(*) n FROM claims_request cr JOIN insurance_details_limit idl ON idl.id=cr.insurance_details_limit_id WHERE cr.request_status='APPROVED' AND idl.is_quarter=1 AND cr.insurance_quarter_id IS NULL",
                "Approved claims whose quarter does not match limit/category": "SELECT COUNT(*) n FROM claims_request cr JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details JOIN insurance_quarter iq ON iq.id=cr.insurance_quarter_id WHERE cr.request_status='APPROVED' AND (iq.insurance_details_id<>cr.insurance_details_limit_id OR iq.treatment_category_code<>icd.treatment_category)",
                "Duplicate request IDs": "SELECT COUNT(*) n FROM (SELECT request_id FROM claims_request GROUP BY request_id HAVING COUNT(*)>1) x",
                "Employee/limit approved totals above global limit": "SELECT COUNT(*) n FROM (SELECT cr.employee,cr.insurance_details_limit_id,SUM(cr.approved_amount) used,MAX(idl.global_limit) lim FROM claims_request cr JOIN insurance_details_limit idl ON idl.id=cr.insurance_details_limit_id WHERE cr.request_status='APPROVED' GROUP BY cr.employee,cr.insurance_details_limit_id HAVING used>lim) x",
            }
            checks = [[name, fetch(cur, sql)[0]["n"]] for name, sql in checks_sql.items()]
            period_issues = fetch(cur, """
                SELECT cr.id claim_id,upd.epf_no,cr.request_id,
                       icd.insurance_staff_category_period stored_period,
                       idl.insurance_staff_category_period expected_period
                FROM claims_request cr
                JOIN application_user au ON au.id=cr.employee
                JOIN user_personal_details upd ON upd.id=au.user_personal_details
                JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details
                JOIN insurance_details_limit idl ON idl.id=cr.insurance_details_limit_id
                WHERE cr.request_status='APPROVED'
                  AND icd.insurance_staff_category_period IS NOT NULL
                  AND icd.insurance_staff_category_period<>idl.insurance_staff_category_period
                ORDER BY cr.id
            """)
            missing_quarters = fetch(cur, """
                SELECT cr.id claim_id,upd.epf_no,cr.request_id,
                       idl.insurance_policy,idl.treatment,icd.treatment_category
                FROM claims_request cr
                JOIN application_user au ON au.id=cr.employee
                JOIN user_personal_details upd ON upd.id=au.user_personal_details
                JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details
                JOIN insurance_details_limit idl ON idl.id=cr.insurance_details_limit_id
                WHERE cr.request_status='APPROVED' AND idl.is_quarter=1
                  AND cr.insurance_quarter_id IS NULL
                ORDER BY cr.id
            """)
        connection.rollback()

    for row in migration_claims:
        row["batch"] = marker_name(row["remark"])
        row["source"] = source_name(row["remark"])
    batch_groups = defaultdict(list)
    for row in migration_claims:
        batch_groups[row["batch"]].append(row)
    batch_summary = []
    for batch, rows in sorted(batch_groups.items()):
        new_count = BATCH_NEW.get(batch, 0)
        batch_summary.append({
            "batch": batch.strip("[]").replace("MIGRATION:", ""),
            "tagged": len(rows),
            "new": new_count,
            "reconciled": len(rows) - new_count,
            "employees": len({r["epf_no"] for r in rows}),
            "treatments": ", ".join(sorted({r["treatment"] for r in rows})),
            "categories": ", ".join(sorted({r["treatment_category"] for r in rows})),
            "approved": sum((r["approved_amount"] or Decimal("0")) for r in rows),
        })

    return locals()


def set_cell_shading(cell, fill: str) -> None:
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), fill)
    tc_pr.append(shd)


def repeat_table_header(row) -> None:
    tr_pr = row._tr.get_or_add_trPr()
    tbl_header = OxmlElement("w:tblHeader")
    tbl_header.set(qn("w:val"), "true")
    tr_pr.append(tbl_header)


def add_page_field(paragraph) -> None:
    run = paragraph.add_run()
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = "PAGE"
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    run._r.extend([begin, instr, end])


def docx_table(doc, headers, rows, font_size=7):
    table = doc.add_table(rows=1, cols=len(headers))
    table.style = "Table Grid"
    table.autofit = True
    for i, header in enumerate(headers):
        cell = table.rows[0].cells[i]
        cell.text = str(header)
        set_cell_shading(cell, NAVY)
        for run in cell.paragraphs[0].runs:
            run.font.color.rgb = RGBColor(255, 255, 255)
            run.font.bold = True
            run.font.size = Pt(font_size)
    repeat_table_header(table.rows[0])
    for row_index, row in enumerate(rows):
        cells = table.add_row().cells
        if row_index % 2:
            for cell in cells:
                set_cell_shading(cell, LIGHT_GRAY)
        for i, value in enumerate(row):
            cells[i].text = text(value)
            for p in cells[i].paragraphs:
                for run in p.runs:
                    run.font.size = Pt(font_size)
    doc.add_paragraph()
    return table


def build_docx(data: dict) -> None:
    doc = Document()
    sec = doc.sections[0]
    sec.orientation = WD_ORIENT.LANDSCAPE
    sec.page_width, sec.page_height = Inches(11.69), Inches(8.27)
    sec.top_margin = sec.bottom_margin = Inches(0.45)
    sec.left_margin = sec.right_margin = Inches(0.45)

    styles = doc.styles
    styles["Normal"].font.name = "Aptos"
    styles["Normal"].font.size = Pt(9)
    for name, size, color in (("Title", 26, NAVY), ("Heading 1", 18, NAVY), ("Heading 2", 13, BLUE)):
        styles[name].font.name = "Aptos Display"
        styles[name].font.size = Pt(size)
        styles[name].font.color.rgb = RGBColor.from_string(color)

    footer = sec.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    footer.add_run("Confidential – Client Review | WeCare Live Insurance & Claims | Page ")
    add_page_field(footer)

    title = doc.add_paragraph(style="Title")
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title.add_run("WeCare Live Insurance Policies, Limits\nand Claims Migration Report")
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run("Detailed client reconciliation and database configuration catalogue").bold = True
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run(f"Database: {data['snapshot']['database_name']}\nSnapshot: {data['snapshot']['snapshot_time']:%Y-%m-%d %H:%M:%S}\nClassification: Confidential – Client Review")
    doc.add_paragraph("This report intentionally excludes passwords, connection credentials, NICs, email addresses, phone numbers, object-storage secrets and unrelated application tables.")
    doc.add_page_break()

    doc.add_heading("1. Executive Summary", level=1)
    doc.add_paragraph(
        f"The live database contains {sum(int(r['claim_count']) for r in data['claim_summary'])} claims. "
        f"A total of {len(data['migration_claims'])} claims are explicitly tagged across "
        f"{len(data['batch_summary'])} controlled migration/reconciliation batches. "
        "Migration markers identify both newly inserted claims and existing live claims reused or corrected to avoid duplication."
    )
    doc.add_paragraph(
        "Approved usage is calculated from approved_amount only. UNDER_REVIEW, REJECTED, pending source rows and zero-approved source rows do not consume the approved policy balance unless an explicit client-approved exception is documented."
    )
    docx_table(doc, ["Metric", "Value"], [
        ["Staff categories configured", len(data["staff"])],
        ["Insurance policies configured", len(data["policies"])],
        ["Policy/staff periods", len(data["periods"])],
        ["Treatment limit rows", len(data["limits"])],
        ["Quarter/category limit rows", len(data["quarters"])],
        ["Migration-tagged claims", len(data["migration_claims"])],
        ["Explicitly excluded/not-added source cases", len(EXCLUSIONS)],
    ], 9)

    doc.add_heading("2. How to Read the Limits", level=1)
    for item in [
        "Policy period: the date range attached to a staff category and policy configuration.",
        "Global limit: the maximum approved usage for the treatment/limit row before other business rules.",
        "Quarter/category limit: the staged or category-specific ceiling for OTHER, DENTAL or SPECTACLE.",
        "Permanent-date selection: the application selects the applicable staged limit using the employee permanent date; employees permanent before the period start use the first eligible window.",
        "Available balance: global limit minus approved_amount totals. Requested amount does not reduce the balance.",
        "Migration marker: an auditable remark showing that a claim was inserted, matched, split, reclassified or status-reconciled during a controlled batch.",
    ]:
        doc.add_paragraph(item, style="List Bullet")

    doc.add_heading("3. Master Configuration", level=1)
    doc.add_heading("3.1 Staff Categories", level=2)
    docx_table(doc, ["Code", "Description", "Status"], [[r["code"], r["description"], r["status"]] for r in data["staff"]], 8)
    doc.add_heading("3.2 Insurance Policies", level=2)
    docx_table(doc, ["Code", "Description", "Status"], [[r["code"], r["description"], r["status"]] for r in data["policies"]], 8)
    doc.add_heading("3.3 Active Employee Assignment Summary", level=2)
    docx_table(doc, ["Company", "Staff", "Policy", "Facility", "Active employees"], [[r["company_type"], r["staff_category"], r["insurance_policy"], r["facility"], r["active_employees"]] for r in data["employee_distribution"]], 7)

    doc.add_heading("4. Policy Years and Treatment Limits", level=1)
    doc.add_heading("4.1 Policy/Staff Periods", level=2)
    docx_table(doc, ["Period ID", "Staff", "From", "To", "Status", "Policies", "Limit rows"], [[r["period_id"], r["staff_category"], r["from_date"], r["to_date"], r["status"], r["policies"], r["configured_limits"]] for r in data["periods"]], 7)
    doc.add_heading("4.2 Full Treatment Limit Catalogue", level=2)
    docx_table(doc, ["Period", "Staff", "Policy dates", "Policy", "Limit ID", "Treatment", "Global limit", "Quarter-based", "Status"], [[r["period_id"], r["staff_category"], f"{text(r['from_date'])} to {text(r['to_date'])}", r["insurance_policy"], r["limit_id"], r["treatment"], money(r["global_limit"]), "Yes" if r["is_quarter"] else "No", r["status"]] for r in data["limits"]], 6.5)

    doc.add_heading("5. Full Quarter and Treatment-Category Limit Catalogue", level=1)
    doc.add_paragraph("Every database quarter/category row is listed below. DENTAL and SPECTACLE may cover the full policy period even though the table is named insurance_quarter.")
    docx_table(doc, ["Period", "Staff", "Policy dates", "Policy", "Treatment", "Limit ID", "Quarter ID", "Category", "Category/quarter limit", "From", "To"], [[r["period_id"], r["staff_category"], f"{text(r['policy_from'])}–{text(r['policy_to'])}", r["insurance_policy"], r["treatment"], r["limit_id"], r["quarter_id"], r["treatment_category_code"], money(r["quarter_limit"]), r["from_date"], r["to_date"]] for r in data["quarters"]], 5.8)

    doc.add_heading("6. Current Claims in the Live Database", level=1)
    doc.add_heading("6.1 Status, Treatment and Category Summary", level=2)
    docx_table(doc, ["Status", "Treatment", "Category", "Claims", "Requested", "Approved"], [[r["request_status"], r["treatment"], r["treatment_category"], r["claim_count"], money(r["requested"]), money(r["approved"])] for r in data["claim_summary"]], 7)
    doc.add_heading("6.2 Approved Employee Usage and Global Balance", level=2)
    doc.add_paragraph("Category-approved values are shown separately. Global remaining is calculated across all categories under the same treatment limit and therefore repeats when an employee has multiple categories.")
    docx_table(doc, ["Period", "Policy dates", "EPF", "Employee", "Staff", "Policy", "Treatment", "Category", "Claims", "Category approved", "Employee treatment total", "Global limit", "Global remaining"], [[r["period_id"], f"{text(r['from_date'])}–{text(r['to_date'])}", r["epf_no"], r["employee_name"], r["staff_category"], r["insurance_policy"], r["treatment"], r["treatment_category"], r["approved_claims"], money(r["category_approved"]), money(r["total_approved"]), money(r["global_limit"]), money(r["global_remaining"])] for r in data["usage"]], 5.5)

    doc.add_heading("7. Migration Reconciliation", level=1)
    doc.add_heading("7.1 Batch Summary", level=2)
    docx_table(doc, ["Batch", "Tagged", "New rows", "Existing reused/corrected", "Employees", "Treatment", "Categories", "Approved total represented"], [[r["batch"], r["tagged"], r["new"], r["reconciled"], r["employees"], r["treatments"], r["categories"], money(r["approved"])] for r in data["batch_summary"]], 6.5)
    doc.add_heading("7.2 Explicit Business Exceptions", level=2)
    docx_table(doc, ["Staff", "EPF", "Source/claim", "Original state", "Final state", "Reason/audit treatment"], EXCEPTIONS, 7)
    doc.add_heading("7.3 Not Added / Excluded Cases", level=2)
    doc.add_paragraph("These records were intentionally not inserted as approved claims. This is not a processing failure; each exclusion follows the approved migration rules.")
    docx_table(doc, ["Source", "Staff", "EPF", "Legacy/source ID", "Category", "Source status", "Approved", "Reason"], EXCLUSIONS, 6.5)
    doc.add_heading("7.4 Why Many Workbook Claims Were Not Added as New Rows", level=2)
    doc.add_paragraph("Where the same employee, policy, treatment and approved amount already existed in live data, the existing row was reused and marked. In a few cases an aggregate live row represented several source lines; it was retained or split only when treatment-category accuracy required it. This avoided double deduction of employee limits.")
    doc.add_heading("7.5 Permanent-Date Quarter Corrections", level=2)
    docx_table(doc, ["Claim", "EPF", "Request ID", "Treatment", "Category", "Final quarter", "Final period"], [[r["claim_id"], r["epf_no"], r["request_id"], r["treatment"], r["treatment_category"], r["insurance_quarter_id"], r["period_id"]] for r in data["quarter_corrections"]], 6.5)

    doc.add_heading("8. Detailed Migration Claim Register", level=1)
    doc.add_paragraph("This appendix lists every currently tagged migration/reconciliation claim. A tagged row may be newly inserted or an existing row reused/corrected to prevent duplication.")
    docx_table(doc, ["Batch", "Claim", "Request ID", "EPF", "Employee", "Staff", "Policy", "Policy dates", "Treatment", "Category", "Status", "Requested", "Approved", "Quarter", "Source/audit note"], [[r["batch"].strip("[]").replace("MIGRATION:", ""), r["claim_id"], r["request_id"], r["epf_no"], r["employee_name"], r["staff_category"], r["insurance_policy"], f"{text(r['policy_from'])}–{text(r['policy_to'])}", r["treatment"], r["treatment_category"], r["request_status"], money(r["request_amount"]), money(r["approved_amount"]), r["insurance_quarter_id"], r["source"]] for r in data["migration_claims"]], 5.0)

    doc.add_heading("9. Data Quality and Follow-up", level=1)
    docx_table(doc, ["Validation check", "Observed count"], data["checks"], 8)
    doc.add_paragraph("A non-zero count is an observation requiring controlled review; it does not automatically mean the approved amount or current employee balance is wrong.")
    doc.add_heading("9.1 Period-Reference Differences", level=2)
    docx_table(doc, ["Claim", "EPF", "Request ID", "Stored period", "Limit period"], [[r["claim_id"], r["epf_no"], r["request_id"], r["stored_period"], r["expected_period"]] for r in data["period_issues"]], 7)
    doc.add_heading("9.2 Approved Quarter-Enabled Claims Without Quarter Reference", level=2)
    docx_table(doc, ["Claim", "EPF", "Request ID", "Policy", "Treatment", "Category"], [[r["claim_id"], r["epf_no"], r["request_id"], r["insurance_policy"], r["treatment"], r["treatment_category"]] for r in data["missing_quarters"]], 6.5)
    doc.add_heading("9.3 Recommended Follow-up", level=2)
    for item in [
        "Review and correct the 15 legacy period-reference differences in a separate transactional change after business confirmation.",
        "Populate the 25 missing quarter references using the same permanent-date rule, after validating each affected historical policy.",
        "Retain migration markers and this report with the production change record.",
        "Re-run duplicate, limit-overrun, period and quarter validation after any future migration.",
    ]:
        doc.add_paragraph(item, style="List Number")

    doc.add_heading("10. Scope and Assurance", level=1)
    doc.add_paragraph("Data sources: live sgcs_care insurance/claims tables, Outdoor.xlsx, indor.xlsx, approved balance sheets and recorded client decisions. Database access for report generation was SELECT-only. This report is a point-in-time snapshot and should be regenerated after material policy or claim changes.")
    doc.save(DOCX_PATH)


def pdf_paragraph(value, style):
    return Paragraph(escape(text(value)).replace("\n", "<br/>"), style)


def pdf_table(story, headers, rows, styles, font_size=6, weights=None):
    cell_style = ParagraphStyle("cell", parent=styles["BodyText"], fontName="Helvetica", fontSize=font_size, leading=font_size + 1)
    head_style = ParagraphStyle("head", parent=cell_style, textColor=colors.white, fontName="Helvetica-Bold")
    matrix = [[pdf_paragraph(v, head_style) for v in headers]]
    matrix += [[pdf_paragraph(v, cell_style) for v in row] for row in rows]
    usable = landscape(A4)[0] - 24 * mm
    weights = weights or [1] * len(headers)
    total = sum(weights)
    widths = [usable * w / total for w in weights]
    table = Table(matrix, colWidths=widths, repeatRows=1, hAlign="LEFT")
    commands = [
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#17365D")),
        ("GRID", (0, 0), (-1, -1), 0.25, colors.HexColor("#A6A6A6")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 2),
        ("RIGHTPADDING", (0, 0), (-1, -1), 2),
        ("TOPPADDING", (0, 0), (-1, -1), 2),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 2),
    ]
    for index in range(2, len(matrix), 2):
        commands.append(("BACKGROUND", (0, index), (-1, index), colors.HexColor("#F2F2F2")))
    table.setStyle(TableStyle(commands))
    story.extend([table, Spacer(1, 4 * mm)])


def build_pdf(data: dict) -> None:
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle("ReportTitle", parent=styles["Title"], fontName="Helvetica-Bold", fontSize=24, leading=29, textColor=colors.HexColor("#17365D"), alignment=TA_CENTER, spaceAfter=8 * mm))
    styles.add(ParagraphStyle("H1x", parent=styles["Heading1"], fontName="Helvetica-Bold", fontSize=16, leading=19, textColor=colors.HexColor("#17365D"), spaceBefore=3 * mm, spaceAfter=3 * mm))
    styles.add(ParagraphStyle("H2x", parent=styles["Heading2"], fontName="Helvetica-Bold", fontSize=11, leading=13, textColor=colors.HexColor("#2F75B5"), spaceBefore=2 * mm, spaceAfter=2 * mm))
    styles.add(ParagraphStyle("BodyX", parent=styles["BodyText"], fontName="Helvetica", fontSize=8.5, leading=11, spaceAfter=2 * mm))

    def footer(canvas, doc):
        canvas.saveState()
        canvas.setFont("Helvetica", 7)
        canvas.setFillColor(colors.HexColor("#666666"))
        canvas.drawCentredString(landscape(A4)[0] / 2, 7 * mm, f"Confidential – Client Review | WeCare Live Insurance & Claims | Page {doc.page}")
        canvas.restoreState()

    report = SimpleDocTemplate(str(PDF_PATH), pagesize=landscape(A4), rightMargin=12 * mm, leftMargin=12 * mm, topMargin=12 * mm, bottomMargin=14 * mm, title="WeCare Live Insurance Policies, Limits and Claims Migration Report", author="WeCare")
    story = [Spacer(1, 25 * mm), Paragraph("WeCare Live Insurance Policies, Limits<br/>and Claims Migration Report", styles["ReportTitle"]), Paragraph("Detailed client reconciliation and database configuration catalogue", ParagraphStyle("sub", parent=styles["BodyX"], fontSize=12, leading=15, alignment=TA_CENTER)), Spacer(1, 8 * mm), Paragraph(f"Database: {escape(data['snapshot']['database_name'])}<br/>Snapshot: {data['snapshot']['snapshot_time']:%Y-%m-%d %H:%M:%S}<br/>Classification: Confidential – Client Review", ParagraphStyle("cover", parent=styles["BodyX"], fontSize=10, leading=14, alignment=TA_CENTER)), Spacer(1, 12 * mm), Paragraph("Sensitive credentials and unrelated application data are intentionally excluded.", ParagraphStyle("note", parent=styles["BodyX"], alignment=TA_CENTER, textColor=colors.HexColor("#666666"))), PageBreak()]

    def h1(value): story.append(Paragraph(value, styles["H1x"]))
    def h2(value): story.append(Paragraph(value, styles["H2x"]))
    def body(value): story.append(Paragraph(escape(value), styles["BodyX"]))

    h1("1. Executive Summary")
    body(f"The live database contains {sum(int(r['claim_count']) for r in data['claim_summary'])} claims. {len(data['migration_claims'])} claims are explicitly tagged across {len(data['batch_summary'])} controlled migration/reconciliation batches. Tagged claims include both new inserts and existing rows reused or corrected to prevent duplicate limit deductions.")
    pdf_table(story, ["Metric", "Value"], [["Staff categories", len(data["staff"])], ["Insurance policies", len(data["policies"])], ["Policy/staff periods", len(data["periods"])], ["Treatment limits", len(data["limits"])], ["Quarter/category limits", len(data["quarters"])], ["Migration-tagged claims", len(data["migration_claims"])], ["Excluded/not-added cases", len(EXCLUSIONS)]], styles, 8, [3, 1])
    h1("2. How to Read the Limits")
    body("Policy period is the configured year/window for a staff category. Global limit is the treatment ceiling. Quarter/category limits stage OTHER eligibility or define DENTAL/SPECTACLE sublimits. Approved usage is calculated from approved_amount only. The application selects staged eligibility using employee permanent date; employees permanent before the policy use the first eligible window.")
    h1("3. Master Configuration")
    h2("3.1 Staff Categories")
    pdf_table(story, ["Code", "Description", "Status"], [[r["code"], r["description"], r["status"]] for r in data["staff"]], styles, 7, [1, 3, 1])
    h2("3.2 Insurance Policies")
    pdf_table(story, ["Code", "Description", "Status"], [[r["code"], r["description"], r["status"]] for r in data["policies"]], styles, 7, [1, 3, 1])
    h2("3.3 Active Employee Assignment Summary")
    pdf_table(story, ["Company", "Staff", "Policy", "Facility", "Active employees"], [[r["company_type"], r["staff_category"], r["insurance_policy"], r["facility"], r["active_employees"]] for r in data["employee_distribution"]], styles, 7, [1, 1, 1.5, 1, 1])

    h1("4. Policy Years and Treatment Limits")
    h2("4.1 Policy/Staff Periods")
    pdf_table(story, ["Period", "Staff", "From", "To", "Status", "Policies", "Limit rows"], [[r["period_id"], r["staff_category"], r["from_date"], r["to_date"], r["status"], r["policies"], r["configured_limits"]] for r in data["periods"]], styles, 6.5, [0.7, 0.8, 1, 1, 0.8, 2, 0.8])
    h2("4.2 Full Treatment Limit Catalogue")
    pdf_table(story, ["Period", "Staff", "Policy dates", "Policy", "Limit", "Treatment", "Global limit", "Quarter?", "Status"], [[r["period_id"], r["staff_category"], f"{text(r['from_date'])} to {text(r['to_date'])}", r["insurance_policy"], r["limit_id"], r["treatment"], money(r["global_limit"]), "Yes" if r["is_quarter"] else "No", r["status"]] for r in data["limits"]], styles, 5.8, [0.5, 0.7, 1.5, 1.1, 0.5, 0.8, 1, 0.7, 0.7])

    h1("5. Full Quarter and Treatment-Category Limit Catalogue")
    body("Every insurance_quarter row is listed. DENTAL and SPECTACLE can cover the full policy period despite the table name.")
    pdf_table(story, ["Period", "Staff", "Policy dates", "Policy", "Treatment", "Limit", "Quarter", "Category", "Limit", "From", "To"], [[r["period_id"], r["staff_category"], f"{text(r['policy_from'])}–{text(r['policy_to'])}", r["insurance_policy"], r["treatment"], r["limit_id"], r["quarter_id"], r["treatment_category_code"], money(r["quarter_limit"]), r["from_date"], r["to_date"]] for r in data["quarters"]], styles, 5.2, [0.5, 0.6, 1.3, 1, 0.7, 0.5, 0.5, 0.8, 0.9, 0.9, 0.9])

    h1("6. Current Claims in the Live Database")
    h2("6.1 Status, Treatment and Category Summary")
    pdf_table(story, ["Status", "Treatment", "Category", "Claims", "Requested", "Approved"], [[r["request_status"], r["treatment"], r["treatment_category"], r["claim_count"], money(r["requested"]), money(r["approved"])] for r in data["claim_summary"]], styles, 6.5, [1, 1, 1, 0.6, 1.2, 1.2])
    h2("6.2 Approved Employee Usage and Global Balance")
    body("Category totals are separate. Global remaining repeats across category rows for the same employee/treatment because it is calculated across all categories under that global limit.")
    pdf_table(story, ["Period", "Dates", "EPF", "Employee", "Staff", "Policy", "Treatment", "Category", "Claims", "Category approved", "Treatment total", "Global limit", "Remaining"], [[r["period_id"], f"{text(r['from_date'])}–{text(r['to_date'])}", r["epf_no"], r["employee_name"], r["staff_category"], r["insurance_policy"], r["treatment"], r["treatment_category"], r["approved_claims"], money(r["category_approved"]), money(r["total_approved"]), money(r["global_limit"]), money(r["global_remaining"])] for r in data["usage"]], styles, 4.8, [0.4, 1.1, 0.5, 1.3, 0.5, 0.8, 0.6, 0.7, 0.4, 0.9, 0.9, 0.9, 0.9])

    h1("7. Migration Reconciliation")
    h2("7.1 Batch Summary")
    pdf_table(story, ["Batch", "Tagged", "New", "Existing reused/corrected", "Employees", "Treatment", "Categories", "Approved represented"], [[r["batch"], r["tagged"], r["new"], r["reconciled"], r["employees"], r["treatments"], r["categories"], money(r["approved"])] for r in data["batch_summary"]], styles, 5.8, [2.2, 0.5, 0.5, 0.9, 0.6, 0.8, 1.2, 1])
    h2("7.2 Explicit Business Exceptions")
    pdf_table(story, ["Staff", "EPF", "Source/claim", "Original", "Final", "Reason"], EXCEPTIONS, styles, 6, [0.6, 0.5, 1.4, 1.5, 1.4, 3])
    h2("7.3 Not Added / Excluded Cases")
    body("These are intentional exclusions, not processing failures.")
    pdf_table(story, ["Source", "Staff", "EPF", "Legacy/source", "Category", "Source status", "Approved", "Reason"], EXCLUSIONS, styles, 5.7, [1, 0.6, 0.5, 1.5, 0.7, 0.8, 0.7, 3])
    h2("7.4 Duplicate Avoidance")
    body("Claims already present with the same employee, policy, treatment and approved value were reused and marked. Aggregate rows were retained or split only where category accuracy required it. This prevented double deduction of limits.")
    h2("7.5 Permanent-Date Quarter Corrections")
    pdf_table(story, ["Claim", "EPF", "Request ID", "Treatment", "Category", "Final quarter", "Final period"], [[r["claim_id"], r["epf_no"], r["request_id"], r["treatment"], r["treatment_category"], r["insurance_quarter_id"], r["period_id"]] for r in data["quarter_corrections"]], styles, 6, [0.5, 0.5, 2, 0.8, 0.8, 0.7, 0.7])

    h1("8. Detailed Migration Claim Register")
    body("Every currently tagged migration/reconciliation claim is listed. Tagged does not always mean newly inserted; it can mean an existing row was safely matched or corrected.")
    pdf_table(story, ["Batch", "Claim", "Request ID", "EPF", "Employee", "Staff", "Policy", "Dates", "Treatment", "Category", "Status", "Requested", "Approved", "Quarter", "Source/audit"], [[r["batch"].strip("[]").replace("MIGRATION:", ""), r["claim_id"], r["request_id"], r["epf_no"], r["employee_name"], r["staff_category"], r["insurance_policy"], f"{text(r['policy_from'])}–{text(r['policy_to'])}", r["treatment"], r["treatment_category"], r["request_status"], money(r["request_amount"]), money(r["approved_amount"]), r["insurance_quarter_id"], r["source"]] for r in data["migration_claims"]], styles, 4.2, [1.5, 0.4, 1.4, 0.4, 1, 0.4, 0.7, 1, 0.5, 0.6, 0.6, 0.7, 0.7, 0.4, 1.6])

    h1("9. Data Quality and Follow-up")
    pdf_table(story, ["Validation check", "Observed count"], data["checks"], styles, 7, [4, 1])
    body("A non-zero observation requires controlled review but does not automatically mean an approved amount or current balance is wrong.")
    h2("9.1 Period-Reference Differences")
    pdf_table(story, ["Claim", "EPF", "Request ID", "Stored period", "Limit period"], [[r["claim_id"], r["epf_no"], r["request_id"], r["stored_period"], r["expected_period"]] for r in data["period_issues"]], styles, 6.5, [0.5, 0.5, 2.5, 0.8, 0.8])
    h2("9.2 Quarter-Enabled Approved Claims Without Quarter Reference")
    pdf_table(story, ["Claim", "EPF", "Request ID", "Policy", "Treatment", "Category"], [[r["claim_id"], r["epf_no"], r["request_id"], r["insurance_policy"], r["treatment"], r["treatment_category"]] for r in data["missing_quarters"]], styles, 6, [0.5, 0.5, 2.4, 1, 0.8, 0.8])
    h2("9.3 Recommended Follow-up")
    body("1. Review the 15 legacy period-reference differences in a separate transaction. 2. Populate the 25 missing quarter references after validating historical policy eligibility. 3. Preserve migration markers and this report. 4. Re-run all validation checks after future migrations.")
    h1("10. Scope and Assurance")
    body("Sources: live sgcs_care insurance/claims tables, Outdoor.xlsx, indor.xlsx, approved balance sheets and recorded client decisions. Report generation used SELECT-only database access. This is a point-in-time snapshot and should be regenerated after material policy or claim changes.")
    report.build(story, onFirstPage=footer, onLaterPages=footer)


def main() -> int:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    data = load_data()
    build_docx(data)
    build_pdf(data)
    print(f"DOCX={DOCX_PATH}")
    print(f"PDF={PDF_PATH}")
    print(f"MIGRATION_CLAIMS={len(data['migration_claims'])}")
    print(f"LIMITS={len(data['limits'])} QUARTERS={len(data['quarters'])}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
