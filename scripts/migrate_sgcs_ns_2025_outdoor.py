"""Reconcile the approved SGCS Normal Staff 2025 OUTDOOR claims.

The script is intentionally idempotent and defaults to a read-only dry run.  It
uses the same externally configured connection helper as the other WeCare data
migration utilities.  Pass --apply only after the dry-run preconditions pass.
"""

from __future__ import annotations

import argparse
from contextlib import closing
from datetime import date, datetime
from decimal import Decimal

from wecare_spectacle import _connect


TAG = "[MIGRATION:SGCS-NS-2025-OUTDOOR-R1]"
LIMIT_ID = 7
PERIOD_ID = 3

EXPECTED_EXISTING = {
    388: ("193", "APPROVED", Decimal("7800.00"), Decimal("7800.00"), "OTHER"),
    389: ("194", "APPROVED", Decimal("3206.00"), Decimal("3206.00"), "OTHER"),
    394: ("237", "APPROVED", Decimal("5356.00"), Decimal("5356.00"), "OTHER"),
    396: ("244", "APPROVED", Decimal("8017.00"), Decimal("8017.00"), "OTHER"),
    434: ("247", "UNDER_REVIEW", Decimal("9000.00"), None, "OTHER"),
    469: ("176", "UNDER_REVIEW", Decimal("1543.00"), None, "OTHER"),
}

EXPECTED_FINAL = {
    "176": {"OTHER": Decimal("1523.00")},
    "193": {"OTHER": Decimal("2800.00"), "DENTAL": Decimal("5000.00")},
    "194": {"OTHER": Decimal("5606.00")},
    "206": {"OTHER": Decimal("2000.00")},
    "237": {"OTHER": Decimal("9000.00")},
    "244": {"OTHER": Decimal("8017.00")},
    "247": {"OTHER": Decimal("9000.00")},
}

NEW_CLAIMS = (
    {
        "epf": "193",
        "request_id": "HC/SGCS/NS/2025/0014",
        "requested": Decimal("5000.00"),
        "approved": Decimal("5000.00"),
        "category": "DENTAL",
        "treatment_date": date(2025, 9, 24),
        "created": datetime(2025, 9, 24, 9, 41),
        "quarter_id": 69,
        "source": "SGCS-OUTDOOR-000015",
    },
    {
        "epf": "194",
        "request_id": "HC/SGCS/NS/2025/0015",
        "requested": Decimal("2400.00"),
        "approved": Decimal("2400.00"),
        "category": "OTHER",
        "treatment_date": date(2025, 12, 13),
        "created": datetime(2025, 12, 13, 12, 0),
        "quarter_id": 64,
        "source": "SGCS-OUTDOOR-000345",
    },
    {
        "epf": "206",
        "request_id": "HC/SGCS/NS/2025/0016",
        "requested": Decimal("2000.00"),
        "approved": Decimal("2000.00"),
        "category": "OTHER",
        "treatment_date": date(2025, 7, 4),
        "created": datetime(2025, 7, 4, 13, 51),
        "quarter_id": 62,
        "source": "SGCS-OUTDOOR-000278;HISTORICAL-NS-OVERRIDE",
    },
    {
        "epf": "237",
        "request_id": "HC/SGCS/NS/2025/0017",
        "requested": Decimal("2650.00"),
        "approved": Decimal("2650.00"),
        "category": "OTHER",
        "treatment_date": date(2025, 11, 9),
        "created": datetime(2025, 11, 11, 3, 51, 16),
        "quarter_id": None,
        "source": "BALANCE-SHEET-EPF237-02",
    },
    {
        "epf": "237",
        "request_id": "HC/SGCS/NS/2025/0018",
        "requested": Decimal("994.00"),
        "approved": Decimal("994.00"),
        "category": "OTHER",
        "treatment_date": date(2025, 11, 9),
        "created": datetime(2025, 11, 11, 3, 51, 16),
        "quarter_id": None,
        "source": "BALANCE-SHEET-EPF237-03",
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
        raise RuntimeError("Expected exactly one row but query returned none")
    return row


def load_users(cur) -> dict[str, int]:
    epfs = tuple(EXPECTED_FINAL)
    marks = ",".join(["%s"] * len(epfs))
    cur.execute(
        f"""
        SELECT upd.epf_no, au.id
        FROM application_user au
        JOIN user_personal_details upd ON upd.id = au.user_personal_details
        WHERE upd.epf_no IN ({marks}) AND upd.user_status = 'ACTIVE'
        ORDER BY upd.epf_no, au.id
        FOR UPDATE
        """,
        epfs,
    )
    rows = cur.fetchall()
    grouped: dict[str, list[int]] = {epf: [] for epf in epfs}
    for row in rows:
        grouped[str(row["epf_no"])].append(int(row["id"]))
    invalid = {epf: ids for epf, ids in grouped.items() if len(ids) != 1}
    if invalid:
        raise RuntimeError(f"Expected one ACTIVE employee/application user per EPF: {invalid}")
    return {epf: ids[0] for epf, ids in grouped.items()}


def validate_configuration(cur) -> None:
    db = one(cur, "SELECT DATABASE() AS db")["db"]
    if db != "sgcs_care":
        raise RuntimeError(f"Refusing to run against database {db!r}; expected 'sgcs_care'")

    limit_row = one(
        cur,
        """
        SELECT id, global_limit, treatment, insurance_policy,
               insurance_staff_category_period
        FROM insurance_details_limit WHERE id = %s
        """,
        (LIMIT_ID,),
    )
    expected = (Decimal("9000.00"), "OUTDOOR", "P-NORMAL", PERIOD_ID)
    actual = (
        limit_row["global_limit"],
        limit_row["treatment"],
        limit_row["insurance_policy"],
        limit_row["insurance_staff_category_period"],
    )
    if actual != expected:
        raise RuntimeError(f"Unexpected OUTDOOR limit configuration: {actual}")

    cur.execute(
        """
        SELECT id, treatment_category_code, quarter_limit, from_date, to_date
        FROM insurance_quarter WHERE id IN (62, 64, 69) ORDER BY id
        """
    )
    quarters = {row["id"]: row for row in cur.fetchall()}
    if set(quarters) != {62, 64, 69}:
        raise RuntimeError(f"Required quarter/category rows are missing: {quarters}")
    if quarters[62]["treatment_category_code"] != "OTHER":
        raise RuntimeError("Quarter 62 is no longer OUTDOOR/OTHER")
    if quarters[64]["treatment_category_code"] != "OTHER":
        raise RuntimeError("Quarter 64 is no longer OUTDOOR/OTHER")
    if quarters[69]["treatment_category_code"] != "DENTAL":
        raise RuntimeError("Quarter 69 is no longer OUTDOOR/DENTAL")


def validate_existing(cur) -> None:
    for claim_id, expected in EXPECTED_EXISTING.items():
        row = one(
            cur,
            """
            SELECT cr.id, upd.epf_no, cr.request_status, cr.request_amount,
                   cr.approved_amount, icd.treatment_category, cr.remark
            FROM claims_request cr
            JOIN application_user au ON au.id = cr.employee
            JOIN user_personal_details upd ON upd.id = au.user_personal_details
            JOIN insurance_claims_details icd ON icd.id = cr.insurance_claims_details
            WHERE cr.id = %s
            FOR UPDATE
            """,
            (claim_id,),
        )
        if TAG in (row["remark"] or ""):
            continue
        actual = (
            str(row["epf_no"]),
            row["request_status"],
            row["request_amount"],
            row["approved_amount"],
            row["treatment_category"],
        )
        if actual != expected:
            raise RuntimeError(
                f"Claim {claim_id} changed since preflight. Expected {expected}, found {actual}"
            )

    request_ids = tuple(item["request_id"] for item in NEW_CLAIMS)
    marks = ",".join(["%s"] * len(request_ids))
    cur.execute(
        f"SELECT request_id, remark FROM claims_request WHERE request_id IN ({marks}) FOR UPDATE",
        request_ids,
    )
    collisions = [r for r in cur.fetchall() if TAG not in (r["remark"] or "")]
    if collisions:
        raise RuntimeError(f"New request ID collision: {collisions}")


def update_workflow(cur, claim_id: int, amount: Decimal) -> None:
    cur.execute(
        """
        UPDATE approval_work_flow aw
        JOIN insurance_claim_approval_work_flow link
          ON link.approval_work_flow_id = aw.id
        SET aw.status = 'APPROVED', aw.approved_amount = %s,
            aw.approved_user = 'MIGRATION_NS25_OUTDOOR',
            aw.approved_date = CURRENT_TIMESTAMP,
            aw.last_modified_date = CURRENT_TIMESTAMP
        WHERE link.insurance_claim_id = %s AND aw.status = 'UNDER_REVIEW'
        """,
        (amount, claim_id),
    )
    if cur.rowcount != 1:
        raise RuntimeError(
            f"Expected one UNDER_REVIEW workflow for claim {claim_id}, updated {cur.rowcount}"
        )


def insert_claim(cur, users: dict[str, int], item: dict) -> int:
    existing = one(
        cur,
        "SELECT COUNT(*) AS n FROM claims_request WHERE request_id = %s",
        (item["request_id"],),
    )["n"]
    if existing:
        row = one(
            cur,
            "SELECT id, remark FROM claims_request WHERE request_id = %s",
            (item["request_id"],),
        )
        if TAG not in (row["remark"] or ""):
            raise RuntimeError(f"Request ID {item['request_id']} belongs to a non-migration claim")
        return int(row["id"])

    cur.execute(
        """
        INSERT INTO insurance_claims_details
          (created_date, last_modified_date, disease, from_treatment_date,
           to_treatment_date, treatment, treatment_category,
           insurance_staff_category_period)
        VALUES (%s, %s, 'The Care Data Migration', NULL, %s,
                'OUTDOOR', %s, %s)
        """,
        (
            item["created"],
            item["created"],
            item["treatment_date"],
            item["category"],
            PERIOD_ID,
        ),
    )
    details_id = cur.lastrowid
    remark = append_tag("Approved legacy claim reconciliation", item["source"])
    cur.execute(
        """
        INSERT INTO claims_request
          (created_date, last_modified_date, remark, request_amount, request_id,
           request_status, dependent, employee, insurance_claims_details,
           approval_work_flow_id, insurance_details_limit_id,
           insurance_quarter_id, approval_level, approved_amount,
           assisted_mobile_no)
        VALUES (%s, %s, %s, %s, %s, 'APPROVED', NULL, %s, %s,
                NULL, %s, %s, 'LEVEL02', %s, NULL)
        """,
        (
            item["created"],
            item["created"],
            remark,
            item["requested"],
            item["request_id"],
            users[item["epf"]],
            details_id,
            LIMIT_ID,
            item["quarter_id"],
            item["approved"],
        ),
    )
    return int(cur.lastrowid)


def apply_changes(cur, users: dict[str, int]) -> list[int]:
    # EPF 176: use the confirmed 1,523 amount and approve its pending claim.
    row = one(cur, "SELECT remark FROM claims_request WHERE id = 469")
    if TAG not in (row["remark"] or ""):
        cur.execute(
            """
            UPDATE claims_request
            SET request_status='APPROVED', request_amount=1523.00,
                approved_amount=1523.00, approval_level='LEVEL02',
                insurance_quarter_id=64, remark=%s,
                last_modified_date=CURRENT_TIMESTAMP
            WHERE id=469 AND request_status='UNDER_REVIEW'
              AND request_amount=1543.00 AND approved_amount IS NULL
            """,
            (append_tag(row["remark"], "CONFIRMED-EPF176-1523"),),
        )
        if cur.rowcount != 1:
            raise RuntimeError("EPF 176 claim 469 update precondition failed")
        update_workflow(cur, 469, Decimal("1523.00"))

    # EPF 193: replace the old aggregate with the confirmed OTHER component.
    row = one(
        cur,
        "SELECT remark, insurance_claims_details FROM claims_request WHERE id = 388",
    )
    if TAG not in (row["remark"] or ""):
        cur.execute(
            """
            UPDATE claims_request
            SET request_amount=2800.00, approved_amount=2800.00,
                insurance_quarter_id=62, remark=%s,
                last_modified_date=CURRENT_TIMESTAMP
            WHERE id=388 AND request_status='APPROVED'
              AND request_amount=7800.00 AND approved_amount=7800.00
            """,
            (append_tag(row["remark"], "SGCS-OUTDOOR-000270;SPLIT-OTHER"),),
        )
        if cur.rowcount != 1:
            raise RuntimeError("EPF 193 aggregate claim 388 update precondition failed")
        cur.execute(
            """
            UPDATE insurance_claims_details
            SET treatment_category='OTHER', to_treatment_date='2025-06-11',
                insurance_staff_category_period=%s,
                disease='The Care Data Migration',
                last_modified_date=CURRENT_TIMESTAMP
            WHERE id=%s AND treatment='OUTDOOR'
            """,
            (PERIOD_ID, row["insurance_claims_details"]),
        )
        if cur.rowcount != 1:
            raise RuntimeError("EPF 193 claim details update failed")

    # EPF 244 already has the confirmed OTHER amount; add the audit marker only.
    row = one(cur, "SELECT remark FROM claims_request WHERE id = 396")
    if TAG not in (row["remark"] or ""):
        cur.execute(
            "UPDATE claims_request SET remark=%s, last_modified_date=CURRENT_TIMESTAMP WHERE id=396",
            (append_tag(row["remark"], "SGCS-OUTDOOR-000260;CONFIRMED-OTHER"),),
        )
        if cur.rowcount != 1:
            raise RuntimeError("EPF 244 audit marker update failed")

    # EPF 247: approve the existing pending claim without duplicating its 9,000.
    row = one(cur, "SELECT remark FROM claims_request WHERE id = 434")
    if TAG not in (row["remark"] or ""):
        cur.execute(
            """
            UPDATE claims_request
            SET request_status='APPROVED', approved_amount=9000.00,
                approval_level='LEVEL02', remark=%s,
                last_modified_date=CURRENT_TIMESTAMP
            WHERE id=434 AND request_status='UNDER_REVIEW'
              AND request_amount=9000.00 AND approved_amount IS NULL
            """,
            (append_tag(row["remark"], "SGCS-OUTDOOR-000333+000336"),),
        )
        if cur.rowcount != 1:
            raise RuntimeError("EPF 247 claim 434 update precondition failed")
        update_workflow(cur, 434, Decimal("9000.00"))

    return [insert_claim(cur, users, item) for item in NEW_CLAIMS]


def fetch_final(cur) -> dict[str, dict[str, Decimal]]:
    epfs = tuple(EXPECTED_FINAL)
    marks = ",".join(["%s"] * len(epfs))
    cur.execute(
        f"""
        SELECT upd.epf_no, icd.treatment_category,
               COALESCE(SUM(cr.approved_amount), 0) AS approved
        FROM claims_request cr
        JOIN application_user au ON au.id=cr.employee
        JOIN user_personal_details upd ON upd.id=au.user_personal_details
        JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details
        WHERE upd.epf_no IN ({marks})
          AND cr.insurance_details_limit_id=%s
          AND cr.request_status='APPROVED'
        GROUP BY upd.epf_no, icd.treatment_category
        ORDER BY CAST(upd.epf_no AS UNSIGNED), icd.treatment_category
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
                changed_ids = apply_changes(cur, users)
                after = fetch_final(cur)
                print("New/idempotent claim IDs:", changed_ids)
                print("After:", after)
                if after != EXPECTED_FINAL:
                    raise RuntimeError(
                        f"Final category totals mismatch. Expected {EXPECTED_FINAL}, found {after}"
                    )
                cur.execute(
                    "SELECT COUNT(*) AS n FROM claims_request WHERE remark LIKE %s",
                    (f"%{TAG}%",),
                )
                marker_count = cur.fetchone()["n"]
                if marker_count != 9:
                    raise RuntimeError(
                        f"Expected 9 specially tagged claims, found {marker_count}"
                    )
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
