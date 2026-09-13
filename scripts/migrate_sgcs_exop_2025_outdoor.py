"""Reconcile SGCS EX-OP1/EX-OP2 2025 OUTDOOR claims.

This migration is intentionally idempotent and transactional. Dry-run is the
default; pass --apply only after every precondition and final-balance check
passes. It also corrects the confirmed permanent-date quarter references from
the three immediately preceding SGCS Normal Staff migrations.
"""

from __future__ import annotations

import argparse
from contextlib import closing
from datetime import datetime
from decimal import Decimal

from wecare_spectacle import _connect


TAG = "[MIGRATION:SGCS-EXOP1-EXOP2-2025-OUTDOOR-R1]"
QUARTER_FIX_TAG = "[CORRECTION:PERMANENT-DATE-QUARTER-R1]"

LIMITS = {
    1: {
        "policy": "P-EXE-OP1",
        "staff": "EX-OP1",
        "period": 4,
        "global": Decimal("12000.00"),
    },
    4: {
        "policy": "P-EXE-OP2",
        "staff": "EX-OP2",
        "period": 5,
        "global": Decimal("20000.00"),
    },
}

EXPECTED_FINAL = {
    "98": {"OTHER": Decimal("12000.00")},
    "206": {"OTHER": Decimal("4838.00")},
    "229": {"OTHER": Decimal("1200.00")},
    "230": {},
    "242": {"OTHER": Decimal("10319.00")},
    "249": {"DENTAL": Decimal("5000.00"), "OTHER": Decimal("1900.00")},
    "253": {"OTHER": Decimal("11562.00")},
    "256": {},
}

# Existing claims that match the workbook. Their financial values are not
# changed; only category/quarter metadata and the migration marker are aligned.
EXISTING_RECONCILIATIONS = {
    380: {
        "epf": "249", "request_id": "HC/SGCS/UBD/2025/0008",
        "requested": Decimal("5000.00"), "approved": Decimal("5000.00"),
        "limit": 1, "period": 4, "old_category": "OTHER",
        "category": "DENTAL", "quarter": 74,
        "source": "SGCS-OUTDOOR-000016;TC/HC/SGCS/EXOP1/DENTAL/001",
    },
    429: {
        "epf": "242", "request_id": "HC/SGCS/EX-OP2/2025/0007",
        "requested": Decimal("5495.00"), "approved": Decimal("5495.00"),
        "limit": 4, "period": 5, "old_category": "OTHER",
        "category": "OTHER", "quarter": 76,
        "source": "SGCS-OUTDOOR-000314;TC/HC/SGCS/EXOP2/OTHER/001",
    },
    430: {
        "epf": "242", "request_id": "HC/SGCS/EX-OP2/2025/0008",
        "requested": Decimal("2412.00"), "approved": Decimal("2412.00"),
        "limit": 4, "period": 5, "old_category": "OTHER",
        "category": "OTHER", "quarter": 76,
        "source": "SGCS-OUTDOOR-000315;TC/HC/SGCS/EXOP2/OTHER/002",
    },
    432: {
        "epf": "242", "request_id": "HC/SGCS/EX-OP2/2025/0009",
        "requested": Decimal("2412.00"), "approved": Decimal("2412.00"),
        "limit": 4, "period": 5, "old_category": "OTHER",
        "category": "OTHER", "quarter": 76,
        "source": "SGCS-OUTDOOR-000316;TC/HC/SGCS/EXOP2/OTHER/003",
    },
    456: {
        "epf": "206", "request_id": "HC/SGCS/EX-OP1/2026/0001",
        "requested": Decimal("4860.85"), "approved": Decimal("4838.00"),
        "limit": 1, "period": 4, "old_category": "OTHER",
        "category": "OTHER", "quarter": 71,
        "source": "SGCS-OUTDOOR-000321;TC/HC/SGCS/EXOP1/OTHER/037",
    },
    458: {
        "epf": "229", "request_id": "HC/SGCS/EX-OP1/2026/0002",
        "requested": Decimal("1200.00"), "approved": Decimal("1200.00"),
        "limit": 1, "period": 4, "old_category": "OTHER",
        "category": "OTHER", "quarter": 71,
        "source": "SGCS-OUTDOOR-000322;TC/HC/SGCS/EXOP1/OTHER/038",
    },
}

NEW_CLAIMS = (
    {
        "epf": "98", "request_id": "TC/HC/SGCS/EXOP1/OTHER/034",
        "requested": Decimal("2500.00"), "approved": Decimal("2500.00"),
        "submitted": datetime(2025, 8, 27, 11, 42),
        "limit": 1, "period": 4, "quarter": 71, "category": "OTHER",
        "source": "SGCS-OUTDOOR-000284",
    },
    {
        "epf": "98", "request_id": "TC/HC/SGCS/EXOP1/OTHER/035",
        "requested": Decimal("1300.00"), "approved": Decimal("1300.00"),
        "submitted": datetime(2025, 9, 2, 15, 49),
        "limit": 1, "period": 4, "quarter": 71, "category": "OTHER",
        "source": "SGCS-OUTDOOR-000285",
    },
    {
        "epf": "98", "request_id": "TC/HC/SGCS/EXOP1/OTHER/036",
        "requested": Decimal("14803.00"), "approved": Decimal("8200.00"),
        "submitted": datetime(2025, 10, 1, 13, 40),
        "limit": 1, "period": 4, "quarter": 71, "category": "OTHER",
        "source": "SGCS-OUTDOOR-000290;APPROVED-LIMIT-EXCESS-6603",
    },
    {
        "epf": "249", "request_id": "TC/HC/SGCS/EXOP1/OTHER/043",
        "requested": Decimal("4035.30"), "approved": Decimal("1900.00"),
        "submitted": datetime(2026, 6, 25, 13, 33),
        "limit": 1, "period": 4, "quarter": 71, "category": "OTHER",
        "source": "SGCS-OUTDOOR-000356;CONFIRMED-APPROVED",
    },
)

# These values were previously selected using claim date. The production
# service selects the eligibility row using the employee permanent date.
PRIOR_QUARTER_FIXES = {
    # SGCS Normal Staff 2025 OUTDOOR: first permanent-date window is quarter 61.
    388: ("193", Decimal("2800.00"), 7, "OTHER", 3, 61),
    396: ("244", Decimal("8017.00"), 7, "OTHER", 3, 61),
    469: ("176", Decimal("1523.00"), 7, "OTHER", 3, 61),
    647: ("194", Decimal("2400.00"), 7, "OTHER", 3, 61),
    648: ("206", Decimal("2000.00"), 7, "OTHER", 3, 61),
    649: ("237", Decimal("2650.00"), 7, "OTHER", 3, 61),
    650: ("237", Decimal("994.00"), 7, "OTHER", 3, 61),
    # SGCS Normal Staff 2026 INDOOR: first OTHER window is quarter 147.
    656: ("247", Decimal("8000.00"), 43, "OTHER", 12, 147),
    # SGCS Normal Staff 2026 OUTDOOR: first OTHER window is quarter 144.
    489: ("247", Decimal("7205.00"), 42, "OTHER", 12, 144),
    496: ("194", Decimal("2395.00"), 42, "OTHER", 12, 144),
    663: ("102", Decimal("9000.00"), 42, "OTHER", 12, 144),
    664: ("162", Decimal("8359.00"), 42, "OTHER", 12, 144),
    665: ("199", Decimal("5200.00"), 42, "OTHER", 12, 144),
    666: ("211", Decimal("1850.00"), 42, "OTHER", 12, 144),
    667: ("211", Decimal("2200.00"), 42, "OTHER", 12, 144),
    668: ("247", Decimal("1795.00"), 42, "OTHER", 12, 144),
}


def one(cur, sql: str, params=()):
    cur.execute(sql, params)
    row = cur.fetchone()
    if row is None:
        raise RuntimeError("Expected a row but query returned none")
    return row


def append_marker(remark: str | None, marker: str, source: str | None = None) -> str:
    original = (remark or "").strip()
    suffix = marker if source is None else f"{marker} SOURCE:{source}"
    if marker in original:
        return original
    value = f"{original} {suffix}".strip()
    if len(value) > 255:
        raise RuntimeError(f"Tagged remark exceeds 255 characters: {value}")
    return value


def validate_configuration(cur) -> None:
    database = one(cur, "SELECT DATABASE() db")["db"]
    if database != "sgcs_care":
        raise RuntimeError(f"Refusing database {database!r}; expected live 'sgcs_care'")

    for limit_id, expected in LIMITS.items():
        row = one(
            cur,
            """
            SELECT global_limit,insurance_policy,treatment,status,
                   insurance_staff_category_period
            FROM insurance_details_limit WHERE id=%s
            """,
            (limit_id,),
        )
        actual = (
            row["global_limit"], row["insurance_policy"], row["treatment"],
            row["status"], row["insurance_staff_category_period"],
        )
        wanted = (
            expected["global"], expected["policy"], "OUTDOOR", "ACTIVE",
            expected["period"],
        )
        if actual != wanted:
            raise RuntimeError(f"Limit {limit_id} configuration changed: {actual}")

    expected_quarters = {
        61: (7, "OTHER", Decimal("9000.00")),
        71: (1, "OTHER", Decimal("12000.00")),
        74: (1, "DENTAL", Decimal("5000.00")),
        76: (4, "OTHER", Decimal("20000.00")),
        144: (42, "OTHER", Decimal("9000.00")),
        147: (43, "OTHER", Decimal("100000.00")),
    }
    marks = ",".join(["%s"] * len(expected_quarters))
    cur.execute(
        f"""
        SELECT id,insurance_details_id,treatment_category_code,quarter_limit
        FROM insurance_quarter WHERE id IN ({marks})
        """,
        tuple(expected_quarters),
    )
    found = {
        int(row["id"]): (
            int(row["insurance_details_id"]), row["treatment_category_code"],
            row["quarter_limit"],
        )
        for row in cur.fetchall()
    }
    if found != expected_quarters:
        raise RuntimeError(f"Required quarter configuration changed: {found}")


def load_users(cur) -> dict[str, int]:
    epfs = tuple(EXPECTED_FINAL)
    marks = ",".join(["%s"] * len(epfs))
    cur.execute(
        f"""
        SELECT upd.epf_no,au.id,upd.user_status,ucd.company_type,
               ucd.staff_category,ucd.insurance_policy,DATE(ucd.permanent_date) permanent_date
        FROM user_personal_details upd
        JOIN user_company_details ucd ON ucd.id=upd.user_company_details
        JOIN application_user au ON au.user_personal_details=upd.id
        WHERE upd.epf_no IN ({marks}) AND ucd.company_type='SGCS'
        ORDER BY upd.epf_no,au.id FOR UPDATE
        """,
        epfs,
    )
    grouped: dict[str, list[dict]] = {epf: [] for epf in epfs}
    for row in cur.fetchall():
        grouped[str(row["epf_no"])].append(row)

    invalid = {}
    for epf, rows in grouped.items():
        if len(rows) != 1:
            invalid[epf] = rows
            continue
        row = rows[0]
        limit_id = 4 if epf == "242" else 1
        expected = LIMITS[limit_id]
        if (
            row["user_status"] != "ACTIVE"
            or row["company_type"] != "SGCS"
            or row["staff_category"] != expected["staff"]
            or row["insurance_policy"] != expected["policy"]
        ):
            invalid[epf] = rows
    if invalid:
        raise RuntimeError(f"Target employee/profile mismatch: {invalid}")
    return {epf: int(rows[0]["id"]) for epf, rows in grouped.items()}


def claim_row(cur, claim_id: int):
    return one(
        cur,
        """
        SELECT cr.id,upd.epf_no,cr.request_id,cr.request_status,
               cr.request_amount,cr.approved_amount,
               cr.insurance_details_limit_id,cr.insurance_quarter_id,
               cr.remark,cr.insurance_claims_details,
               icd.treatment,icd.treatment_category,
               icd.insurance_staff_category_period
        FROM claims_request cr
        JOIN application_user au ON au.id=cr.employee
        JOIN user_personal_details upd ON upd.id=au.user_personal_details
        JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details
        WHERE cr.id=%s FOR UPDATE
        """,
        (claim_id,),
    )


def reconcile_existing(cur) -> list[int]:
    changed = []
    for claim_id, spec in EXISTING_RECONCILIATIONS.items():
        row = claim_row(cur, claim_id)
        immutable = (
            str(row["epf_no"]), row["request_id"], row["request_status"],
            row["request_amount"], row["approved_amount"],
            row["insurance_details_limit_id"], row["treatment"],
            row["insurance_staff_category_period"],
        )
        expected = (
            spec["epf"], spec["request_id"], "APPROVED", spec["requested"],
            spec["approved"], spec["limit"], "OUTDOOR", spec["period"],
        )
        if immutable != expected:
            raise RuntimeError(f"Claim {claim_id} changed after preflight: {immutable}")
        if row["treatment_category"] not in {spec["old_category"], spec["category"]}:
            raise RuntimeError(f"Claim {claim_id} has unexpected category {row['treatment_category']}")
        if row["insurance_quarter_id"] not in {None, spec["quarter"]}:
            raise RuntimeError(f"Claim {claim_id} has unexpected quarter {row['insurance_quarter_id']}")

        final = (
            row["treatment_category"] == spec["category"]
            and row["insurance_quarter_id"] == spec["quarter"]
            and TAG in (row["remark"] or "")
        )
        if final:
            continue
        cur.execute(
            """
            UPDATE claims_request
            SET insurance_quarter_id=%s,remark=%s,last_modified_date=CURRENT_TIMESTAMP
            WHERE id=%s
            """,
            (
                spec["quarter"],
                append_marker(row["remark"], TAG, spec["source"]),
                claim_id,
            ),
        )
        if cur.rowcount != 1:
            raise RuntimeError(f"Claim {claim_id} request reconciliation failed")
        if row["treatment_category"] != spec["category"]:
            cur.execute(
                """
                UPDATE insurance_claims_details
                SET treatment_category=%s,last_modified_date=CURRENT_TIMESTAMP
                WHERE id=%s AND treatment_category=%s
                """,
                (
                    spec["category"], row["insurance_claims_details"],
                    spec["old_category"],
                ),
            )
            if cur.rowcount != 1:
                raise RuntimeError(f"Claim {claim_id} category reconciliation failed")
        changed.append(claim_id)
    return changed


def insert_claim(cur, users: dict[str, int], spec: dict) -> tuple[int, bool]:
    cur.execute(
        "SELECT id FROM claims_request WHERE request_id=%s FOR UPDATE",
        (spec["request_id"],),
    )
    existing = cur.fetchone()
    if existing:
        row = claim_row(cur, int(existing["id"]))
        actual = (
            str(row["epf_no"]), row["request_status"], row["request_amount"],
            row["approved_amount"], row["insurance_details_limit_id"],
            row["insurance_quarter_id"], row["treatment"],
            row["treatment_category"], row["insurance_staff_category_period"],
        )
        expected = (
            spec["epf"], "APPROVED", spec["requested"], spec["approved"],
            spec["limit"], spec["quarter"], "OUTDOOR", spec["category"],
            spec["period"],
        )
        if TAG not in (row["remark"] or "") or actual != expected:
            raise RuntimeError(
                f"Request ID {spec['request_id']} is not this migration row: {actual}"
            )
        return int(row["id"]), False

    cur.execute(
        """
        INSERT INTO insurance_claims_details
          (created_date,last_modified_date,disease,from_treatment_date,
           to_treatment_date,treatment,treatment_category,
           insurance_staff_category_period)
        VALUES (%s,%s,'The Care Data Migration',NULL,%s,'OUTDOOR',%s,%s)
        """,
        (
            spec["submitted"], spec["submitted"], spec["submitted"].date(),
            spec["category"], spec["period"],
        ),
    )
    details_id = int(cur.lastrowid)
    remark = append_marker("Approved legacy claim reconciliation", TAG, spec["source"])
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
            spec["submitted"], spec["submitted"], remark, spec["requested"],
            spec["request_id"], users[spec["epf"]], details_id,
            spec["limit"], spec["quarter"], spec["approved"],
        ),
    )
    return int(cur.lastrowid), True


def correct_prior_quarters(cur) -> list[int]:
    changed = []
    for claim_id, expected in PRIOR_QUARTER_FIXES.items():
        epf, approved, limit_id, category, target_period, target_quarter = expected
        row = claim_row(cur, claim_id)
        immutable = (
            str(row["epf_no"]), row["request_status"], row["approved_amount"],
            row["insurance_details_limit_id"], row["treatment"],
            row["treatment_category"],
        )
        wanted = (epf, "APPROVED", approved, limit_id, "OUTDOOR" if limit_id != 43 else "INDOOR", category)
        if immutable != wanted:
            raise RuntimeError(f"Prior claim {claim_id} changed after preflight: {immutable}")
        if row["insurance_quarter_id"] == target_quarter and row["insurance_staff_category_period"] == target_period and QUARTER_FIX_TAG in (row["remark"] or ""):
            continue
        cur.execute(
            """
            UPDATE claims_request
            SET insurance_quarter_id=%s,remark=%s,last_modified_date=CURRENT_TIMESTAMP
            WHERE id=%s
            """,
            (
                target_quarter,
                append_marker(row["remark"], QUARTER_FIX_TAG),
                claim_id,
            ),
        )
        if cur.rowcount != 1:
            raise RuntimeError(f"Prior claim {claim_id} quarter correction failed")
        if row["insurance_staff_category_period"] != target_period:
            cur.execute(
                """
                UPDATE insurance_claims_details
                SET insurance_staff_category_period=%s,last_modified_date=CURRENT_TIMESTAMP
                WHERE id=%s
                """,
                (target_period, row["insurance_claims_details"]),
            )
            if cur.rowcount != 1:
                raise RuntimeError(f"Prior claim {claim_id} period correction failed")
        changed.append(claim_id)
    return changed


def fetch_final(cur) -> dict[str, dict[str, Decimal]]:
    epfs = tuple(EXPECTED_FINAL)
    marks = ",".join(["%s"] * len(epfs))
    cur.execute(
        f"""
        SELECT upd.epf_no,icd.treatment_category,SUM(cr.approved_amount) approved
        FROM claims_request cr
        JOIN application_user au ON au.id=cr.employee
        JOIN user_personal_details upd ON upd.id=au.user_personal_details
        JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details
        WHERE upd.epf_no IN ({marks})
          AND cr.insurance_details_limit_id IN (1,4)
          AND cr.request_status='APPROVED'
        GROUP BY upd.epf_no,icd.treatment_category
        ORDER BY CAST(upd.epf_no AS UNSIGNED),icd.treatment_category
        """,
        epfs,
    )
    result: dict[str, dict[str, Decimal]] = {epf: {} for epf in epfs}
    for row in cur.fetchall():
        result[str(row["epf_no"])][row["treatment_category"]] = row["approved"]
    return result


def balances(totals: dict[str, dict[str, Decimal]]) -> dict[str, Decimal]:
    result = {}
    for epf, categories in totals.items():
        limit = LIMITS[4]["global"] if epf == "242" else LIMITS[1]["global"]
        result[epf] = limit - sum(categories.values(), Decimal("0.00"))
    return result


def validate_final_metadata(cur) -> None:
    for claim_id, spec in EXISTING_RECONCILIATIONS.items():
        row = claim_row(cur, claim_id)
        if (
            row["insurance_quarter_id"] != spec["quarter"]
            or row["treatment_category"] != spec["category"]
            or TAG not in (row["remark"] or "")
        ):
            raise RuntimeError(f"Claim {claim_id} did not reach final metadata state")
    for claim_id, expected in PRIOR_QUARTER_FIXES.items():
        row = claim_row(cur, claim_id)
        if (
            row["insurance_quarter_id"] != expected[5]
            or row["insurance_staff_category_period"] != expected[4]
            or QUARTER_FIX_TAG not in (row["remark"] or "")
        ):
            raise RuntimeError(f"Prior claim {claim_id} did not reach corrected state")


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
                before = fetch_final(cur)
                print("Mode:", "APPLY" if args.apply else "DRY_RUN")
                print("Before totals:", before)
                print("Before balances:", balances(before))

                reconciled = reconcile_existing(cur)
                new_results = [insert_claim(cur, users, spec) for spec in NEW_CLAIMS]
                quarter_fixed = correct_prior_quarters(cur)

                after = fetch_final(cur)
                after_balances = balances(after)
                validate_final_metadata(cur)
                if after != EXPECTED_FINAL:
                    raise RuntimeError(
                        f"Final category totals mismatch: expected {EXPECTED_FINAL}, found {after}"
                    )
                if any(value < 0 for value in after_balances.values()):
                    raise RuntimeError(f"A final employee balance is negative: {after_balances}")

                cur.execute(
                    "SELECT COUNT(*) n FROM claims_request WHERE remark LIKE %s",
                    (f"%{TAG}%",),
                )
                migration_count = int(cur.fetchone()["n"])
                cur.execute(
                    "SELECT COUNT(*) n FROM claims_request WHERE remark LIKE %s",
                    (f"%{QUARTER_FIX_TAG}%",),
                )
                quarter_fix_count = int(cur.fetchone()["n"])
                if migration_count != 10:
                    raise RuntimeError(f"Expected 10 current migration markers, found {migration_count}")
                if quarter_fix_count != len(PRIOR_QUARTER_FIXES):
                    raise RuntimeError(
                        f"Expected {len(PRIOR_QUARTER_FIXES)} quarter-fix markers, found {quarter_fix_count}"
                    )

                print("Existing claims reconciled:", reconciled)
                print("New/idempotent claim IDs:", new_results)
                print("Prior quarter claims corrected:", quarter_fixed)
                print("Final totals:", after)
                print("Final balances:", after_balances)

                if args.apply:
                    connection.commit()
                    print(
                        f"COMMITTED: marker={TAG}, migration_claims={migration_count}, "
                        f"quarter_corrections={quarter_fix_count}"
                    )
                else:
                    connection.rollback()
                    print("DRY RUN PASSED; transaction rolled back")
            except Exception:
                connection.rollback()
                raise
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
