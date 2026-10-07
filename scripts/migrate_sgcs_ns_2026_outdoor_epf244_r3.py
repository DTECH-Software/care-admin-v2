"""Add the client-confirmed EPF 244 2026 OUTDOOR balance claim.

The supplied balance row establishes Rs. 950 approved and Rs. 8,050
remaining. It also says "Transfer to executive staff", but the live profile is
currently NS/P-NORMAL and has no recorded transfer date. The claim therefore
uses the employee's current live Normal Staff policy, while preserving that
source note in the audit marker.

Dry-run is the default. Pass --apply only after all live preconditions pass.
"""

from __future__ import annotations

import argparse
from contextlib import closing
from datetime import date, datetime
from decimal import Decimal

from wecare_spectacle import _connect


TAG = "[MIGRATION:SGCS-NS-2026-OUTDOOR-R3]"
SOURCE = (
    "BALANCE-SHEET-EPF244-950;TRANSFER-TO-EXECUTIVE-STAFF;"
    "SOURCE-DATE-NOT-PROVIDED;POLICY-START-USED"
)
EPF = "244"
REQUEST_ID = "TC/HC/SGCS/NS/2026/0020"
LIMIT_ID = 42
PERIOD_ID = 12
QUARTER_ID = 144
POLICY_START = date(2026, 1, 21)
POLICY_END = date(2027, 1, 20)
CREATED = datetime(2026, 1, 21, 12, 1)
AMOUNT = Decimal("950.00")
GLOBAL_LIMIT = Decimal("9000.00")
EXPECTED_BALANCE = Decimal("8050.00")


def one(cur, sql: str, params=()):
    cur.execute(sql, params)
    row = cur.fetchone()
    if row is None:
        raise RuntimeError("Expected one row but query returned none")
    return row


def approved_total(cur, user_id: int) -> Decimal:
    return one(
        cur,
        """
        SELECT COALESCE(SUM(approved_amount),0) approved
        FROM claims_request
        WHERE employee=%s AND insurance_details_limit_id=%s
          AND request_status='APPROVED'
        """,
        (user_id, LIMIT_ID),
    )["approved"]


def validate_configuration(cur) -> int:
    database = one(cur, "SELECT DATABASE() db")["db"]
    if database != "sgcs_care":
        raise RuntimeError(f"Refusing database {database!r}; expected live 'sgcs_care'")

    cur.execute(
        """
        SELECT au.id application_user_id,upd.user_status,ucd.company_type,
               ucd.staff_category,ucd.insurance_policy,
               ucd.previous_staff_category,DATE(ucd.transfer_date) transfer_date
        FROM user_personal_details upd
        JOIN user_company_details ucd ON ucd.id=upd.user_company_details
        JOIN application_user au ON au.user_personal_details=upd.id
        WHERE upd.epf_no=%s
        FOR UPDATE
        """,
        (EPF,),
    )
    users = cur.fetchall()
    if len(users) != 1:
        raise RuntimeError(f"Expected one EPF {EPF} application user; found {len(users)}")
    user = users[0]
    actual = (
        user["user_status"], user["company_type"],
        user["staff_category"], user["insurance_policy"],
        user["previous_staff_category"], user["transfer_date"],
    )
    expected = ("ACTIVE", "SGCS", "NS", "P-NORMAL", None, None)
    if actual != expected:
        raise RuntimeError(f"EPF {EPF} profile changed after confirmation: {actual}")

    limit_row = one(
        cur,
        """
        SELECT insurance_staff_category_period,insurance_policy,treatment,
               global_limit,is_quarter,status
        FROM insurance_details_limit WHERE id=%s
        """,
        (LIMIT_ID,),
    )
    actual_limit = (
        limit_row["insurance_staff_category_period"],
        limit_row["insurance_policy"], limit_row["treatment"],
        limit_row["global_limit"], limit_row["is_quarter"], limit_row["status"],
    )
    expected_limit = (PERIOD_ID, "P-NORMAL", "OUTDOOR", GLOBAL_LIMIT, 1, "ACTIVE")
    if actual_limit != expected_limit:
        raise RuntimeError(f"OUTDOOR limit configuration mismatch: {actual_limit}")

    period = one(
        cur,
        "SELECT from_date,to_date,status FROM insurance_staff_category_period WHERE id=%s",
        (PERIOD_ID,),
    )
    if (period["from_date"], period["to_date"], period["status"]) != (
        POLICY_START, POLICY_END, "ACTIVE"
    ):
        raise RuntimeError(f"Policy period configuration mismatch: {period}")

    quarter = one(
        cur,
        """
        SELECT insurance_details_id,treatment_category_code,quarter_limit,
               from_date,to_date
        FROM insurance_quarter WHERE id=%s
        """,
        (QUARTER_ID,),
    )
    actual_quarter = (
        quarter["insurance_details_id"], quarter["treatment_category_code"],
        quarter["quarter_limit"], quarter["from_date"], quarter["to_date"],
    )
    expected_quarter = (
        LIMIT_ID, "OTHER", GLOBAL_LIMIT, date(2026, 1, 21), date(2026, 4, 20)
    )
    if actual_quarter != expected_quarter:
        raise RuntimeError(f"Quarter configuration mismatch: {actual_quarter}")

    return int(user["application_user_id"])


def insert_claim(cur, user_id: int) -> int:
    cur.execute(
        "SELECT id,employee,approved_amount,remark FROM claims_request WHERE request_id=%s FOR UPDATE",
        (REQUEST_ID,),
    )
    existing = cur.fetchone()
    if existing:
        if (
            existing["employee"] != user_id
            or existing["approved_amount"] != AMOUNT
            or TAG not in (existing["remark"] or "")
        ):
            raise RuntimeError(f"Request ID collision: {existing}")
        return int(existing["id"])

    cur.execute(
        "SELECT id FROM claims_request WHERE remark LIKE %s FOR UPDATE",
        (f"%{TAG}%",),
    )
    marker_rows = cur.fetchall()
    if marker_rows:
        raise RuntimeError(f"Unexpected existing R3 marker: {marker_rows}")

    cur.execute(
        """
        INSERT INTO insurance_claims_details
          (created_date,last_modified_date,disease,from_treatment_date,
           to_treatment_date,treatment,treatment_category,
           insurance_staff_category_period)
        VALUES (%s,%s,'The Care Data Migration',NULL,%s,'OUTDOOR','OTHER',%s)
        """,
        (CREATED, CREATED, POLICY_START, PERIOD_ID),
    )
    details_id = int(cur.lastrowid)
    remark = f"Approved legacy balance reconciliation {TAG} SOURCE:{SOURCE}"
    if len(remark) > 255:
        raise RuntimeError("Audit remark exceeds 255 characters")
    cur.execute(
        """
        INSERT INTO claims_request
          (created_date,last_modified_date,remark,request_amount,request_id,
           request_status,dependent,employee,insurance_claims_details,
           approval_work_flow_id,insurance_details_limit_id,
           insurance_quarter_id,approval_level,approved_amount,
           assisted_mobile_no)
        VALUES (%s,%s,%s,%s,%s,'APPROVED',NULL,%s,%s,NULL,%s,%s,
                'LEVEL02',%s,NULL)
        """,
        (
            CREATED, CREATED, remark, AMOUNT, REQUEST_ID, user_id,
            details_id, LIMIT_ID, QUARTER_ID, AMOUNT,
        ),
    )
    return int(cur.lastrowid)


def validate_final(cur, user_id: int, claim_id: int) -> None:
    total = approved_total(cur, user_id)
    balance = GLOBAL_LIMIT - total
    if total != AMOUNT or balance != EXPECTED_BALANCE:
        raise RuntimeError(f"Final limit mismatch: approved={total}, balance={balance}")

    row = one(
        cur,
        """
        SELECT cr.request_status,cr.request_amount,cr.approved_amount,
               cr.insurance_details_limit_id,cr.insurance_quarter_id,
               icd.treatment,icd.treatment_category,
               icd.insurance_staff_category_period,cr.remark
        FROM claims_request cr
        JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details
        WHERE cr.id=%s
        """,
        (claim_id,),
    )
    actual = (
        row["request_status"], row["request_amount"], row["approved_amount"],
        row["insurance_details_limit_id"], row["insurance_quarter_id"],
        row["treatment"], row["treatment_category"],
        row["insurance_staff_category_period"], TAG in (row["remark"] or ""),
    )
    expected = (
        "APPROVED", AMOUNT, AMOUNT, LIMIT_ID, QUARTER_ID,
        "OUTDOOR", "OTHER", PERIOD_ID, True,
    )
    if actual != expected:
        raise RuntimeError(f"Inserted claim validation failed: {actual}")

    cur.execute(
        """
        SELECT COUNT(*) rows_found
        FROM claims_request cr
        JOIN application_user au ON au.id=cr.employee
        JOIN user_personal_details upd ON upd.id=au.user_personal_details
        WHERE upd.epf_no=%s AND cr.id<>%s
          AND cr.insurance_details_limit_id=%s
        """,
        (EPF, claim_id, LIMIT_ID),
    )
    other_2026_outdoor = int(cur.fetchone()["rows_found"])
    if other_2026_outdoor != 0:
        raise RuntimeError(f"Unexpected additional 2026 OUTDOOR claims: {other_2026_outdoor}")

    print(f"Claim ID: {claim_id}")
    print(f"Request ID: {REQUEST_ID}")
    print(f"Approved OUTDOOR/OTHER: {total}")
    print(f"Remaining OUTDOOR global balance: {balance}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true", help="commit the live change")
    args = parser.parse_args()

    with closing(_connect()) as connection:
        connection.autocommit = False
        with closing(connection.cursor(dictionary=True)) as cur:
            try:
                user_id = validate_configuration(cur)
                before = approved_total(cur, user_id)
                already_applied = one(
                    cur,
                    "SELECT COUNT(*) n FROM claims_request WHERE request_id=%s AND remark LIKE %s",
                    (REQUEST_ID, f"%{TAG}%"),
                )["n"] == 1
                if not already_applied and before != Decimal("0.00"):
                    raise RuntimeError(f"Unexpected approved total before migration: {before}")

                print("Mode:", "APPLY" if args.apply else "DRY_RUN")
                print("Before approved OUTDOOR total:", before)
                claim_id = insert_claim(cur, user_id)
                validate_final(cur, user_id, claim_id)

                if args.apply:
                    connection.commit()
                    print(f"COMMITTED: marker={TAG}")
                else:
                    connection.rollback()
                    print("DRY RUN PASSED; transaction rolled back")
            except Exception:
                connection.rollback()
                raise
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
