"""Hard-delete the five confirmed live profiles matched by four NIC values.

This is a guarded, one-purpose cleanup. It verifies exact identities and owned
dependencies, performs deletion in foreign-key order, and validates that no
target operational rows remain. Dry-run is the default; --apply commits.

Shared policy, beneficiary, master-data and audit-log rows are never deleted.
"""

from __future__ import annotations

import argparse
from contextlib import closing

from wecare_spectacle import _connect


TARGET_NICS = (
    "200073600733",
    "200056701804",
    "200168802653",
    "960190084V",
)

EXPECTED_PROFILES = {
    # personal_id: (NIC, EPF, status, company_id, address_id, application_user_id)
    305: ("200073600733", "12", "ACTIVE", 340, 340, 140),
    310: ("200056701804", "2005", "INACTIVE", 345, 345, None),
    311: ("200056701804", "2500", "INACTIVE", 346, 346, None),
    313: ("200168802653", "1001", "ACTIVE", 348, 348, 163),
    314: ("960190084V", "1500", "ACTIVE", 349, 349, 165),
}

EXPECTED_APPS = {
    140: ("Asm@123", 305, 142, 1196),
    163: ("Amila*123", 313, 165, 1085),
    165: ("Tharu@123", 314, 167, None),
}

EXPECTED_CLAIMS = {
    435, 436, 437, 438, 439, 440, 441,
    442, 443, 444, 450, 451, 453, 486,
}
EXPECTED_DETAILS = {
    429, 430, 431, 432, 433, 434, 435,
    436, 437, 438, 442, 443, 445, 478,
}
EXPECTED_DEPENDENTS = {322, 325, 326}
EXPECTED_DEATH_CLAIMS = {59, 60, 61}
EXPECTED_WORKFLOWS = {
    577, 578, 612, 613, 614, 615, 616, 617, 618, 619,
    620, 621, 622, 623, 624, 625, 626, 627, 628, 629,
    630, 631, 632, 633, 641, 642, 643, 644, 645, 647,
    648, 664, 665, 668, 715,
}
EXPECTED_DOCUMENTS = {
    171, 172, 229, 230, 233, 234, 235, 236, 237, 238,
    239, 240, 250, 251, 260, 302, 305, 306, 307, 361,
}
EXPECTED_ONBOARDING = {142, 165, 167}
EXPECTED_OTP_SESSIONS = {1085, 1196}
EXPECTED_ADDRESSES = {340, 345, 346, 348, 349}
EXPECTED_COMPANIES = {340, 345, 346, 348, 349}


def placeholders(values) -> str:
    return ",".join(["%s"] * len(values))


def fetch_ids(cur, table: str, column: str, values, id_column: str = "id") -> set[int]:
    values = tuple(values)
    if not values:
        return set()
    cur.execute(
        f"SELECT `{id_column}` value FROM `{table}` "
        f"WHERE `{column}` IN ({placeholders(values)}) FOR UPDATE",
        values,
    )
    return {int(row["value"]) for row in cur.fetchall()}


def count_rows(cur, table: str, column: str, values) -> int:
    values = tuple(values)
    if not values:
        return 0
    cur.execute(
        f"SELECT COUNT(*) n FROM `{table}` WHERE `{column}` IN ({placeholders(values)})",
        values,
    )
    return int(cur.fetchone()["n"])


def delete_rows(cur, table: str, column: str, values, expected: int) -> None:
    values = tuple(values)
    if not values:
        if expected:
            raise RuntimeError(f"No values supplied for expected delete from {table}")
        return
    cur.execute(
        f"DELETE FROM `{table}` WHERE `{column}` IN ({placeholders(values)})",
        values,
    )
    if cur.rowcount != expected:
        raise RuntimeError(
            f"Delete count mismatch for {table}: expected {expected}, got {cur.rowcount}"
        )
    print(f"Deleted {table}: {cur.rowcount}")


def validate_profiles(cur) -> None:
    nics = tuple(nic.upper() for nic in TARGET_NICS)
    cur.execute(
        f"""
        SELECT upd.id personal_id,UPPER(REPLACE(upd.nic,' ','')) nic,
               CAST(upd.epf_no AS CHAR) epf_no,upd.user_status,
               upd.user_company_details company_id,upd.user_address address_id,
               au.id application_user_id
        FROM user_personal_details upd
        LEFT JOIN application_user au ON au.user_personal_details=upd.id
        WHERE UPPER(REPLACE(upd.nic,' ','')) IN ({placeholders(nics)})
        ORDER BY upd.id,au.id
        FOR UPDATE
        """,
        nics,
    )
    actual = {}
    for row in cur.fetchall():
        personal_id = int(row["personal_id"])
        if personal_id in actual:
            raise RuntimeError(f"Multiple application users for personal ID {personal_id}")
        actual[personal_id] = (
            row["nic"], row["epf_no"], row["user_status"],
            row["company_id"], row["address_id"], row["application_user_id"],
        )
    if actual != EXPECTED_PROFILES:
        raise RuntimeError(f"Target profiles changed after confirmation: {actual}")

    app_ids = tuple(EXPECTED_APPS)
    cur.execute(
        f"""
        SELECT id,username,user_personal_details,onboarding_request,otp_session
        FROM application_user WHERE id IN ({placeholders(app_ids)})
        ORDER BY id FOR UPDATE
        """,
        app_ids,
    )
    actual_apps = {
        int(row["id"]): (
            row["username"], row["user_personal_details"],
            row["onboarding_request"], row["otp_session"],
        )
        for row in cur.fetchall()
    }
    if actual_apps != EXPECTED_APPS:
        raise RuntimeError(f"Target application users changed: {actual_apps}")

    # Each address and company row must be owned by exactly one target profile.
    if count_rows(cur, "user_personal_details", "user_address", EXPECTED_ADDRESSES) != 5:
        raise RuntimeError("One or more target address rows are shared or missing")
    if count_rows(cur, "user_personal_details", "user_company_details", EXPECTED_COMPANIES) != 5:
        raise RuntimeError("One or more target company rows are shared or missing")


def validate_dependencies(cur) -> None:
    app_ids = tuple(EXPECTED_APPS)
    claim_ids = fetch_ids(cur, "claims_request", "employee", app_ids)
    if claim_ids != EXPECTED_CLAIMS:
        raise RuntimeError(f"Medical claim set changed: {claim_ids}")

    cur.execute(
        f"""
        SELECT insurance_claims_details detail_id,approval_work_flow_id
        FROM claims_request WHERE id IN ({placeholders(claim_ids)}) FOR UPDATE
        """,
        tuple(claim_ids),
    )
    claim_rows = cur.fetchall()
    detail_ids = {int(row["detail_id"]) for row in claim_rows}
    if detail_ids != EXPECTED_DETAILS:
        raise RuntimeError(f"Claim detail set changed: {detail_ids}")
    if any(row["approval_work_flow_id"] is not None for row in claim_rows):
        raise RuntimeError("A target claim gained a direct workflow reference")

    dependents = fetch_ids(cur, "claims_dependents", "application_user", app_ids)
    if dependents != EXPECTED_DEPENDENTS:
        raise RuntimeError(f"Dependent set changed: {dependents}")

    death_claims = fetch_ids(cur, "death_claim_request", "employee", app_ids)
    if death_claims != EXPECTED_DEATH_CLAIMS:
        raise RuntimeError(f"Death claim set changed: {death_claims}")

    cur.execute(
        f"""
        SELECT approval_work_flow_id workflow_id
        FROM insurance_claim_approval_work_flow
        WHERE insurance_claim_id IN ({placeholders(claim_ids)}) FOR UPDATE
        """,
        tuple(claim_ids),
    )
    workflows = {int(row["workflow_id"]) for row in cur.fetchall()}
    cur.execute(
        f"""
        SELECT approval_work_flow_id workflow_id
        FROM death_claim_approval_work_flow
        WHERE death_claim_id IN ({placeholders(death_claims)}) FOR UPDATE
        """,
        tuple(death_claims),
    )
    workflows.update(int(row["workflow_id"]) for row in cur.fetchall())
    if workflows != EXPECTED_WORKFLOWS:
        raise RuntimeError(f"Workflow set changed: {workflows}")

    # None of these workflow rows may be linked to non-target claims.
    if count_rows(cur, "insurance_claim_approval_work_flow", "approval_work_flow_id", workflows) != 30:
        raise RuntimeError("A medical workflow is missing or shared")
    if count_rows(cur, "death_claim_approval_work_flow", "approval_work_flow_id", workflows) != 5:
        raise RuntimeError("A death workflow is missing or shared")
    if count_rows(cur, "claims_request", "approval_work_flow_id", workflows) != 0:
        raise RuntimeError("A target workflow is directly referenced by another claim")

    if count_rows(cur, "approval_workflow_reject_reason", "approval_workflow_id", workflows) != 0:
        raise RuntimeError("Unexpected workflow reject-reason rows require review")
    if count_rows(cur, "payment_attachment_claim", "insurance_claim_id", claim_ids) != 0:
        raise RuntimeError("Unexpected medical payment attachments require review")
    if count_rows(cur, "third_party_indoor_claim_batch_row", "insurance_claim_id", claim_ids) != 0:
        raise RuntimeError("Unexpected third-party batch links require review")
    if count_rows(cur, "payment_advice_death_claim", "death_claim_id", death_claims) != 0:
        raise RuntimeError("Unexpected death payment advice links require review")

    # The target dependents must be referenced only by the three target deaths.
    if count_rows(cur, "claims_request", "dependent", dependents) != 0:
        raise RuntimeError("A target dependent is used by a medical claim")
    if count_rows(cur, "death_claim_request", "dependent", dependents) != 3:
        raise RuntimeError("Target dependent/death-claim mapping changed")

    document_ids = set()
    link_specs = (
        ("insurance_claims_details_document", "insurance_claims_details_id", detail_ids, 14),
        ("claims_dependents_document", "claims_dependents_id", dependents, 3),
        ("death_claims_document", "death_claims__id", death_claims, 3),
    )
    for table, column, ids, expected_count in link_specs:
        cur.execute(
            f"SELECT document_id FROM `{table}` WHERE `{column}` IN "
            f"({placeholders(ids)}) FOR UPDATE",
            tuple(ids),
        )
        rows = cur.fetchall()
        if len(rows) != expected_count:
            raise RuntimeError(f"Document link count changed for {table}: {len(rows)}")
        document_ids.update(int(row["document_id"]) for row in rows)
    if document_ids != EXPECTED_DOCUMENTS:
        raise RuntimeError(f"Document set changed: {document_ids}")

    # Verify these documents are not shared by anything outside target links.
    document_references = (
        ("insurance_claims_details_document", "document_id"),
        ("claims_dependents_document", "document_id"),
        ("death_claims_document", "document_id"),
        ("cheque_payment_ddf_document", "document_id"),
        ("cheque_payment_document", "document_id"),
        ("marital_status_update_document", "document_id"),
        ("support_ticket_attachment", "document_id"),
        ("application_user", "profile_img"),
        ("user_company_details", "transfer_doc"),
        ("user_personal_details", "birth_img"),
        ("web_user", "profile_img"),
    )
    reference_count = sum(
        count_rows(cur, table, column, document_ids)
        for table, column in document_references
    )
    if reference_count != 20:
        raise RuntimeError(f"Target documents are missing or shared; references={reference_count}")

    if fetch_ids(cur, "application_otp_sessions", "application_user_id", app_ids) != EXPECTED_OTP_SESSIONS:
        raise RuntimeError("Application OTP session set changed")
    if fetch_ids(cur, "onboarding_request", "id", EXPECTED_ONBOARDING) != EXPECTED_ONBOARDING:
        raise RuntimeError("Onboarding request set changed")
    if count_rows(cur, "application_user", "onboarding_request", EXPECTED_ONBOARDING) != 3:
        raise RuntimeError("An onboarding request is shared or missing")
    if count_rows(cur, "application_user", "otp_session", EXPECTED_OTP_SESSIONS) != 2:
        raise RuntimeError("An OTP session is shared or missing")
    if count_rows(cur, "onboadring_verified_mobile", "otp_session", EXPECTED_OTP_SESSIONS) != 0:
        raise RuntimeError("Unexpected verified-mobile OTP links require review")

    expected_direct = {
        ("application_password_history", "user_id"): 7,
        ("application_user_biometrics", "user_id"): 0,
        ("application_user_sessions", "user_id"): 3,
        ("claims_account_balance", "employee"): 0,
        ("notification_history", "employee"): 0,
    }
    for (table, column), expected in expected_direct.items():
        actual = count_rows(cur, table, column, app_ids)
        if actual != expected:
            raise RuntimeError(f"Dependency count changed for {table}: {actual}")


def perform_deletion(cur) -> None:
    app_ids = tuple(EXPECTED_APPS)

    # Claim and dependent documents, followed later by the owned document rows.
    delete_rows(cur, "insurance_claims_details_document", "insurance_claims_details_id", EXPECTED_DETAILS, 14)
    delete_rows(cur, "claims_dependents_document", "claims_dependents_id", EXPECTED_DEPENDENTS, 3)
    delete_rows(cur, "death_claims_document", "death_claims__id", EXPECTED_DEATH_CLAIMS, 3)

    # Workflow links must be removed before workflow and request rows.
    delete_rows(cur, "insurance_claim_approval_work_flow", "insurance_claim_id", EXPECTED_CLAIMS, 30)
    delete_rows(cur, "death_claim_approval_work_flow", "death_claim_id", EXPECTED_DEATH_CLAIMS, 5)
    delete_rows(cur, "approval_work_flow", "id", EXPECTED_WORKFLOWS, 35)

    # Operational request records and their private detail/dependent rows.
    delete_rows(cur, "death_claim_request", "id", EXPECTED_DEATH_CLAIMS, 3)
    delete_rows(cur, "claims_request", "id", EXPECTED_CLAIMS, 14)
    delete_rows(cur, "insurance_claims_details", "id", EXPECTED_DETAILS, 14)
    delete_rows(cur, "claims_dependents", "id", EXPECTED_DEPENDENTS, 3)
    delete_rows(cur, "document", "id", EXPECTED_DOCUMENTS, 20)

    # Authentication and application-account children.
    delete_rows(cur, "application_password_history", "user_id", app_ids, 7)
    delete_rows(cur, "application_user_sessions", "user_id", app_ids, 3)
    delete_rows(cur, "application_user_biometrics", "user_id", app_ids, 0)
    delete_rows(cur, "claims_account_balance", "employee", app_ids, 0)
    delete_rows(cur, "notification_history", "employee", app_ids, 0)
    delete_rows(cur, "application_user", "id", app_ids, 3)

    # These rows are parents of application_user and can be deleted afterward.
    delete_rows(cur, "application_otp_sessions", "id", EXPECTED_OTP_SESSIONS, 2)
    delete_rows(cur, "onboarding_request", "id", EXPECTED_ONBOARDING, 3)

    # Personal rows precede their privately owned address/company parents.
    delete_rows(cur, "user_personal_details", "id", EXPECTED_PROFILES, 5)
    delete_rows(cur, "user_address", "id", EXPECTED_ADDRESSES, 5)
    delete_rows(cur, "user_company_details", "id", EXPECTED_COMPANIES, 5)


def validate_removed(cur) -> None:
    checks = (
        ("user_personal_details", "id", EXPECTED_PROFILES),
        ("application_user", "id", EXPECTED_APPS),
        ("claims_request", "id", EXPECTED_CLAIMS),
        ("insurance_claims_details", "id", EXPECTED_DETAILS),
        ("claims_dependents", "id", EXPECTED_DEPENDENTS),
        ("death_claim_request", "id", EXPECTED_DEATH_CLAIMS),
        ("approval_work_flow", "id", EXPECTED_WORKFLOWS),
        ("document", "id", EXPECTED_DOCUMENTS),
        ("application_otp_sessions", "id", EXPECTED_OTP_SESSIONS),
        ("onboarding_request", "id", EXPECTED_ONBOARDING),
        ("user_address", "id", EXPECTED_ADDRESSES),
        ("user_company_details", "id", EXPECTED_COMPANIES),
    )
    for table, column, values in checks:
        remaining = count_rows(cur, table, column, values)
        if remaining:
            raise RuntimeError(f"Post-delete validation failed: {table} has {remaining} rows")

    nics = tuple(nic.upper() for nic in TARGET_NICS)
    cur.execute(
        f"""
        SELECT COUNT(*) n FROM user_personal_details
        WHERE UPPER(REPLACE(nic,' ','')) IN ({placeholders(nics)})
        """,
        nics,
    )
    if int(cur.fetchone()["n"]) != 0:
        raise RuntimeError("One or more target NIC profiles still exist")
    print("Post-delete validation: all target operational rows are absent")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true", help="commit permanent deletion")
    args = parser.parse_args()

    with closing(_connect()) as connection:
        connection.autocommit = False
        with closing(connection.cursor(dictionary=True)) as cur:
            try:
                database = cur.execute("SELECT DATABASE() db") or cur.fetchone()["db"]
                if database != "sgcs_care":
                    raise RuntimeError(f"Refusing database {database!r}; expected live 'sgcs_care'")
                print("Mode:", "APPLY" if args.apply else "DRY_RUN")
                validate_profiles(cur)
                validate_dependencies(cur)
                print("Preflight: exact identities and dependency ownership verified")
                perform_deletion(cur)
                validate_removed(cur)
                if args.apply:
                    connection.commit()
                    print("COMMITTED: five profiles and owned operational records permanently deleted")
                else:
                    connection.rollback()
                    print("DRY RUN PASSED; all deletions rolled back")
            except Exception:
                connection.rollback()
                raise
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
