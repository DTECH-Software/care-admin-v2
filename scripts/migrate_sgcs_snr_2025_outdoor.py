"""Reconcile SGCS Senior Staff 2025-2026 OUTDOOR claims.

The migration is idempotent and transactional. Dry-run is the default. Pass
--apply only after every live precondition and final-balance assertion passes.
"""

from __future__ import annotations

import argparse
from contextlib import closing
from datetime import datetime
from decimal import Decimal

from wecare_spectacle import _connect


TAG = "[MIGRATION:SGCS-SNR-2025-OUTDOOR-R1]"
LIMIT_ID = 13
PERIOD_ID = 7
GLOBAL_LIMIT = Decimal("80000.00")

EXPECTED_FINAL = {
    "14": {"DENTAL": Decimal("15000.00"), "OTHER": Decimal("55279.00")},
    "29": {"OTHER": Decimal("24816.00")},
    "48": {"OTHER": Decimal("6149.00"), "SPECTACLE": Decimal("20000.00")},
    "110": {"OTHER": Decimal("63917.00")},
    "123": {"OTHER": Decimal("25134.00")},
    "134": {"OTHER": Decimal("51319.00")},
    "197": {"OTHER": Decimal("8068.00")},
    "219": {"OTHER": Decimal("31053.00")},
    "251": {"DENTAL": Decimal("7000.00"), "OTHER": Decimal("7000.00")},
}


def spec(
    epf: str,
    request_id: str,
    requested: str,
    approved: str,
    category: str = "OTHER",
    quarter: int = 86,
    source: str = "LIVE-MATCH",
    *,
    old_requested: str | None = None,
    old_approved: str | None = None,
    old_category: str | None = None,
    old_period: int = PERIOD_ID,
) -> dict:
    return {
        "epf": epf,
        "request_id": request_id,
        "requested": Decimal(requested),
        "approved": Decimal(approved),
        "category": category,
        "quarter": quarter,
        "source": source,
        "old_requested": Decimal(old_requested or requested),
        "old_approved": Decimal(old_approved or approved),
        "old_category": old_category or category,
        "old_period": old_period,
    }


EXISTING = {
    # EPF 14: live-only rows that exactly form the supplied balance sheet.
    403: spec("14", "HC/SGCS/SNR/2025/0007", "19000.00", "14240.00", source="BALANCE-SHEET-EPF14-14240"),
    404: spec("14", "HC/SGCS/SNR/2025/0008", "15000.00", "15000.00", "DENTAL", 89, "BALANCE-SHEET-EPF14-DENTAL-15000"),
    420: spec("14", "HC/SGCS/SNR/2025/0014", "22000.00", "14136.00", source="BALANCE-SHEET-EPF14-14136"),
    457: spec("14", "HC/SGCS/SNR/2026/0001", "36624.00", "26903.00", source="BALANCE-SHEET-EPF14-26903"),

    # EPF 29 workbook matches plus the two separately confirmed approvals.
    460: spec("29", "HC/SGCS/SNR/2026/0002", "7150.00", "7150.00", source="SGCS-OUTDOOR-000317"),
    461: spec("29", "HC/SGCS/SNR/2026/0003", "4233.42", "4233.00", source="SGCS-OUTDOOR-000318"),
    462: spec("29", "HC/SGCS/SNR/2026/0004", "2639.39", "2639.00", source="SGCS-OUTDOOR-000319"),
    467: spec("29", "HC/SGCS/SNR/2026/0006", "3554.00", "3554.00", source="SGCS-OUTDOOR-000323"),
    468: spec("29", "HC/SGCS/SNR/2026/0007", "1105.00", "600.00", source="SGCS-OUTDOOR-000324"),

    # EPF 48 aggregate is reduced to its OTHER component. SPECTACLE is inserted
    # separately so the category limits remain correct.
    373: spec(
        "48", "HC/SGCS/UBD/2025/0003", "6149.00", "6149.00",
        source="SGCS-OUTDOOR-000297;OTHER-COMPONENT",
        old_requested="26149.00", old_approved="26149.00",
    ),

    # EPF 110: first two workbook amounts are represented by aggregate 375.
    375: spec("110", "HC/SGCS/UBD/2025/0004", "24958.00", "24958.00", source="SGCS-OUTDOOR-000294+000295"),
    488: spec("110", "HC/SGCS/SNR/2026/0016", "12206.85", "9166.00", source="SGCS-OUTDOOR-000332"),
    507: spec("110", "HC/SGCS/SNR/2026/0028", "20971.65", "8181.00", source="SGCS-OUTDOOR-000359"),
    508: spec("110", "HC/SGCS/SNR/2026/0029", "4131.65", "4131.00", source="SGCS-OUTDOOR-000362"),
    512: spec("110", "HC/SGCS/SNR/2026/0033", "17481.65", "17481.00", source="SGCS-OUTDOOR-000366"),

    # EPF 123.
    376: spec("123", "HC/SGCS/UBD/2025/0005", "3500.00", "3500.00", source="SGCS-OUTDOOR-000291"),
    400: spec("123", "HC/SGCS/SNR/2025/0004", "8084.00", "8084.00", source="SGCS-OUTDOOR-000307"),
    401: spec("123", "HC/SGCS/SNR/2025/0005", "3800.00", "1150.00", source="SGCS-OUTDOOR-000308"),
    464: spec("123", "HC/SGCS/SNR/2026/0005", "2200.00", "2200.00", source="SGCS-OUTDOOR-000320"),
    471: spec("123", "HC/SGCS/SNR/2026/0008", "1800.00", "1800.00", source="SGCS-OUTDOOR-000327"),
    477: spec("123", "HC/SGCS/SNR/2026/0010", "1200.00", "1200.00", source="SGCS-OUTDOOR-000330"),
    485: spec("123", "HC/SGCS/SNR/2026/0015", "1000.00", "1000.00", source="SGCS-OUTDOOR-000331"),
    497: spec("123", "HC/SGCS/SNR/2026/0019", "1600.00", "1600.00", source="SGCS-OUTDOOR-000337"),
    498: spec("123", "HC/SGCS/SNR/2026/0020", "1800.00", "1800.00", source="SGCS-OUTDOOR-000338"),
    510: spec("123", "HC/SGCS/SNR/2026/0031", "1000.00", "1000.00", source="SGCS-OUTDOOR-000363"),
    511: spec("123", "HC/SGCS/SNR/2026/0032", "800.00", "800.00", source="SGCS-OUTDOOR-000364"),
    513: spec("123", "HC/SGCS/SNR/2026/0034", "1000.00", "1000.00", source="SGCS-OUTDOOR-000365"),

    # EPF 134.
    377: spec("134", "HC/SGCS/UBD/2025/0006", "27914.00", "27914.00", source="SGCS-OUTDOOR-000289"),
    490: spec("134", "HC/SGCS/SNR/2026/0017", "4624.00", "4349.00", source="SGCS-OUTDOOR-000334"),
    491: spec("134", "HC/SGCS/SNR/2026/0018", "9649.00", "9649.00", source="SGCS-OUTDOOR-000335"),
    501: spec("134", "HC/SGCS/SNR/2026/0023", "9407.00", "3949.00", source="SGCS-OUTDOOR-000349"),
    502: spec("134", "HC/SGCS/SNR/2026/0024", "5458.00", "5458.00", source="SGCS-OUTDOOR-000350"),

    # EPF 219: aggregate 449 has the wrong EX-OP1 period reference in live DB.
    449: spec(
        "219", "HC/SGCS/UBD/2025/0029", "6243.00", "6243.00",
        source="SGCS-OUTDOOR-000304+000305;PERIOD-CORRECTION",
        old_period=4,
    ),
    499: spec("219", "HC/SGCS/SNR/2026/0021", "7600.00", "7600.00", source="SGCS-OUTDOOR-000343"),
    500: spec("219", "HC/SGCS/SNR/2026/0022", "1647.92", "1647.00", source="SGCS-OUTDOOR-000344"),
    506: spec("219", "HC/SGCS/SNR/2026/0027", "22763.00", "15563.00", source="SGCS-OUTDOOR-000358"),

    # EPF 251 has one OTHER and one DENTAL approved row already in live DB.
    408: spec("251", "HC/SGCS/SNR/2025/0010", "10040.00", "7000.00", source="SGCS-OUTDOOR-000310"),
    414: spec("251", "HC/SGCS/SNR/2025/0012", "7000.00", "7000.00", "DENTAL", 89, "SGCS-OUTDOOR-000020"),
}

CONVERSIONS = {
    515: {
        "epf": "29", "request_id": "HC/SGCS/SNR/2026/0036",
        "requested": Decimal("3640.00"), "approved": Decimal("3640.00"),
        "source": "BALANCE-SHEET-EPF29-3640;CONFIRMED-APPROVED",
    },
    516: {
        "epf": "29", "request_id": "HC/SGCS/SNR/2026/0037",
        "requested": Decimal("3000.00"), "approved": Decimal("3000.00"),
        "source": "BALANCE-SHEET-EPF29-3000;CONFIRMED-APPROVED",
    },
}

NEW = (
    {
        "epf": "48", "request_id": "TC/HC/SGCS/SNR/SPEC/003",
        "requested": Decimal("31300.00"), "approved": Decimal("20000.00"),
        "submitted": datetime(2025, 11, 7, 9, 53),
        "category": "SPECTACLE", "quarter": 90,
        "source": "SGCS-OUTDOOR-000026;SPLIT-FROM-LEGACY-CLAIM-373",
    },
    {
        "epf": "197", "request_id": "TC/HC/SGCS/SNR/OTHER/150",
        "requested": Decimal("7205.00"), "approved": Decimal("5684.00"),
        "submitted": datetime(2025, 11, 10, 10, 10),
        "category": "OTHER", "quarter": 86,
        "source": "SGCS-OUTDOOR-000303",
    },
    {
        "epf": "197", "request_id": "TC/HC/SGCS/SNR/OTHER/155",
        "requested": Decimal("2384.90"), "approved": Decimal("2384.00"),
        "submitted": datetime(2025, 11, 26, 13, 45),
        "category": "OTHER", "quarter": 86,
        "source": "SGCS-OUTDOOR-000309",
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
        SELECT global_limit,insurance_policy,treatment,status,
               insurance_staff_category_period
        FROM insurance_details_limit WHERE id=%s
        """,
        (LIMIT_ID,),
    )
    actual = (
        row["global_limit"], row["insurance_policy"], row["treatment"],
        row["status"], row["insurance_staff_category_period"],
    )
    expected = (GLOBAL_LIMIT, "P-SENIOR", "OUTDOOR", "ACTIVE", PERIOD_ID)
    if actual != expected:
        raise RuntimeError(f"SNR OUTDOOR limit configuration changed: {actual}")

    expected_quarters = {
        86: (LIMIT_ID, "OTHER", Decimal("80000.00")),
        89: (LIMIT_ID, "DENTAL", Decimal("15000.00")),
        90: (LIMIT_ID, "SPECTACLE", Decimal("20000.00")),
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
        raise RuntimeError(f"SNR quarter configuration changed: {found}")


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
        or rows[0]["staff_category"] != "SNR"
        or rows[0]["insurance_policy"] != "P-SENIOR"
    }
    if invalid:
        raise RuntimeError(f"Target employee/profile mismatch: {invalid}")
    return {epf: int(rows[0]["id"]) for epf, rows in grouped.items()}


def claim_row(cur, claim_id: int):
    return one(
        cur,
        """
        SELECT cr.id,upd.epf_no,cr.request_id,cr.request_status,
               cr.request_amount,cr.approved_amount,cr.approval_level,
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
    for claim_id, item in EXISTING.items():
        row = claim_row(cur, claim_id)
        common = (
            str(row["epf_no"]), row["request_id"], row["request_status"],
            row["insurance_details_limit_id"], row["treatment"],
        )
        wanted_common = (
            item["epf"], item["request_id"], "APPROVED", LIMIT_ID, "OUTDOOR",
        )
        if common != wanted_common:
            raise RuntimeError(f"Claim {claim_id} changed after preflight: {common}")
        current = (
            row["request_amount"], row["approved_amount"],
            row["treatment_category"], row["insurance_staff_category_period"],
        )
        before = (
            item["old_requested"], item["old_approved"],
            item["old_category"], item["old_period"],
        )
        after = (
            item["requested"], item["approved"], item["category"], PERIOD_ID,
        )
        if current not in {before, after}:
            raise RuntimeError(f"Claim {claim_id} data changed: {current}")
        if row["insurance_quarter_id"] not in {None, item["quarter"]}:
            raise RuntimeError(
                f"Claim {claim_id} has unexpected quarter {row['insurance_quarter_id']}"
            )
        final = (
            current == after
            and row["insurance_quarter_id"] == item["quarter"]
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
                item["requested"], item["approved"], item["quarter"],
                tagged(row["remark"], item["source"]), claim_id,
            ),
        )
        if cur.rowcount != 1:
            raise RuntimeError(f"Claim {claim_id} reconciliation failed")
        if (
            row["treatment_category"] != item["category"]
            or row["insurance_staff_category_period"] != PERIOD_ID
        ):
            cur.execute(
                """
                UPDATE insurance_claims_details
                SET treatment_category=%s,insurance_staff_category_period=%s,
                    last_modified_date=CURRENT_TIMESTAMP
                WHERE id=%s
                """,
                (item["category"], PERIOD_ID, row["insurance_claims_details"]),
            )
            if cur.rowcount != 1:
                raise RuntimeError(f"Claim {claim_id} details correction failed")
        changed.append(claim_id)
    return changed


def update_workflow(cur, claim_id: int, amount: Decimal) -> None:
    cur.execute(
        """
        UPDATE approval_work_flow aw
        JOIN insurance_claim_approval_work_flow link
          ON link.approval_work_flow_id=aw.id
        SET aw.status='APPROVED',aw.approved_amount=%s,
            aw.approved_user='MIGRATION_SNR25_OUTDOOR',
            aw.approved_date=CURRENT_TIMESTAMP,
            aw.last_modified_date=CURRENT_TIMESTAMP
        WHERE link.insurance_claim_id=%s AND aw.status='UNDER_REVIEW'
        """,
        (amount, claim_id),
    )
    if cur.rowcount != 1:
        raise RuntimeError(
            f"Expected one UNDER_REVIEW workflow for claim {claim_id}; updated {cur.rowcount}"
        )


def convert_under_review(cur) -> list[int]:
    changed = []
    for claim_id, item in CONVERSIONS.items():
        row = claim_row(cur, claim_id)
        common = (
            str(row["epf_no"]), row["request_id"], row["request_amount"],
            row["insurance_details_limit_id"], row["treatment"],
            row["treatment_category"], row["insurance_staff_category_period"],
        )
        wanted = (
            item["epf"], item["request_id"], item["requested"], LIMIT_ID,
            "OUTDOOR", "OTHER", PERIOD_ID,
        )
        if common != wanted:
            raise RuntimeError(f"Claim {claim_id} changed after preflight: {common}")
        if row["request_status"] == "APPROVED":
            if (
                row["approved_amount"] != item["approved"]
                or row["insurance_quarter_id"] != 86
                or TAG not in (row["remark"] or "")
            ):
                raise RuntimeError(f"Claim {claim_id} has a partial final state")
            continue
        if row["request_status"] != "UNDER_REVIEW" or row["approved_amount"] is not None:
            raise RuntimeError(f"Claim {claim_id} is no longer safely convertible")
        cur.execute(
            """
            UPDATE claims_request
            SET request_status='APPROVED',approved_amount=%s,
                approval_level='LEVEL02',insurance_quarter_id=86,
                remark=%s,last_modified_date=CURRENT_TIMESTAMP
            WHERE id=%s AND request_status='UNDER_REVIEW' AND approved_amount IS NULL
            """,
            (item["approved"], tagged(row["remark"], item["source"]), claim_id),
        )
        if cur.rowcount != 1:
            raise RuntimeError(f"Claim {claim_id} conversion failed")
        update_workflow(cur, claim_id, item["approved"])
        changed.append(claim_id)
    return changed


def insert_claim(cur, users: dict[str, int], item: dict) -> tuple[int, bool]:
    cur.execute(
        "SELECT id FROM claims_request WHERE request_id=%s FOR UPDATE",
        (item["request_id"],),
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
            item["epf"], "APPROVED", item["requested"], item["approved"],
            LIMIT_ID, item["quarter"], "OUTDOOR", item["category"], PERIOD_ID,
        )
        if actual != expected or TAG not in (row["remark"] or ""):
            raise RuntimeError(
                f"Request ID {item['request_id']} is not owned by this migration: {actual}"
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
            item["submitted"], item["submitted"], item["submitted"].date(),
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
            item["submitted"], item["submitted"],
            tagged("Approved legacy claim reconciliation", item["source"]),
            item["requested"], item["request_id"], users[item["epf"]],
            details_id, LIMIT_ID, item["quarter"], item["approved"],
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
    for claim_id, item in EXISTING.items():
        row = claim_row(cur, claim_id)
        if (
            row["request_amount"] != item["requested"]
            or row["approved_amount"] != item["approved"]
            or row["treatment_category"] != item["category"]
            or row["insurance_staff_category_period"] != PERIOD_ID
            or row["insurance_quarter_id"] != item["quarter"]
            or TAG not in (row["remark"] or "")
        ):
            raise RuntimeError(f"Claim {claim_id} did not reach its final state")
    for claim_id, item in CONVERSIONS.items():
        row = claim_row(cur, claim_id)
        if (
            row["request_status"] != "APPROVED"
            or row["approved_amount"] != item["approved"]
            or row["insurance_quarter_id"] != 86
            or TAG not in (row["remark"] or "")
        ):
            raise RuntimeError(f"Converted claim {claim_id} did not reach its final state")


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
                converted = convert_under_review(cur)
                new_rows = [insert_claim(cur, users, item) for item in NEW]
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
                expected_markers = len(EXISTING) + len(CONVERSIONS) + len(NEW)
                if marker_count != expected_markers:
                    raise RuntimeError(
                        f"Expected {expected_markers} migration markers, found {marker_count}"
                    )

                print("Existing claims reconciled:", reconciled)
                print("UNDER_REVIEW claims approved:", converted)
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
