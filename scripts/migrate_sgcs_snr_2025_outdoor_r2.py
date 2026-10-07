"""Add two client-confirmed SGCS SNR 2025-2026 OUTDOOR claims.

EPF 48 receives Rs. 15,000 DENTAL and EPF 251 receives Rs. 6,340
OTHER. Source treatment dates were not supplied, so the policy start and the
first applicable category quarter are used with an explicit audit marker.

Dry-run is the default; --apply commits after all exact-balance checks pass.
"""

from __future__ import annotations

import argparse
from contextlib import closing
from datetime import date, datetime
from decimal import Decimal

from wecare_spectacle import _connect


TAG = "[MIGRATION:SGCS-SNR-2025-OUTDOOR-R2]"
LIMIT_ID = 13
PERIOD_ID = 7
POLICY_START = date(2025,8,13)
POLICY_END = date(2026,8,13)
GLOBAL_LIMIT = Decimal("80000.00")

CLAIMS = (
    {
        "epf":"48","request_id":"TC/HC/SGCS/SNR/2026/0036",
        "amount":Decimal("15000.00"),"category":"DENTAL","quarter_id":89,
        "created":datetime(2025,8,13,12,6),
        "source":"BALANCE-SHEET-EPF48-DENTAL-15000",
    },
    {
        "epf":"251","request_id":"TC/HC/SGCS/SNR/2026/0037",
        "amount":Decimal("6340.00"),"category":"OTHER","quarter_id":86,
        "created":datetime(2025,8,13,12,7),
        "source":"BALANCE-SHEET-EPF251-OTHER-6340",
    },
)

EXPECTED_BEFORE={"48":Decimal("26149.00"),"251":Decimal("14000.00")}
EXPECTED_AFTER={"48":Decimal("41149.00"),"251":Decimal("20340.00")}
EXPECTED_BALANCES={"48":Decimal("38851.00"),"251":Decimal("59660.00")}
EXPECTED_CATEGORY_AFTER={
    "48":{"OTHER":Decimal("6149.00"),"SPECTACLE":Decimal("20000.00"),"DENTAL":Decimal("15000.00")},
    "251":{"OTHER":Decimal("13340.00"),"DENTAL":Decimal("7000.00")},
}


def one(cur,sql,params=()):
    cur.execute(sql,params)
    row=cur.fetchone()
    if row is None:
        raise RuntimeError("Expected one row but query returned none")
    return row


def validate_configuration(cur):
    if one(cur,"SELECT DATABASE() db")["db"]!="sgcs_care":
        raise RuntimeError("Refusing to run outside live sgcs_care")
    row=one(cur,"""
        SELECT insurance_staff_category_period,insurance_policy,treatment,
               global_limit,is_quarter,status
        FROM insurance_details_limit WHERE id=%s
    """,(LIMIT_ID,))
    actual=(row["insurance_staff_category_period"],row["insurance_policy"],row["treatment"],row["global_limit"],row["is_quarter"],row["status"])
    if actual!=(PERIOD_ID,"P-SENIOR","OUTDOOR",GLOBAL_LIMIT,1,"ACTIVE"):
        raise RuntimeError(f"SNR OUTDOOR limit changed: {actual}")
    period=one(cur,"SELECT staff_category,from_date,to_date,status FROM insurance_staff_category_period WHERE id=%s",(PERIOD_ID,))
    if tuple(period.values())!=("SNR",POLICY_START,POLICY_END,"ACTIVE"):
        raise RuntimeError(f"SNR period changed: {period}")
    cur.execute("SELECT id,treatment_category_code,quarter_limit,from_date,to_date FROM insurance_quarter WHERE id IN (86,89) ORDER BY id")
    quarters={int(r["id"]):r for r in cur.fetchall()}
    if set(quarters)!={86,89}:
        raise RuntimeError(f"Required category quarters missing: {quarters}")
    if (quarters[86]["treatment_category_code"],quarters[86]["quarter_limit"],quarters[86]["from_date"],quarters[86]["to_date"])!=("OTHER",GLOBAL_LIMIT,POLICY_START,date(2026,2,12)):
        raise RuntimeError(f"OTHER quarter changed: {quarters[86]}")
    if (quarters[89]["treatment_category_code"],quarters[89]["quarter_limit"],quarters[89]["from_date"],quarters[89]["to_date"])!=("DENTAL",Decimal("15000.00"),POLICY_START,POLICY_END):
        raise RuntimeError(f"DENTAL quarter changed: {quarters[89]}")


def load_users(cur):
    cur.execute("""
        SELECT upd.epf_no,au.id application_user_id,upd.user_status,
               ucd.company_type,ucd.staff_category,ucd.insurance_policy
        FROM user_personal_details upd
        JOIN user_company_details ucd ON ucd.id=upd.user_company_details
        JOIN application_user au ON au.user_personal_details=upd.id
        WHERE upd.epf_no IN ('48','251') ORDER BY upd.epf_no,au.id FOR UPDATE
    """)
    rows=cur.fetchall()
    if len(rows)!=2:
        raise RuntimeError(f"Expected exactly EPF 48 and 251 users: {rows}")
    users={}
    for row in rows:
        actual=(row["user_status"],row["company_type"],row["staff_category"],row["insurance_policy"])
        if actual!=("ACTIVE","SGCS","SNR","P-SENIOR"):
            raise RuntimeError(f"EPF {row['epf_no']} profile changed: {actual}")
        users[str(row["epf_no"])]=int(row["application_user_id"])
    if set(users)!={"48","251"}:
        raise RuntimeError(f"Employee set mismatch: {users}")
    return users


def totals(cur,users):
    result={}
    for epf,user_id in users.items():
        result[epf]=one(cur,"""
            SELECT COALESCE(SUM(approved_amount),0) approved
            FROM claims_request WHERE employee=%s AND insurance_details_limit_id=%s
              AND request_status='APPROVED'
        """,(user_id,LIMIT_ID))["approved"]
    return result


def category_totals(cur,users):
    result={epf:{} for epf in users}
    for epf,user_id in users.items():
        cur.execute("""
            SELECT icd.treatment_category,COALESCE(SUM(cr.approved_amount),0) approved
            FROM claims_request cr
            JOIN insurance_claims_details icd ON icd.id=cr.insurance_claims_details
            WHERE cr.employee=%s AND cr.insurance_details_limit_id=%s
              AND cr.request_status='APPROVED'
            GROUP BY icd.treatment_category
        """,(user_id,LIMIT_ID))
        result[epf]={r["treatment_category"]:r["approved"] for r in cur.fetchall()}
    return result


def validate_existing(cur,users):
    expected={
        373:(users["48"],Decimal("6149.00"),"OTHER"),
        684:(users["48"],Decimal("20000.00"),"SPECTACLE"),
        408:(users["251"],Decimal("7000.00"),"OTHER"),
        414:(users["251"],Decimal("7000.00"),"DENTAL"),
    }
    ids=tuple(expected)
    cur.execute(f"""
        SELECT cr.id,cr.employee,cr.request_status,cr.approved_amount,
               cr.insurance_details_limit_id,icd.treatment,icd.treatment_category
        FROM claims_request cr JOIN insurance_claims_details icd
          ON icd.id=cr.insurance_claims_details
        WHERE cr.id IN ({','.join(['%s']*len(ids))}) ORDER BY cr.id FOR UPDATE
    """,ids)
    rows={int(r["id"]):r for r in cur.fetchall()}
    if set(rows)!=set(expected):
        raise RuntimeError(f"Base claim set changed: {rows}")
    for claim_id,(user_id,amount,category) in expected.items():
        r=rows[claim_id]
        actual=(r["employee"],r["request_status"],r["approved_amount"],r["insurance_details_limit_id"],r["treatment"],r["treatment_category"])
        if actual!=(user_id,"APPROVED",amount,LIMIT_ID,"OUTDOOR",category):
            raise RuntimeError(f"Base claim {claim_id} changed: {actual}")
    cur.execute("""
        SELECT id,request_status,request_amount,approved_amount
        FROM claims_request WHERE employee=%s AND id IN (509,514) ORDER BY id FOR UPDATE
    """,(users["251"],))
    rejected=cur.fetchall()
    if [(r["id"],r["request_status"],r["request_amount"],r["approved_amount"]) for r in rejected] != [
        (509,"REJECTED",Decimal("15930.00"),None),(514,"REJECTED",Decimal("9120.00"),None)
    ]:
        raise RuntimeError(f"EPF 251 rejected claims changed: {rejected}")


def insert_claim(cur,user_id,spec):
    cur.execute("SELECT id,employee,approved_amount,remark FROM claims_request WHERE request_id=%s FOR UPDATE",(spec["request_id"],))
    existing=cur.fetchone()
    if existing:
        if existing["employee"]!=user_id or existing["approved_amount"]!=spec["amount"] or TAG not in (existing["remark"] or ""):
            raise RuntimeError(f"Request ID collision: {existing}")
        return int(existing["id"])
    cur.execute("""
        INSERT INTO insurance_claims_details
          (created_date,last_modified_date,disease,from_treatment_date,
           to_treatment_date,treatment,treatment_category,
           insurance_staff_category_period)
        VALUES (%s,%s,'The Care Data Migration',NULL,%s,'OUTDOOR',%s,%s)
    """,(spec["created"],spec["created"],POLICY_START,spec["category"],PERIOD_ID))
    detail_id=int(cur.lastrowid)
    remark=f"Approved legacy balance reconciliation {TAG} SOURCE:{spec['source']};SOURCE-DATE-NOT-PROVIDED;POLICY-START-USED"
    if len(remark)>255:
        raise RuntimeError("Audit remark exceeds 255 characters")
    cur.execute("""
        INSERT INTO claims_request
          (created_date,last_modified_date,remark,request_amount,request_id,
           request_status,dependent,employee,insurance_claims_details,
           approval_work_flow_id,insurance_details_limit_id,
           insurance_quarter_id,approval_level,approved_amount,assisted_mobile_no)
        VALUES (%s,%s,%s,%s,%s,'APPROVED',NULL,%s,%s,NULL,%s,%s,'LEVEL02',%s,NULL)
    """,(spec["created"],spec["created"],remark,spec["amount"],spec["request_id"],user_id,detail_id,LIMIT_ID,spec["quarter_id"],spec["amount"]))
    return int(cur.lastrowid)


def validate_final(cur,users,claim_ids):
    final_totals=totals(cur,users)
    balances={epf:GLOBAL_LIMIT-value for epf,value in final_totals.items()}
    if final_totals!=EXPECTED_AFTER or balances!=EXPECTED_BALANCES:
        raise RuntimeError(f"Final totals/balances mismatch: {final_totals} / {balances}")
    final_categories=category_totals(cur,users)
    if final_categories!=EXPECTED_CATEGORY_AFTER:
        raise RuntimeError(f"Final category totals mismatch: {final_categories}")
    request_ids=tuple(spec["request_id"] for spec in CLAIMS)
    cur.execute(f"SELECT id,request_status,remark FROM claims_request WHERE request_id IN ({','.join(['%s']*len(request_ids))}) ORDER BY request_id",request_ids)
    rows=cur.fetchall()
    if len(rows)!=2 or {int(r["id"]) for r in rows}!=set(claim_ids) or any(r["request_status"]!="APPROVED" or TAG not in (r["remark"] or "") for r in rows):
        raise RuntimeError(f"Final R2 claim set invalid: {rows}")
    # The two rejected EPF 251 requests remain unchanged and do not consume limits.
    cur.execute("SELECT COUNT(*) n FROM claims_request WHERE employee=%s AND id IN (509,514) AND request_status='REJECTED' AND approved_amount IS NULL",(users["251"],))
    if int(cur.fetchone()["n"])!=2:
        raise RuntimeError("EPF 251 rejected requests were not preserved")
    print("Created/idempotent claim IDs:",claim_ids)
    print("Final approved totals:",final_totals)
    print("Final balances:",balances)
    print("Final category totals:",final_categories)
    print("EPF 251 rejected claims preserved: 2")


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--apply",action="store_true")
    args=parser.parse_args()
    with closing(_connect()) as connection:
        connection.autocommit=False
        with closing(connection.cursor(dictionary=True)) as cur:
            try:
                validate_configuration(cur)
                users=load_users(cur)
                validate_existing(cur,users)
                request_ids=tuple(spec["request_id"] for spec in CLAIMS)
                cur.execute(f"SELECT request_id,remark FROM claims_request WHERE request_id IN ({','.join(['%s']*len(request_ids))}) FOR UPDATE",request_ids)
                existing=cur.fetchall()
                already_applied=len(existing)==2 and all(TAG in (r["remark"] or "") for r in existing)
                if existing and not already_applied:
                    raise RuntimeError(f"Partial/colliding request IDs: {existing}")
                before=totals(cur,users)
                if before!=(EXPECTED_AFTER if already_applied else EXPECTED_BEFORE):
                    raise RuntimeError(f"Unexpected approved totals before migration: {before}")
                print("Mode:","APPLY" if args.apply else "DRY_RUN")
                print("Before approved totals:",before)
                claim_ids=[insert_claim(cur,users[s["epf"]],s) for s in CLAIMS]
                validate_final(cur,users,claim_ids)
                if args.apply:
                    connection.commit(); print(f"COMMITTED: marker={TAG}")
                else:
                    connection.rollback(); print("DRY RUN PASSED; transaction rolled back")
            except Exception:
                connection.rollback(); raise
    return 0


if __name__=="__main__":
    raise SystemExit(main())
