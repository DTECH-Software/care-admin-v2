"""Reconcile SGCS Middle Management 2025-2026 OUTDOOR claims.

The migration is idempotent and transactional. Dry-run is the default. Pass
--apply only after every live precondition and final-balance assertion passes.
"""

from __future__ import annotations

import argparse
from contextlib import closing
from datetime import datetime
from decimal import Decimal

from wecare_spectacle import _connect


TAG = "[MIGRATION:SGCS-MM-2025-OUTDOOR-R1]"
LIMIT_ID = 10
PERIOD_ID = 6
GLOBAL_LIMIT = Decimal("30000.00")

EXPECTED_FINAL = {
    "22": {"OTHER": Decimal("29530.00")},
    "181": {"OTHER": Decimal("1690.00")},
    "195": {"DENTAL": Decimal("10000.00")},
    "233": {"OTHER": Decimal("14900.00")},
    "252": {"DENTAL": Decimal("3000.00"), "OTHER": Decimal("26580.00")},
}

# Existing live rows matched to the workbook/balance sheet. Claim 383 is the
# legacy aggregate: 15,940 = 12,940 OTHER + 3,000 DENTAL. The DENTAL component
# is split into its own specially marked claim below.
EXISTING = {
    381: {
        "epf": "22", "request_id": "HC/SGCS/UBD/2025/0009",
        "old_requested": Decimal("29530.00"), "requested": Decimal("29530.00"),
        "old_approved": Decimal("29530.00"), "approved": Decimal("29530.00"),
        "old_category": "OTHER", "category": "OTHER", "quarter": 81,
        "source": "SGCS-OUTDOOR-000283+000302",
    },
    382: {
        "epf": "195", "request_id": "HC/SGCS/UBD/2025/0010",
        "old_requested": Decimal("10000.00"), "requested": Decimal("10000.00"),
        "old_approved": Decimal("10000.00"), "approved": Decimal("10000.00"),
        "old_category": "OTHER", "category": "DENTAL", "quarter": 84,
        "source": "SGCS-OUTDOOR-000013;TC/HC/SGCS/MM/DENTAL/001",
    },
    383: {
        "epf": "252", "request_id": "HC/SGCS/UBD/2025/0011",
        "old_requested": Decimal("15940.00"), "requested": Decimal("12940.00"),
        "old_approved": Decimal("15940.00"), "approved": Decimal("12940.00"),
        "old_category": "OTHER", "category": "OTHER", "quarter": 81,
        "source": "SGCS-OUTDOOR-000287+000288+000293+000298+000301;OTHER-REMAINDER",
    },
    426: {
        "epf": "252", "request_id": "HC/SGCS/MM/2025/0004",
        "old_requested": Decimal("1353.58"), "requested": Decimal("1353.58"),
        "old_approved": Decimal("1000.00"), "approved": Decimal("1000.00"),
        "old_category": "OTHER", "category": "OTHER", "quarter": 81,
        "source": "SGCS-OUTDOOR-000312;TC/HC/SGCS/MM/OTHER/036",
    },
    427: {
        "epf": "252", "request_id": "HC/SGCS/MM/2025/0005",
        "old_requested": Decimal("7640.00"), "requested": Decimal("7640.00"),
        "old_approved": Decimal("5740.00"), "approved": Decimal("5740.00"),
        "old_category": "OTHER", "category": "OTHER", "quarter": 81,
        "source": "SGCS-OUTDOOR-000313;TC/HC/SGCS/MM/OTHER/037",
    },
    428: {
        "epf": "252", "request_id": "HC/SGCS/MM/2025/0006",
        "old_requested": Decimal("3000.00"), "requested": Decimal("3000.00"),
        "old_approved": Decimal("3000.00"), "approved": Decimal("3000.00"),
        "old_category": "OTHER", "category": "OTHER", "quarter": 81,
        "source": "SGCS-OUTDOOR-000311;TC/HC/SGCS/MM/OTHER/035",
    },
    455: {
        "epf": "233", "request_id": "HC/SGCS/MM/2026/0001",
        "old_requested": Decimal("28831.14"), "requested": Decimal("28831.14"),
        "old_approved": Decimal("14900.00"), "approved": Decimal("14900.00"),
        "old_category": "OTHER", "category": "OTHER", "quarter": 81,
        "source": "SGCS-OUTDOOR-000325;TC/HC/SGCS/MM/OTHER/038",
    },
    473: {
        "epf": "252", "request_id": "HC/SGCS/MM/2026/0003",
        "old_requested": Decimal("2000.00"), "requested": Decimal("2000.00"),
        "old_approved": Decimal("2000.00"), "approved": Decimal("2000.00"),
        "old_category": "OTHER", "category": "OTHER", "quarter": 81,
        "source": "SGCS-OUTDOOR-000328;TC/HC/SGCS/MM/OTHER/039",
    },
    474: {
        "epf": "252", "request_id": "HC/SGCS/MM/2026/0004",
        "old_requested": Decimal("1900.00"), "requested": Decimal("1900.00"),
        "old_approved": Decimal("1900.00"), "approved": Decimal("1900.00"),
        "old_category": "OTHER", "category": "OTHER", "quarter": 81,
        "source": "SGCS-OUTDOOR-000329;TC/HC/SGCS/MM/OTHER/040",
    },
}

NEW = (
    {
        "epf": "252", "request_id": "TC/HC/SGCS/MM/DENTAL/002",
        "requested": Decimal("3000.00"), "approved": Decimal("3000.00"),
        "submitted": datetime(2025, 10, 22, 13, 12),
        "category": "DENTAL", "quarter": 84,
        "source": "SGCS-OUTDOOR-000017;SPLIT-FROM-LEGACY-CLAIM-383",
    },
    {
        "epf": "181", "request_id": "TC/HC/SGCS/MM/OTHER/041",
        "requested": Decimal("2809.10"), "approved": Decimal("1690.00"),
        "submitted": datetime(2026, 7, 15, 13, 44),
        "category": "OTHER", "quarter": 81,
        "source": "SGCS-OUTDOOR-000361;CONFIRMED-APPROVED-1690;SOURCE-DATE-AFTER-POLICY",
    },
)


def one(cur, sql: str, params=()):
    cur.execute(sql, params)
    row = cur.fetchone()
    if row is None:
        raise RuntimeError("Expected a row but query returned none")
    return row


def tagged(remark: str | None, source: str) -> str:
    original = (remark or "").strip()
    if TAG in original:
        return original
    value = f"{original} {TAG} SOURCE:{source}".strip()
    if len(value) > 255:
        raise RuntimeError(f"Tagged remark exceeds 255 characters: {value}")
    return value


def validate_configuration(cur) -> None:
    database = one(cur, "SELECT DATABASE() db")["db"]
    if database != "sgcs_care":
        raise RuntimeError(f"Refusing database {database!r}; expected live 'sgcs_care'")
    row = one(
        cur,
        """
        SELECT id,global_limit,insurance_policy,treatment,status,
               insurance_staff_category_period
        FROM insurance_details_limit WHERE id=%s
        """,
        (LIMIT_ID,),
    )
    actual = (
        row["global_limit"], row["insurance_policy"], row["treatment"],
        row["status"], row["insurance_staff_category_period"],
    )
    expected = (GLOBAL_LIMIT, "P-MIDDLE", "OUTDOOR", "ACTIVE", PERIOD_ID)
    if actual != expected:
        raise RuntimeError(f"MM OUTDOOR limit configuration changed: {actual}")

    cur.execute(
        """
        SELECT id,insurance_details_id,treatment_category_code,quarter_limit
        FROM insurance_quarter WHERE id IN (81,84) ORDER BY id
        """
    )
    quarters = {
        int(row["id"]): (
            int(row["insurance_details_id"]), row["treatment_category_code"],
            row["quarter_limit"],
        )
        for row in cur.fetchall()
    }
    expected_quarters = {
        81: (LIMIT_ID, "OTHER", Decimal("30000.00")),
        84: (LIMIT_ID, "DENTAL", Decimal("10000.00")),
    }
    if quarters != expected_quarters:
        raise RuntimeError(f"MM quarter configuration changed: {quarters}")


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
    invalid = {
        epf: rows
        for epf, rows in grouped.items()
        if len(rows) != 1
        or rows[0]["user_status"] != "ACTIVE"
        or rows[0]["company_type"] != "SGCS"
        or rows[0]["staff_category"] != "MM"
        or rows[0]["insurance_policy"] != "P-MIDDLE"
    }
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
    for claim_id, spec in EXISTING.items():
        row = claim_row(cur, claim_id)
        common = (
            str(row["epf_no"]), row["request_id"], row["request_status"],
            row["insurance_details_limit_id"], row["treatment"],
            row["insurance_staff_category_period"],
        )
        expected_common = (
            spec["epf"], spec["request_id"], "APPROVED", LIMIT_ID,
            "OUTDOOR", PERIOD_ID,
        )
        if common != expected_common:
            raise RuntimeError(f"Claim {claim_id} changed after preflight: {common}")

        before_financial = (
            spec["old_requested"], spec["old_approved"], spec["old_category"],
        )
        after_financial = (
            spec["requested"], spec["approved"], spec["category"],
        )
        current_financial = (
            row["request_amount"], row["approved_amount"],
            row["treatment_category"],
        )
        if current_financial not in {before_financial, after_financial}:
            raise RuntimeError(
                f"Claim {claim_id} financial/category state changed: {current_financial}"
            )
        if row["insurance_quarter_id"] not in {None, spec["quarter"]}:
            raise RuntimeError(
                f"Claim {claim_id} has unexpected quarter {row['insurance_quarter_id']}"
            )
        final = (
            current_financial == after_financial
            and row["insurance_quarter_id"] == spec["quarter"]
            and TAG in (row["remark"] or "")
        )
        if final:
            continue

        cur.execute(
            """
            UPDATE claims_request
            SET request_amount=%s,approved_amount=%s,insurance_quarter_id=%s,
                remark=%s,last_modified_date=CURRENT_TIMESTAMP
            WHERE id=%s
            """,
            (
                spec["requested"], spec["approved"], spec["quarter"],
                tagged(row["remark"], spec["source"]), claim_id,
            ),
        )
        if cur.rowcount != 1:
            raise RuntimeError(f"Claim {claim_id} reconciliation failed")
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
                raise RuntimeError(f"Claim {claim_id} category correction failed")
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
            LIMIT_ID, spec["quarter"], "OUTDOOR", spec["category"], PERIOD_ID,
        )
        if actual != expected or TAG not in (row["remark"] or ""):
            raise RuntimeError(
                f"Request ID {spec['request_id']} is not owned by this migration: {actual}"
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
            spec["category"], PERIOD_ID,
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
            spec["submitted"], spec["submitted"],
            tagged("Approved legacy claim reconciliation", spec["source"]),
            spec["requested"], spec["request_id"], users[spec["epf"]],
            details_id, LIMIT_ID, spec["quarter"], spec["approved"],
        ),
    )
    return int(cur.lastrowid), True


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


def balances(totals: dict[str, dict[str, Decimal]]) -> dict[str, Decimal]:
    return {
        epf: GLOBAL_LIMIT - sum(categories.values(), Decimal("0.00"))
        for epf, categories in totals.items()
    }


def validate_final(cur) -> None:
    for claim_id, spec in EXISTING.items():
        row = claim_row(cur, claim_id)
        if (
            row["request_amount"] != spec["requested"]
            or row["approved_amount"] != spec["approved"]
            or row["treatment_category"] != spec["category"]
            or row["insurance_quarter_id"] != spec["quarter"]
            or TAG not in (row["remark"] or "")
        ):
            raise RuntimeError(f"Claim {claim_id} did not reach its final state")


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
                new_rows = [insert_claim(cur, users, spec) for spec in NEW]
                after = fetch_final(cur)
                after_balances = balances(after)
                validate_final(cur)

                if after != EXPECTED_FINAL:
                    raise RuntimeError(
                        f"Final category totals mismatch: expected {EXPECTED_FINAL}, found {after}"
                    )
                if any(value < 0 for value in after_balances.values()):
                    raise RuntimeError(f"A final balance is negative: {after_balances}")
                cur.execute(
                    "SELECT COUNT(*) n FROM claims_request WHERE remark LIKE %s",
                    (f"%{TAG}%",),
                )
                marker_count = int(cur.fetchone()["n"])
                expected_markers = len(EXISTING) + len(NEW)
                if marker_count != expected_markers:
                    raise RuntimeError(
                        f"Expected {expected_markers} migration markers, found {marker_count}"
                    )

                print("Existing claims reconciled:", reconciled)
                print("New/idempotent claim IDs:", new_rows)
                print("Final totals:", after)
                print("Final balances:", after_balances)
                if args.apply:
                    connection.commit()
                    print(
                        f"COMMITTED: marker={TAG}, tagged_claims={marker_count}"
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
