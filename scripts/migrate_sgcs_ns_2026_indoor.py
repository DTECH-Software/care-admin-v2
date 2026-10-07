"""Reconcile the approved SGCS Normal Staff 2026 INDOOR claims.

The script defaults to a transactional dry run. Pass --apply to commit after
all live-data preconditions and final limit/category totals have passed.
"""

from __future__ import annotations

import argparse
from contextlib import closing
from datetime import date, datetime
from decimal import Decimal

from wecare_spectacle import _connect


TAG = "[MIGRATION:SGCS-NS-2026-INDOOR-R1]"
LIMIT_ID = 43
PERIOD_ID = 12
GLOBAL_LIMIT = Decimal("100000.00")

EXPECTED_FINAL = {
    "211": {"SPECTACLE": Decimal("10000.00")},
    "237": {"SPECTACLE": Decimal("10000.00")},
    "244": {"SPECTACLE": Decimal("10000.00")},
    "247": {"OTHER": Decimal("8000.00")},
}

NEW_CLAIMS = (
    {
        "epf": "211",
        "request_id": "HC/SGCS/NS/2026/0013",
        "amount": Decimal("10000.00"),
        "category": "SPECTACLE",
        "treatment_date": date(2026, 5, 4),
        "created": datetime(2026, 5, 4, 12, 0),
        "quarter_id": 151,
        "source": "BALANCE-SHEET-EPF211-EYE",
    },
    {
        "epf": "244",
        "request_id": "HC/SGCS/NS/2026/0014",
        "amount": Decimal("10000.00"),
        "category": "SPECTACLE",
        "treatment_date": date(2026, 5, 4),
        "created": datetime(2026, 5, 4, 12, 0),
        "quarter_id": 151,
        "source": "BALANCE-SHEET-EPF244-EYE",
    },
    {
        "epf": "247",
        "request_id": "HC/SGCS/NS/2026/0015",
        "amount": Decimal("8000.00"),
        "category": "OTHER",
        "treatment_date": date(2026, 5, 4),
        "created": datetime(2026, 5, 4, 12, 0),
        "quarter_id": 148,
        "source": "SGCS-INDOOR-000015;LOCAL-ANESTHESIA-COVER",
    },
)


def append_tag(remark: str | None, source: str) -> str:
    original = (remark or "").strip()
    suffix = f"{TAG} SOURCE:{source}"
    result = f"{original} {suffix}".strip()
    if len(result) > 255:
        raise RuntimeError(f"Tagged remark exceeds 255 characters: {result}")
    return result


def one(cur, sql: str, params=()):
    cur.execute(sql, params)
    row = cur.fetchone()
    if row is None:
        raise RuntimeError("Expected one row but query returned none")
    return row


def validate_configuration(cur) -> None:
    db = one(cur, "SELECT DATABASE() db")["db"]
    if db != "sgcs_care":
        raise RuntimeError(f"Refusing database {db!r}; expected live 'sgcs_care'")

    limit_row = one(
        cur,
        """
        SELECT global_limit, insurance_policy, treatment, status,
               insurance_staff_category_period
        FROM insurance_details_limit WHERE id=%s
        """,
        (LIMIT_ID,),
    )
    actual = (
        limit_row["global_limit"], limit_row["insurance_policy"],
        limit_row["treatment"], limit_row["status"],
        limit_row["insurance_staff_category_period"],
    )
    expected = (GLOBAL_LIMIT, "P-NORMAL", "INDOOR", "ACTIVE", PERIOD_ID)
    if actual != expected:
        raise RuntimeError(f"Unexpected limit ID {LIMIT_ID}: {actual}")

    cur.execute(
        """
        SELECT id, treatment_category_code, quarter_limit, from_date, to_date
        FROM insurance_quarter WHERE id IN (148,151) ORDER BY id
        """
    )
    quarters = {row["id"]: row for row in cur.fetchall()}
    if set(quarters) != {148, 151}:
        raise RuntimeError(f"Required quarter rows are missing: {quarters}")
    if quarters[148]["treatment_category_code"] != "OTHER":
        raise RuntimeError("Quarter 148 is no longer INDOOR/OTHER")
    if not (quarters[148]["from_date"] <= date(2026, 5, 4) <= quarters[148]["to_date"]):
        raise RuntimeError("Migration date is outside quarter 148")
    if (
        quarters[151]["treatment_category_code"] != "SPECTACLE"
        or quarters[151]["quarter_limit"] != Decimal("10000.00")
    ):
        raise RuntimeError("Quarter 151 is no longer the 10,000 SPECTACLE limit")


def load_users(cur) -> dict[str, int]:
    epfs = tuple(EXPECTED_FINAL)
    marks = ",".join(["%s"] * len(epfs))
    cur.execute(
        f"""
        SELECT upd.epf_no, au.id, upd.user_status, ucd.company_type,
               ucd.staff_category, ucd.insurance_policy
        FROM user_personal_details upd
        JOIN user_company_details ucd ON ucd.id=upd.user_company_details
        JOIN application_user au ON au.user_personal_details=upd.id
        WHERE upd.epf_no IN ({marks})
        ORDER BY upd.epf_no, au.id
        FOR UPDATE
        """,
        epfs,
    )
    grouped: dict[str, list[dict]] = {epf: [] for epf in epfs}
    for row in cur.fetchall():
        grouped[str(row["epf_no"])].append(row)
    invalid = {
        epf: rows for epf, rows in grouped.items()
        if len(rows) != 1
        or rows[0]["user_status"] != "ACTIVE"
        or rows[0]["company_type"] != "SGCS"
        or rows[0]["staff_category"] != "NS"
        or rows[0]["insurance_policy"] != "P-NORMAL"
    }
    if invalid:
        raise RuntimeError(f"Target employee profile mismatch: {invalid}")
    return {epf: int(rows[0]["id"]) for epf, rows in grouped.items()}


def validate_existing(cur) -> None:
    row = one(
        cur,
        """
        SELECT cr.id, upd.epf_no, cr.request_id, cr.request_status,
               cr.request_amount, cr.approved_amount,
               cr.insurance_details_limit_id, cr.insurance_quarter_id,
               cr.remark, icd.treatment, icd.treatment_category,
               icd.insurance_staff_category_period
        FROM claims_request cr
        JOIN application_user au ON au.id=cr.employee
        JOIN user_personal_details upd ON upd.id=au.user_personal_details
        JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details
        WHERE cr.id=482
        FOR UPDATE
        """,
    )
    if TAG not in (row["remark"] or ""):
        actual = (
            str(row["epf_no"]), row["request_id"], row["request_status"],
            row["request_amount"], row["approved_amount"],
            row["insurance_details_limit_id"], row["insurance_quarter_id"],
            row["treatment"], row["treatment_category"],
            row["insurance_staff_category_period"],
        )
        expected = (
            "237", "HC/SGCS/NS/2026/0006", "UNDER_REVIEW",
            Decimal("10000.00"), None, LIMIT_ID, 151,
            "INDOOR", "SPECTACLE", PERIOD_ID,
        )
        if actual != expected:
            raise RuntimeError(f"Claim 482 changed after preflight: {actual}")

    request_ids = tuple(item["request_id"] for item in NEW_CLAIMS)
    marks = ",".join(["%s"] * len(request_ids))
    cur.execute(
        f"SELECT request_id,remark FROM claims_request WHERE request_id IN ({marks}) FOR UPDATE",
        request_ids,
    )
    collisions = [row for row in cur.fetchall() if TAG not in (row["remark"] or "")]
    if collisions:
        raise RuntimeError(f"Request ID collision: {collisions}")

    # Refuse an amount/category duplicate under any request ID.
    for item in NEW_CLAIMS:
        cur.execute(
            """
            SELECT cr.id,cr.request_id,cr.request_status,cr.remark
            FROM claims_request cr
            JOIN application_user au ON au.id=cr.employee
            JOIN user_personal_details upd ON upd.id=au.user_personal_details
            JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details
            WHERE upd.epf_no=%s AND cr.insurance_details_limit_id=%s
              AND icd.treatment='INDOOR' AND icd.treatment_category=%s
              AND cr.request_amount=%s
              AND cr.request_status IN ('APPROVED','UNDER_REVIEW')
            FOR UPDATE
            """,
            (item["epf"], LIMIT_ID, item["category"], item["amount"]),
        )
        duplicates = [r for r in cur.fetchall() if TAG not in (r["remark"] or "")]
        if duplicates:
            raise RuntimeError(f"Unexpected duplicate candidate for EPF {item['epf']}: {duplicates}")


def update_workflow(cur) -> None:
    cur.execute(
        """
        UPDATE approval_work_flow aw
        JOIN insurance_claim_approval_work_flow link
          ON link.approval_work_flow_id=aw.id
        SET aw.status='APPROVED', aw.approved_amount=10000.00,
            aw.approved_user='MIGRATION_NS26_INDOOR',
            aw.approved_date=CURRENT_TIMESTAMP,
            aw.last_modified_date=CURRENT_TIMESTAMP
        WHERE link.insurance_claim_id=482 AND aw.status='UNDER_REVIEW'
        """
    )
    if cur.rowcount != 1:
        raise RuntimeError(f"Expected one pending workflow for claim 482, updated {cur.rowcount}")


def approve_existing(cur) -> None:
    row = one(cur, "SELECT remark FROM claims_request WHERE id=482")
    if TAG in (row["remark"] or ""):
        return
    cur.execute(
        """
        UPDATE claims_request
        SET request_status='APPROVED', approved_amount=10000.00,
            approval_level='LEVEL02', remark=%s,
            last_modified_date=CURRENT_TIMESTAMP
        WHERE id=482 AND request_status='UNDER_REVIEW'
          AND request_amount=10000.00 AND approved_amount IS NULL
        """,
        (append_tag(row["remark"], "EXISTING-CLAIM-482;BALANCE-SHEET-EPF237-EYE"),),
    )
    if cur.rowcount != 1:
        raise RuntimeError("Claim 482 approval precondition failed")
    update_workflow(cur)


def insert_claim(cur, users: dict[str, int], item: dict) -> int:
    cur.execute(
        "SELECT id,remark FROM claims_request WHERE request_id=%s",
        (item["request_id"],),
    )
    existing = cur.fetchone()
    if existing:
        if TAG not in (existing["remark"] or ""):
            raise RuntimeError(f"Request ID {item['request_id']} is not owned by this batch")
        return int(existing["id"])

    cur.execute(
        """
        INSERT INTO insurance_claims_details
          (created_date,last_modified_date,disease,from_treatment_date,
           to_treatment_date,treatment,treatment_category,
           insurance_staff_category_period)
        VALUES (%s,%s,'The Care Data Migration',NULL,%s,'INDOOR',%s,%s)
        """,
        (
            item["created"], item["created"], item["treatment_date"],
            item["category"], PERIOD_ID,
        ),
    )
    details_id = int(cur.lastrowid)
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
            item["created"], item["created"],
            append_tag("Approved legacy claim reconciliation", item["source"]),
            item["amount"], item["request_id"], users[item["epf"]],
            details_id, LIMIT_ID, item["quarter_id"], item["amount"],
        ),
    )
    return int(cur.lastrowid)


def fetch_final(cur) -> dict[str, dict[str, Decimal]]:
    epfs = tuple(EXPECTED_FINAL)
    marks = ",".join(["%s"] * len(epfs))
    cur.execute(
        f"""
        SELECT upd.epf_no,icd.treatment_category,
               COALESCE(SUM(cr.approved_amount),0) approved
        FROM claims_request cr
        JOIN application_user au ON au.id=cr.employee
        JOIN user_personal_details upd ON upd.id=au.user_personal_details
        JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details
        WHERE upd.epf_no IN ({marks})
          AND cr.insurance_details_limit_id=%s
          AND cr.request_status='APPROVED'
        GROUP BY upd.epf_no,icd.treatment_category
        ORDER BY CAST(upd.epf_no AS UNSIGNED),icd.treatment_category
        """,
        (*epfs, LIMIT_ID),
    )
    result: dict[str, dict[str, Decimal]] = {epf: {} for epf in epfs}
    for row in cur.fetchall():
        result[str(row["epf_no"])][row["treatment_category"]] = row["approved"]
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true", help="commit changes")
    args = parser.parse_args()

    with closing(_connect()) as connection:
        connection.autocommit = False
        with closing(connection.cursor(dictionary=True)) as cur:
            try:
                validate_configuration(cur)
                users = load_users(cur)
                validate_existing(cur)
                before = fetch_final(cur)
                print("Mode:", "APPLY" if args.apply else "DRY_RUN")
                print("Before:", before)
                approve_existing(cur)
                new_ids = [insert_claim(cur, users, item) for item in NEW_CLAIMS]
                after = fetch_final(cur)
                print("New/idempotent claim IDs:", new_ids)
                print("After:", after)
                if after != EXPECTED_FINAL:
                    raise RuntimeError(
                        f"Final category totals mismatch: expected {EXPECTED_FINAL}, found {after}"
                    )
                cur.execute(
                    "SELECT COUNT(*) n FROM claims_request WHERE remark LIKE %s",
                    (f"%{TAG}%",),
                )
                marker_count = int(cur.fetchone()["n"])
                if marker_count != 4:
                    raise RuntimeError(f"Expected four tagged claims, found {marker_count}")
                if args.apply:
                    connection.commit()
                    print(f"COMMITTED: marker={TAG}, tagged_claims={marker_count}")
                else:
                    connection.rollback()
                    print("DRY RUN PASSED; transaction rolled back")
            except Exception:
                connection.rollback()
                raise
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
