"""Reconcile SGCS Normal Staff 2026 OUTDOOR claims and EPF 265 eligibility.

Dry-run is the default. Pass --apply only after all live preconditions and final
approved totals pass in the same transaction.
"""

from __future__ import annotations

import argparse
from contextlib import closing
from datetime import date, datetime
from decimal import Decimal

from wecare_spectacle import _connect


TAG = "[MIGRATION:SGCS-NS-2026-OUTDOOR-R1]"
LIMIT_ID = 42
PERIOD_ID = 12
GLOBAL_LIMIT = Decimal("9000.00")
EPF265_PERMANENT_DATE = date(2026, 5, 1)

EXPECTED_FINAL = {
    "102": Decimal("9000.00"),
    "162": Decimal("8359.00"),
    "194": Decimal("2395.00"),
    "199": Decimal("5200.00"),
    "200": Decimal("9000.00"),
    "211": Decimal("4050.00"),
    "247": Decimal("9000.00"),
}

EXISTING_CONVERSIONS = {
    496: {
        "epf": "194",
        "old_limit_id": 42,
        "old_treatment": "OUTDOOR",
        "old_requested": Decimal("2400.00"),
        "requested": Decimal("2395.64"),
        "approved": Decimal("2395.00"),
        "treatment_date": date(2026, 6, 26),
        "quarter_id": 145,
        "source": "SGCS-OUTDOOR-000357;TC/HC/SGCS/NS/OTHER/071",
    },
    487: {
        "epf": "200",
        "old_limit_id": 43,
        "old_treatment": "INDOOR",
        "old_requested": Decimal("14223.60"),
        "requested": Decimal("14223.60"),
        "approved": Decimal("9000.00"),
        "treatment_date": date(2026, 2, 9),
        "quarter_id": 144,
        "source": "SGCS-OUTDOOR-000346;TC/HC/SGCS/NS/OTHER/063",
    },
    489: {
        "epf": "247",
        "old_limit_id": 42,
        "old_treatment": "OUTDOOR",
        "old_requested": Decimal("9000.00"),
        "requested": Decimal("7205.00"),
        "approved": Decimal("7205.00"),
        "treatment_date": date(2026, 5, 4),
        "quarter_id": 145,
        "source": "SGCS-OUTDOOR-000347;TC/HC/SGCS/NS/OTHER/064;SPLIT-1",
    },
}

NEW_CLAIMS = (
    {
        "epf": "102", "request_id": "TC/HC/SGCS/NS/OTHER/070",
        "requested": Decimal("9260.00"), "approved": Decimal("9000.00"),
        "submitted": datetime(2026, 6, 25, 11, 18), "quarter_id": 145,
        "source": "SGCS-OUTDOOR-000355",
    },
    {
        "epf": "162", "request_id": "TC/HC/SGCS/NS/OTHER/067",
        "requested": Decimal("8359.00"), "approved": Decimal("8359.00"),
        "submitted": datetime(2026, 5, 14, 9, 21), "quarter_id": 145,
        "source": "SGCS-OUTDOOR-000352",
    },
    {
        "epf": "199", "request_id": "TC/HC/SGCS/NS/OTHER/066",
        "requested": Decimal("5200.00"), "approved": Decimal("5200.00"),
        "submitted": datetime(2026, 5, 14, 9, 21), "quarter_id": 145,
        "source": "SGCS-OUTDOOR-000351",
    },
    {
        "epf": "211", "request_id": "TC/HC/SGCS/NS/OTHER/068",
        "requested": Decimal("2300.00"), "approved": Decimal("1850.00"),
        "submitted": datetime(2026, 6, 2, 8, 46), "quarter_id": 145,
        "source": "SGCS-OUTDOOR-000353",
    },
    {
        "epf": "211", "request_id": "TC/HC/SGCS/NS/OTHER/069",
        "requested": Decimal("2200.00"), "approved": Decimal("2200.00"),
        "submitted": datetime(2026, 6, 16, 11, 57), "quarter_id": 145,
        "source": "SGCS-OUTDOOR-000354",
    },
    {
        "epf": "247", "request_id": "TC/HC/SGCS/NS/OTHER/065",
        "requested": Decimal("1800.00"), "approved": Decimal("1795.00"),
        "submitted": datetime(2026, 5, 4, 9, 9), "quarter_id": 145,
        "source": "SGCS-OUTDOOR-000348;SPLIT-2",
    },
)


def append_tag(remark: str | None, source: str) -> str:
    result = f"{(remark or '').strip()} {TAG} SOURCE:{source}".strip()
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
        SELECT global_limit,insurance_policy,treatment,status,
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
    expected = (GLOBAL_LIMIT, "P-NORMAL", "OUTDOOR", "ACTIVE", PERIOD_ID)
    if actual != expected:
        raise RuntimeError(f"Unexpected OUTDOOR limit configuration: {actual}")

    cur.execute(
        """
        SELECT id,treatment_category_code,quarter_limit,from_date,to_date
        FROM insurance_quarter WHERE id IN (144,145) ORDER BY id
        """
    )
    quarters = {row["id"]: row for row in cur.fetchall()}
    if set(quarters) != {144, 145}:
        raise RuntimeError(f"Required quarter rows are missing: {quarters}")
    expected_limits = {144: Decimal("9000.00"), 145: Decimal("6750.00")}
    for quarter_id, expected_limit in expected_limits.items():
        row = quarters[quarter_id]
        if row["treatment_category_code"] != "OTHER" or row["quarter_limit"] != expected_limit:
            raise RuntimeError(f"Unexpected quarter {quarter_id}: {row}")


def load_users(cur) -> dict[str, int]:
    epfs = tuple(set(EXPECTED_FINAL) | {"265"})
    marks = ",".join(["%s"] * len(epfs))
    cur.execute(
        f"""
        SELECT upd.epf_no,au.id,upd.user_status,ucd.company_type,
               ucd.staff_category,ucd.insurance_policy
        FROM user_personal_details upd
        JOIN user_company_details ucd ON ucd.id=upd.user_company_details
        JOIN application_user au ON au.user_personal_details=upd.id
        WHERE upd.epf_no IN ({marks})
        ORDER BY upd.epf_no,au.id FOR UPDATE
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
    for claim_id, spec in EXISTING_CONVERSIONS.items():
        row = one(
            cur,
            """
            SELECT upd.epf_no,cr.request_status,cr.request_amount,
                   cr.approved_amount,cr.insurance_details_limit_id,
                   cr.remark,icd.treatment,icd.treatment_category,
                   icd.insurance_staff_category_period
            FROM claims_request cr
            JOIN application_user au ON au.id=cr.employee
            JOIN user_personal_details upd ON upd.id=au.user_personal_details
            JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details
            WHERE cr.id=%s FOR UPDATE
            """,
            (claim_id,),
        )
        if TAG in (row["remark"] or ""):
            continue
        actual = (
            str(row["epf_no"]), row["request_status"], row["request_amount"],
            row["approved_amount"], row["insurance_details_limit_id"],
            row["treatment"], row["treatment_category"],
            row["insurance_staff_category_period"],
        )
        expected = (
            spec["epf"], "UNDER_REVIEW", spec["old_requested"], None,
            spec["old_limit_id"], spec["old_treatment"], "OTHER", PERIOD_ID,
        )
        if actual != expected:
            raise RuntimeError(f"Claim {claim_id} changed after preflight: {actual}")

    request_ids = tuple(item["request_id"] for item in NEW_CLAIMS)
    marks = ",".join(["%s"] * len(request_ids))
    cur.execute(
        f"SELECT request_id,remark FROM claims_request WHERE request_id IN ({marks}) FOR UPDATE",
        request_ids,
    )
    collisions = [row for row in cur.fetchall() if TAG not in (row["remark"] or "")]
    if collisions:
        raise RuntimeError(f"Source request ID collision: {collisions}")


def update_workflow(cur, claim_id: int, amount: Decimal) -> None:
    cur.execute(
        """
        UPDATE approval_work_flow aw
        JOIN insurance_claim_approval_work_flow link
          ON link.approval_work_flow_id=aw.id
        SET aw.status='APPROVED',aw.approved_amount=%s,
            aw.approved_user='MIGRATION_NS26_OUTDOOR',
            aw.approved_date=CURRENT_TIMESTAMP,
            aw.last_modified_date=CURRENT_TIMESTAMP
        WHERE link.insurance_claim_id=%s AND aw.status='UNDER_REVIEW'
        """,
        (amount, claim_id),
    )
    if cur.rowcount != 1:
        raise RuntimeError(f"Expected one pending workflow for claim {claim_id}; updated {cur.rowcount}")


def convert_existing(cur, claim_id: int, spec: dict) -> None:
    row = one(
        cur,
        "SELECT remark,insurance_claims_details FROM claims_request WHERE id=%s",
        (claim_id,),
    )
    if TAG in (row["remark"] or ""):
        return
    cur.execute(
        """
        UPDATE claims_request
        SET request_status='APPROVED',request_amount=%s,approved_amount=%s,
            approval_level='LEVEL02',insurance_details_limit_id=%s,
            insurance_quarter_id=%s,remark=%s,
            last_modified_date=CURRENT_TIMESTAMP
        WHERE id=%s AND request_status='UNDER_REVIEW'
          AND request_amount=%s AND approved_amount IS NULL
        """,
        (
            spec["requested"], spec["approved"], LIMIT_ID, spec["quarter_id"],
            append_tag(row["remark"], spec["source"]), claim_id,
            spec["old_requested"],
        ),
    )
    if cur.rowcount != 1:
        raise RuntimeError(f"Claim {claim_id} conversion precondition failed")
    cur.execute(
        """
        UPDATE insurance_claims_details
        SET treatment='OUTDOOR',treatment_category='OTHER',
            to_treatment_date=%s,insurance_staff_category_period=%s,
            disease='The Care Data Migration',last_modified_date=CURRENT_TIMESTAMP
        WHERE id=%s
        """,
        (spec["treatment_date"], PERIOD_ID, row["insurance_claims_details"]),
    )
    if cur.rowcount != 1:
        raise RuntimeError(f"Claim {claim_id} details conversion failed")
    update_workflow(cur, claim_id, spec["approved"])


def insert_claim(cur, users: dict[str, int], spec: dict) -> int:
    cur.execute(
        "SELECT id,remark FROM claims_request WHERE request_id=%s",
        (spec["request_id"],),
    )
    existing = cur.fetchone()
    if existing:
        if TAG not in (existing["remark"] or ""):
            raise RuntimeError(f"Request ID {spec['request_id']} is not owned by this batch")
        return int(existing["id"])

    treatment_date = spec["submitted"].date()
    cur.execute(
        """
        INSERT INTO insurance_claims_details
          (created_date,last_modified_date,disease,from_treatment_date,
           to_treatment_date,treatment,treatment_category,
           insurance_staff_category_period)
        VALUES (%s,%s,'The Care Data Migration',NULL,%s,'OUTDOOR','OTHER',%s)
        """,
        (spec["submitted"], spec["submitted"], treatment_date, PERIOD_ID),
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
            spec["submitted"], spec["submitted"],
            append_tag("Approved legacy claim reconciliation", spec["source"]),
            spec["requested"], spec["request_id"], users[spec["epf"]],
            details_id, LIMIT_ID, spec["quarter_id"], spec["approved"],
        ),
    )
    return int(cur.lastrowid)


def correct_epf265_permanent_date(cur) -> None:
    row = one(
        cur,
        """
        SELECT ucd.id,ucd.permanent_date
        FROM user_company_details ucd
        JOIN user_personal_details upd ON upd.user_company_details=ucd.id
        WHERE upd.epf_no='265' AND upd.user_status='ACTIVE'
          AND ucd.company_type='SGCS'
        FOR UPDATE
        """,
    )
    current = row["permanent_date"]
    current_date = current.date() if hasattr(current, "date") else current
    if current_date == EPF265_PERMANENT_DATE:
        return
    if current_date != date(2026, 2, 5):
        raise RuntimeError(f"EPF 265 permanent date changed after preflight: {current_date}")
    cur.execute(
        """
        UPDATE user_company_details
        SET permanent_date=%s,last_modified_date=CURRENT_TIMESTAMP
        WHERE id=%s AND DATE(permanent_date)='2026-02-05'
        """,
        (EPF265_PERMANENT_DATE, row["id"]),
    )
    if cur.rowcount != 1:
        raise RuntimeError("EPF 265 permanent-date correction failed")


def fetch_final(cur) -> dict[str, Decimal]:
    epfs = tuple(EXPECTED_FINAL)
    marks = ",".join(["%s"] * len(epfs))
    cur.execute(
        f"""
        SELECT upd.epf_no,COALESCE(SUM(cr.approved_amount),0) approved
        FROM user_personal_details upd
        JOIN application_user au ON au.user_personal_details=upd.id
        LEFT JOIN claims_request cr ON cr.employee=au.id
          AND cr.insurance_details_limit_id=%s
          AND cr.request_status='APPROVED'
        WHERE upd.epf_no IN ({marks})
        GROUP BY upd.epf_no
        ORDER BY CAST(upd.epf_no AS UNSIGNED)
        """,
        (LIMIT_ID, *epfs),
    )
    return {str(row["epf_no"]): row["approved"] for row in cur.fetchall()}


def validate_epf265_eligibility(cur) -> None:
    row = one(
        cur,
        """
        SELECT DATE(ucd.permanent_date) permanent_date,iq.id quarter_id,
               iq.quarter_limit
        FROM user_personal_details upd
        JOIN user_company_details ucd ON ucd.id=upd.user_company_details
        JOIN insurance_quarter iq ON iq.insurance_details_id=%s
          AND iq.treatment_category_code='OTHER'
          AND DATE(ucd.permanent_date) BETWEEN iq.from_date AND iq.to_date
        WHERE upd.epf_no='265' AND upd.user_status='ACTIVE'
        """,
        (LIMIT_ID,),
    )
    actual = (row["permanent_date"], row["quarter_id"], row["quarter_limit"])
    expected = (EPF265_PERMANENT_DATE, 145, Decimal("6750.00"))
    if actual != expected:
        raise RuntimeError(f"EPF 265 eligibility mismatch: expected {expected}, found {actual}")


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
                print("Before approved totals:", before)
                for claim_id, spec in EXISTING_CONVERSIONS.items():
                    convert_existing(cur, claim_id, spec)
                new_ids = [insert_claim(cur, users, spec) for spec in NEW_CLAIMS]
                correct_epf265_permanent_date(cur)
                after = fetch_final(cur)
                validate_epf265_eligibility(cur)
                print("New/idempotent claim IDs:", new_ids)
                print("After approved totals:", after)
                print("After balances:", {epf: GLOBAL_LIMIT-amount for epf, amount in after.items()})
                print("EPF 265 eligibility: permanent_date=2026-05-01, available_limit=6750.00")
                if after != EXPECTED_FINAL:
                    raise RuntimeError(
                        f"Final totals mismatch: expected {EXPECTED_FINAL}, found {after}"
                    )
                cur.execute(
                    "SELECT COUNT(*) n FROM claims_request WHERE remark LIKE %s",
                    (f"%{TAG}%",),
                )
                marker_count = int(cur.fetchone()["n"])
                if marker_count != 9:
                    raise RuntimeError(f"Expected nine tagged claims, found {marker_count}")
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
