"""Create EPF 263 and reconcile four confirmed NS 2026 OUTDOOR amounts.

Confirmed operations:
* EPF 162: add approved Rs. 641 (final Rs. 9,000 / balance zero).
* EPF 237: add approved Rs. 6,080 and Rs. 1,937 while preserving three
  existing UNDER_REVIEW claims (final approved Rs. 9,000 / balance zero).
* EPF 263: create the supplied employee master and application user, then add
  approved Rs. 3,000 (balance Rs. 6,000).

No source treatment dates were supplied. The policy start and first eligible
OTHER quarter are used and explicitly recorded in every claim audit marker.
Dry-run is the default; --apply commits the guarded transaction.
"""

from __future__ import annotations

import argparse
from contextlib import closing
from datetime import date, datetime
from decimal import Decimal

from wecare_application import (
    DEFAULT_LOGIN_STATUS,
    _build_username,
    _create_application_user,
)
from wecare_master import (
    INSERT_USER_ADDRESS_AUTO,
    INSERT_USER_COMPANY_AUTO,
    INSERT_USER_PERSONAL_AUTO,
    _build_address_values,
    _build_company_values,
    _build_personal_values,
)
from wecare_spectacle import _connect


TAG = "[MIGRATION:SGCS-NS-2026-OUTDOOR-R4]"
LIMIT_ID = 42
PERIOD_ID = 12
QUARTER_ID = 144
POLICY_START = date(2026, 1, 21)
POLICY_END = date(2027, 1, 20)
GLOBAL_LIMIT = Decimal("9000.00")

EMPLOYEE_263 = {
    "title": "MS",
    "initials": "K.K.O.C.",
    "first_name": "OSHADI",
    "last_name": "CHANODYA",
    "dob": "07/06/2001",
    "epf_no": "263",
    "marital_status": "Single",
    "contact_number": "0778739887",
    "nic": "200168802653",
    "gender": "Female",
    "email": "oshadichanodya@gmail.com",
    "designation": "DOCUMENTATION ASSISTANT",
    "terminate_date": None,
    "appointment_date": "03/01/2026",
    "company": "SGCS",
    "option": "NORMAL STAFF",
    "staff_category": "NORMAL STAFF",
    "staff_type": "PERMANENT",
    "city": "HOMAGAMA",
    "street_1": "MAGAMMANA",
    "street_2": None,
    "street_no": "NO. 207/2",
    "ddf": "YES",
    "medical": "YES",
}

CLAIMS = (
    {
        "epf": "162", "request_id": "TC/HC/SGCS/NS/2026/0021",
        "amount": Decimal("641.00"), "created": datetime(2026, 1, 21, 12, 2),
        "source": "BALANCE-SHEET-EPF162-641",
    },
    {
        "epf": "237", "request_id": "TC/HC/SGCS/NS/2026/0022",
        "amount": Decimal("6080.00"), "created": datetime(2026, 1, 21, 12, 3),
        "source": "BALANCE-SHEET-EPF237-6080",
    },
    {
        "epf": "237", "request_id": "TC/HC/SGCS/NS/2026/0023",
        "amount": Decimal("1937.00"), "created": datetime(2026, 1, 21, 12, 4),
        "source": "BALANCE-SHEET-EPF237-1937",
    },
    {
        "epf": "263", "request_id": "TC/HC/SGCS/NS/2026/0024",
        "amount": Decimal("3000.00"), "created": datetime(2026, 1, 21, 12, 5),
        "source": "BALANCE-SHEET-EPF263-3000",
    },
)

EXPECTED_BEFORE = {
    "162": Decimal("8359.00"),
    "237": Decimal("983.00"),
    "263": Decimal("0.00"),
}
EXPECTED_AFTER = {
    "162": Decimal("9000.00"),
    "237": Decimal("9000.00"),
    "263": Decimal("3000.00"),
}
EXPECTED_BALANCES = {
    "162": Decimal("0.00"),
    "237": Decimal("0.00"),
    "263": Decimal("6000.00"),
}
EXPECTED_UNDER_REVIEW_237 = {
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


def validate_configuration(cur) -> None:
    if one(cur, "SELECT DATABASE() db")["db"] != "sgcs_care":
        raise RuntimeError("Refusing to run outside live sgcs_care")

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
        limit_row["insurance_policy"],limit_row["treatment"],
        limit_row["global_limit"],limit_row["is_quarter"],limit_row["status"],
    )
    expected_limit = (PERIOD_ID,"P-NORMAL","OUTDOOR",GLOBAL_LIMIT,1,"ACTIVE")
    if actual_limit != expected_limit:
        raise RuntimeError(f"OUTDOOR limit configuration changed: {actual_limit}")

    period = one(
        cur,
        "SELECT staff_category,from_date,to_date,status FROM insurance_staff_category_period WHERE id=%s",
        (PERIOD_ID,),
    )
    if (period["staff_category"],period["from_date"],period["to_date"],period["status"]) != (
        "NS",POLICY_START,POLICY_END,"ACTIVE"
    ):
        raise RuntimeError(f"Normal Staff policy period changed: {period}")

    quarter = one(
        cur,
        """
        SELECT insurance_details_id,treatment_category_code,quarter_limit,
               from_date,to_date
        FROM insurance_quarter WHERE id=%s
        """,
        (QUARTER_ID,),
    )
    if (
        quarter["insurance_details_id"],quarter["treatment_category_code"],
        quarter["quarter_limit"],quarter["from_date"],quarter["to_date"],
    ) != (LIMIT_ID,"OTHER",GLOBAL_LIMIT,date(2026,1,21),date(2026,4,20)):
        raise RuntimeError(f"First OUTDOOR quarter changed: {quarter}")


def validate_existing_employees(cur) -> dict[str, int]:
    cur.execute(
        """
        SELECT upd.epf_no,au.id application_user_id,upd.user_status,
               ucd.company_type,ucd.staff_category,ucd.insurance_policy
        FROM user_personal_details upd
        JOIN user_company_details ucd ON ucd.id=upd.user_company_details
        JOIN application_user au ON au.user_personal_details=upd.id
        WHERE upd.epf_no IN ('162','237')
        ORDER BY upd.epf_no,au.id FOR UPDATE
        """
    )
    rows = cur.fetchall()
    if len(rows) != 2:
        raise RuntimeError(f"Expected exactly EPF 162 and 237 application users: {rows}")
    users = {}
    for row in rows:
        actual = (
            row["user_status"],row["company_type"],
            row["staff_category"],row["insurance_policy"],
        )
        if actual != ("ACTIVE","SGCS","NS","P-NORMAL"):
            raise RuntimeError(f"EPF {row['epf_no']} profile changed: {actual}")
        users[str(row["epf_no"])] = int(row["application_user_id"])
    if set(users) != {"162","237"}:
        raise RuntimeError(f"Existing employee set mismatch: {users}")
    return users


def validate_epf263_absent(cur) -> None:
    row = one(
        cur,
        """
        SELECT
          (SELECT COUNT(*) FROM user_personal_details WHERE epf_no='263') epf_rows,
          (SELECT COUNT(*) FROM user_personal_details
             WHERE UPPER(REPLACE(nic,' ',''))='200168802653') nic_rows,
          (SELECT COUNT(*) FROM user_personal_details
             WHERE LOWER(TRIM(email))='oshadichanodya@gmail.com') email_rows,
          (SELECT COUNT(*) FROM application_user
             WHERE LOWER(TRIM(username))='oshadi263sgcs') username_rows,
          (SELECT COUNT(*) FROM application_user
             WHERE LOWER(TRIM(primary_email))='oshadichanodya@gmail.com') app_email_rows,
          (SELECT COUNT(*) FROM application_user
             WHERE primary_mobile='0778739887') mobile_rows
        """,
    )
    if any(int(value) for value in row.values()):
        raise RuntimeError(f"EPF 263 identity is no longer free: {row}")


def create_epf263(cur) -> tuple[int,int,int,int,str]:
    now = datetime.now().replace(microsecond=0)
    # String dates are parsed directly, so this epoch value is not otherwise used.
    epoch = datetime(1899,12,30)

    cur.execute(INSERT_USER_ADDRESS_AUTO,_build_address_values(EMPLOYEE_263,now))
    address_id = int(cur.lastrowid)
    cur.execute(INSERT_USER_COMPANY_AUTO,_build_company_values(EMPLOYEE_263,now,epoch))
    company_id = int(cur.lastrowid)
    cur.execute(
        INSERT_USER_PERSONAL_AUTO,
        _build_personal_values(EMPLOYEE_263,now,epoch,address_id,company_id),
    )
    personal_id = int(cur.lastrowid)

    username = _build_username(
        EMPLOYEE_263["first_name"],EMPLOYEE_263["epf_no"],EMPLOYEE_263["company"]
    )
    if username != "OSHADI263SGCS":
        raise RuntimeError(f"Unexpected generated username: {username}")
    app_id = _create_application_user(
        cur,personal_id,username,EMPLOYEE_263["email"],
        EMPLOYEE_263["contact_number"],now,
    )
    return address_id,company_id,personal_id,int(app_id),username


def fetch_approved_totals(cur, users: dict[str,int]) -> dict[str,Decimal]:
    result = {}
    for epf,user_id in users.items():
        result[epf] = one(
            cur,
            """
            SELECT COALESCE(SUM(approved_amount),0) approved
            FROM claims_request WHERE employee=%s
              AND insurance_details_limit_id=%s AND request_status='APPROVED'
            """,
            (user_id,LIMIT_ID),
        )["approved"]
    return result


def validate_existing_claims(cur,users: dict[str,int]) -> None:
    row162 = one(
        cur,
        """
        SELECT id,request_status,approved_amount,insurance_details_limit_id
        FROM claims_request WHERE id=664 AND employee=%s FOR UPDATE
        """,
        (users["162"],),
    )
    if tuple(row162.values()) != (664,"APPROVED",Decimal("8359.00"),LIMIT_ID):
        raise RuntimeError(f"EPF 162 base claim changed: {row162}")

    row237 = one(
        cur,
        """
        SELECT id,request_status,approved_amount,insurance_details_limit_id
        FROM claims_request WHERE id=688 AND employee=%s FOR UPDATE
        """,
        (users["237"],),
    )
    if tuple(row237.values()) != (688,"APPROVED",Decimal("983.00"),LIMIT_ID):
        raise RuntimeError(f"EPF 237 Rs. 983 claim changed: {row237}")

    ids = tuple(EXPECTED_UNDER_REVIEW_237)
    cur.execute(
        f"""
        SELECT id,request_status,request_amount,approved_amount,
               insurance_details_limit_id
        FROM claims_request WHERE employee=%s
          AND id IN ({','.join(['%s']*len(ids))}) ORDER BY id FOR UPDATE
        """,
        (users["237"],*ids),
    )
    rows = {int(row["id"]):row for row in cur.fetchall()}
    if set(rows) != set(ids):
        raise RuntimeError(f"EPF 237 under-review claim set changed: {rows}")
    for claim_id,amount in EXPECTED_UNDER_REVIEW_237.items():
        row=rows[claim_id]
        actual=(row["request_status"],row["request_amount"],row["approved_amount"],row["insurance_details_limit_id"])
        if actual != ("UNDER_REVIEW",amount,None,LIMIT_ID):
            raise RuntimeError(f"EPF 237 under-review claim {claim_id} changed: {actual}")


def insert_claim(cur,user_id:int,spec:dict) -> int:
    cur.execute(
        "SELECT id,employee,approved_amount,remark FROM claims_request WHERE request_id=%s FOR UPDATE",
        (spec["request_id"],),
    )
    existing=cur.fetchone()
    if existing:
        if existing["employee"]!=user_id or existing["approved_amount"]!=spec["amount"] or TAG not in (existing["remark"] or ""):
            raise RuntimeError(f"Request ID collision: {existing}")
        return int(existing["id"])

    cur.execute(
        """
        INSERT INTO insurance_claims_details
          (created_date,last_modified_date,disease,from_treatment_date,
           to_treatment_date,treatment,treatment_category,
           insurance_staff_category_period)
        VALUES (%s,%s,'The Care Data Migration',NULL,%s,'OUTDOOR','OTHER',%s)
        """,
        (spec["created"],spec["created"],POLICY_START,PERIOD_ID),
    )
    details_id=int(cur.lastrowid)
    remark=(
        f"Approved legacy balance reconciliation {TAG} SOURCE:{spec['source']};"
        "SOURCE-DATE-NOT-PROVIDED;POLICY-START-USED"
    )
    if len(remark)>255:
        raise RuntimeError("Claim audit remark exceeds 255 characters")
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
            spec["created"],spec["created"],remark,spec["amount"],
            spec["request_id"],user_id,details_id,LIMIT_ID,QUARTER_ID,spec["amount"],
        ),
    )
    return int(cur.lastrowid)


def validate_final(cur,users:dict[str,int],claim_ids:list[int],personal_id:int,company_id:int,username:str) -> None:
    totals=fetch_approved_totals(cur,users)
    balances={epf:GLOBAL_LIMIT-total for epf,total in totals.items()}
    if totals!=EXPECTED_AFTER or balances!=EXPECTED_BALANCES:
        raise RuntimeError(f"Final totals/balances mismatch: {totals} / {balances}")

    cur.execute(
        """
        SELECT upd.epf_no,upd.nic,upd.user_status,upd.dob,upd.email,upd.mobile_no,
               upd.title,upd.marital_status,upd.gender,
               ucd.company_type,ucd.insurance_policy,ucd.staff_category,
               ucd.staff_type,ucd.facility,DATE(ucd.permanent_date) permanent_date,
               au.id application_user_id,au.username,au.login_status
        FROM user_personal_details upd
        JOIN user_company_details ucd ON ucd.id=upd.user_company_details
        JOIN application_user au ON au.user_personal_details=upd.id
        WHERE upd.id=%s AND ucd.id=%s
        """,
        (personal_id,company_id),
    )
    profile=cur.fetchone()
    if profile is None:
        raise RuntimeError("EPF 263 profile was not created")
    expected_profile=(
        "263","200168802653","ACTIVE",date(2001,6,7),
        "oshadichanodya@gmail.com","0778739887","MS","SINGLE","FEMALE",
        "SGCS","P-NORMAL","NS","PER","BOTH",date(2026,1,3),
        users["263"],username,DEFAULT_LOGIN_STATUS,
    )
    actual_profile=tuple(profile.values())
    if actual_profile!=expected_profile:
        raise RuntimeError(f"EPF 263 final profile mismatch: {profile}")

    request_ids=tuple(spec["request_id"] for spec in CLAIMS)
    cur.execute(
        f"""
        SELECT id,request_id,request_status,approved_amount,
               insurance_details_limit_id,insurance_quarter_id,remark
        FROM claims_request WHERE request_id IN ({','.join(['%s']*len(request_ids))})
        ORDER BY request_id
        """,
        request_ids,
    )
    created=cur.fetchall()
    if len(created)!=4 or {int(row["id"]) for row in created}!=set(claim_ids):
        raise RuntimeError(f"Final R4 claim set mismatch: {created}")
    for row in created:
        if row["request_status"]!="APPROVED" or row["insurance_details_limit_id"]!=LIMIT_ID or row["insurance_quarter_id"]!=QUARTER_ID or TAG not in (row["remark"] or ""):
            raise RuntimeError(f"Invalid final R4 claim: {row}")

    cur.execute(
        """
        SELECT COUNT(*) n,COALESCE(SUM(request_amount),0) requested
        FROM claims_request WHERE employee=%s AND insurance_details_limit_id=%s
          AND request_status='UNDER_REVIEW'
        """,
        (users["237"],LIMIT_ID),
    )
    pending=cur.fetchone()
    if (int(pending["n"]),pending["requested"])!=(3,Decimal("5570.00")):
        raise RuntimeError(f"EPF 237 under-review claims were not preserved: {pending}")

    print("Created/idempotent claim IDs:",claim_ids)
    print("Final approved totals:",totals)
    print("Final balances:",balances)
    print(f"EPF 263: personal={personal_id}, application_user={users['263']}, username={username}, login_status={DEFAULT_LOGIN_STATUS}")
    print("EPF 237 UNDER_REVIEW preserved: 3 claims / Rs. 5570.00 requested")


def main()->int:
    parser=argparse.ArgumentParser()
    parser.add_argument("--apply",action="store_true",help="commit live changes")
    args=parser.parse_args()

    if DEFAULT_LOGIN_STATUS not in {"ACTIVE","INACTIVE"}:
        raise RuntimeError(f"Invalid application login status: {DEFAULT_LOGIN_STATUS}")

    with closing(_connect()) as connection:
        connection.autocommit=False
        with closing(connection.cursor(dictionary=True)) as cur:
            try:
                validate_configuration(cur)
                users=validate_existing_employees(cur)
                request_ids=tuple(spec["request_id"] for spec in CLAIMS)
                cur.execute(
                    f"SELECT request_id,remark FROM claims_request WHERE request_id IN ({','.join(['%s']*len(request_ids))}) FOR UPDATE",
                    request_ids,
                )
                existing_batch=cur.fetchall()
                already_applied=len(existing_batch)==4 and all(TAG in (row["remark"] or "") for row in existing_batch)

                if already_applied:
                    cur.execute(
                        """
                        SELECT upd.id personal_id,upd.user_company_details company_id,
                               au.id application_user_id,au.username
                        FROM user_personal_details upd
                        JOIN application_user au ON au.user_personal_details=upd.id
                        WHERE upd.epf_no='263' AND upd.nic='200168802653'
                        """
                    )
                    existing263=cur.fetchone()
                    if existing263 is None:
                        raise RuntimeError("R4 claims exist but EPF 263 profile is missing")
                    personal_id=int(existing263["personal_id"])
                    company_id=int(existing263["company_id"])
                    users["263"]=int(existing263["application_user_id"])
                    username=existing263["username"]
                    address_id=one(cur,"SELECT user_address id FROM user_personal_details WHERE id=%s",(personal_id,))["id"]
                else:
                    if existing_batch:
                        raise RuntimeError(f"Partial/colliding R4 request IDs exist: {existing_batch}")
                    validate_epf263_absent(cur)

                before={**fetch_approved_totals(cur,users),"263":Decimal("0.00")} if not already_applied else fetch_approved_totals(cur,users)
                if before!=(EXPECTED_AFTER if already_applied else EXPECTED_BEFORE):
                    raise RuntimeError(f"Unexpected pre-migration approved totals: {before}")

                validate_existing_claims(cur,users)
                print("Mode:","APPLY" if args.apply else "DRY_RUN")
                print("Before approved totals:",before)

                if not already_applied:
                    address_id,company_id,personal_id,app_id,username=create_epf263(cur)
                    users["263"]=app_id

                claim_ids=[insert_claim(cur,users[spec["epf"]],spec) for spec in CLAIMS]
                validate_final(cur,users,claim_ids,personal_id,company_id,username)

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


if __name__=="__main__":
    raise SystemExit(main())
