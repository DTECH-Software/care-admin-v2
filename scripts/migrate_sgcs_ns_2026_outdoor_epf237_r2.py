"""Add the client-confirmed EPF 237 2026 OUTDOOR balance claim.

The supplied balance evidence establishes Rs. 983 approved and Rs. 8,017
remaining from the Rs. 9,000 policy limit, but does not provide a treatment
date.  The policy start date and first eligible OTHER quarter are therefore
used, and that decision is preserved in the audit marker.

Dry-run is the default. Pass --apply only after all live preconditions pass.
"""

from __future__ import annotations

import argparse
from contextlib import closing
from datetime import date, datetime
from decimal import Decimal

from wecare_spectacle import _connect


TAG = "[MIGRATION:SGCS-NS-2026-OUTDOOR-R2]"
SOURCE = "BALANCE-SHEET-EPF237-983;SOURCE-DATE-NOT-PROVIDED;POLICY-START-USED"
EPF = "237"
REQUEST_ID = "TC/HC/SGCS/NS/2026/0019"
LIMIT_ID = 42
PERIOD_ID = 12
QUARTER_ID = 144
POLICY_START = date(2026, 1, 21)
POLICY_END = date(2027, 1, 20)
CREATED = datetime(2026, 1, 21, 12, 0)
AMOUNT = Decimal("983.00")
GLOBAL_LIMIT = Decimal("9000.00")
EXPECTED_BALANCE = Decimal("8017.00")
EXPECTED_UNDER_REVIEW = {
    476: Decimal("2650.00"),
    483: Decimal("1680.00"),
    484: Decimal("1240.00"),
}


def one(cur, sql: str, params=()):
    cur.execute(sql, params)
    row = cur.fetchone()
    if row is None:
        raise RuntimeError("Expected one row but query returned none")
    return row


def approved_total(cur, user_id: int) -> Decimal:
    row = one(
        cur,
        """
        SELECT COALESCE(SUM(approved_amount), 0) approved
        FROM claims_request
        WHERE employee=%s AND insurance_details_limit_id=%s
          AND request_status='APPROVED'
        """,
        (user_id, LIMIT_ID),
    )
    return row["approved"]


def validate_configuration(cur) -> int:
    database = one(cur, "SELECT DATABASE() db")["db"]
    if database != "sgcs_care":
        raise RuntimeError(f"Refusing database {database!r}; expected live 'sgcs_care'")

    cur.execute(
        """
        SELECT au.id application_user_id,upd.user_status,ucd.company_type,
               ucd.staff_category,ucd.insurance_policy
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
    actual_user = (
        user["user_status"], user["company_type"],
        user["staff_category"], user["insurance_policy"],
    )
    if actual_user != ("ACTIVE", "SGCS", "NS", "P-NORMAL"):
        raise RuntimeError(f"EPF {EPF} profile mismatch: {actual_user}")

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


def validate_under_review_unchanged(cur, user_id: int) -> None:
    ids = tuple(EXPECTED_UNDER_REVIEW)
    marks = ",".join(["%s"] * len(ids))
    cur.execute(
        f"""
        SELECT id,request_status,request_amount,approved_amount,
               insurance_details_limit_id
        FROM claims_request
        WHERE employee=%s AND id IN ({marks})
        ORDER BY id FOR UPDATE
        """,
        (user_id, *ids),
    )
    rows = {row["id"]: row for row in cur.fetchall()}
    if set(rows) != set(ids):
        raise RuntimeError(f"Expected under-review claims are missing: {rows}")
    for claim_id, expected_amount in EXPECTED_UNDER_REVIEW.items():
        row = rows[claim_id]
        actual = (
            row["request_status"], row["request_amount"],
            row["approved_amount"], row["insurance_details_limit_id"],
        )
        expected = ("UNDER_REVIEW", expected_amount, None, LIMIT_ID)
        if actual != expected:
            raise RuntimeError(f"Under-review claim {claim_id} changed: {actual}")


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
    collisions = cur.fetchall()
    if collisions:
        raise RuntimeError(f"Unexpected existing R2 marker: {collisions}")

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
        SELECT COUNT(*) count_rows,COALESCE(SUM(request_amount),0) requested
        FROM claims_request
        WHERE employee=%s AND insurance_details_limit_id=%s
          AND request_status='UNDER_REVIEW'
        """,
        (user_id, LIMIT_ID),
    )
    pending = cur.fetchone()
    if (pending["count_rows"], pending["requested"]) != (3, Decimal("5570.00")):
        raise RuntimeError(f"Under-review claims were not preserved: {pending}")

    print(f"Claim ID: {claim_id}")
    print(f"Request ID: {REQUEST_ID}")
    print(f"Approved OUTDOOR/OTHER: {total}")
    print(f"Remaining OUTDOOR global balance: {balance}")
    print("Preserved UNDER_REVIEW: 3 claims / requested 5570.00")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true", help="commit the live change")
    args = parser.parse_args()

    with closing(_connect()) as connection:
        connection.autocommit = False
        with closing(connection.cursor(dictionary=True)) as cur:
            try:
                user_id = validate_configuration(cur)
                validate_under_review_unchanged(cur, user_id)
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
