"""Scoped DTECH master import. Default is a read-only production preflight.

No existing rows, application users, claims or dependents are updated.
Execute requires the SHA256 printed by preflight, preventing workbook drift.
Credentials are read in memory over SSH; never logged or written to disk.
"""
import argparse
import contextlib
import hashlib
import io
import json
import re
import socket
import subprocess
import time
from datetime import date, datetime
from pathlib import Path

import mysql.connector
import openpyxl


HOST = "172.237.71.80"
DATABASE = "sgcs_care"
ACTOR = "DTECH_MASTER_20261029"
EXCLUDED = {"112"}
NO_INITIALS = {"141", "155"}
EXPECTED_EPFS = {
    "18", "78", "90", "108", "113", "117", "118", "124", "125", "133",
    "134", "136", "137", "138", "140", "141", "142", "145", "146", "147",
    "151", "152", "153", "154", "155", "158", "159", "160", "161",
}
CATEGORIES = {
    "NORMAL STAFF": ("NS", "P-NORMAL"),
    "SENIOR STAFF": ("SNR", "P-SENIOR"),
    "MIDDLE MANAGEMENT STAFF": ("MM", "P-MIDDLE"),
    "OPTION 1": ("EX-OP1", "P-EXE-OP1"),
    "OPTION 2": ("EX-OP2", "P-EXE-OP2"),
}
FIELDS = {
    "tital": "title", "initials": "initials", "firstname": "first_name",
    "lastname": "last_name", "dateofbirthddmmyyyy": "dob", "epfno": "epf",
    "maritalstatus": "marital", "contactnumber": "mobile", "nic": "nic",
    "gender": "gender", "emailaddress": "email", "currentdesignation": "designation",
    "dateofterminateddmmyyyy": "termination", "dateofappointmentddmmyyyy": "appointment",
    "company": "company", "option": "option", "staffcategory": "category",
    "approvestatus": "approval", "status": "status", "stafftype": "staff_type",
    "cityaddress": "city", "street01": "street_1", "street02": "street_2",
    "streetno": "street_no", "ddf": "ddf", "medical": "medical",
}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def text(value):
    return "" if value is None else str(value).strip()


def parse_date(value):
    if isinstance(value, datetime):
        return value.date()
    if isinstance(value, date):
        return value
    for fmt in ("%d/%m/%Y", "%Y-%m-%d", "%Y/%m/%d"):
        try:
            return datetime.strptime(text(value), fmt).date()
        except ValueError:
            pass
    raise ValueError("Invalid workbook date")


def load_rows(path):
    raw = path.read_bytes()
    digest = hashlib.sha256(raw).hexdigest()
    workbook = openpyxl.load_workbook(io.BytesIO(raw), read_only=True, data_only=True)
    try:
        rows = workbook["Sheet1"].iter_rows(values_only=True)
        headers = next(rows)
        columns = {i: FIELDS.get(re.sub(r"[^a-z0-9]", "", text(v).lower()))
                   for i, v in enumerate(headers)}
        require(set(FIELDS.values()) <= set(columns.values()), "Missing workbook columns")
        result = []
        seen = {k: set() for k in ("epf", "nic", "email")}
        for number, values in enumerate(rows, 2):
            if not any(text(v) for v in values):
                continue
            row = {key: values[i] for i, key in columns.items() if key}
            row = {k: (v if k in {"dob", "appointment"} else text(v)) for k, v in row.items()}
            if row["epf"] in EXCLUDED:
                continue
            epf = row["epf"]
            require(epf in EXPECTED_EPFS, f"Unapproved EPF in row {number}")
            require(row["company"] == "DTECH", f"Wrong company for EPF {epf}")
            require(row["status"] == "ACTIVE" and row["approval"] == "APPROVED",
                    f"Unapproved status for EPF {epf}")
            require(row["staff_type"] == "PERMANENT", f"Unexpected staff type: {epf}")
            require(row["ddf"] == "YES" and row["medical"] == "YES", f"Unexpected benefits: {epf}")
            require(row["termination"] in {"", "N/A"}, f"Termination date requires review: {epf}")
            row["dob"] = parse_date(row["dob"])
            row["appointment"] = parse_date(row["appointment"])
            require(row["dob"] < row["appointment"] <= date(2026, 10, 29), f"Invalid dates: {epf}")
            row["nic"] = row["nic"].upper()
            require(re.fullmatch(r"(?:\d{12}|\d{9}[VX])", row["nic"]), f"Invalid NIC: {epf}")
            require(re.fullmatch(r"0\d{9}", row["mobile"]), f"Invalid phone: {epf}")
            require(re.fullmatch(r"[^\s@]+@[^\s@]+\.[^\s@]+", row["email"]), f"Invalid email: {epf}")
            require(row["title"] in {"MR", "MRS", "MS", "MISS"}, f"Invalid title: {epf}")
            require(row["gender"] in {"MALE", "FEMALE"}, f"Invalid gender: {epf}")
            require(row["marital"] in {"MARRIED", "UNMARRIED", "SINGLE"}, f"Invalid marital status: {epf}")
            for field in ("first_name", "last_name", "initials"):
                require(len(row[field]) <= 30, f"Too-long {field}: {epf}")
                require(row[field] or (field == "initials" and epf in NO_INITIALS), f"Missing {field}: {epf}")
            for field in ("city", "street_1", "designation"):
                require(row[field], f"Missing {field}: {epf}")
            for field, value in row.items():
                if isinstance(value, str):
                    require(len(value) <= 255, f"Too-long {field}: {epf}")
            category_key = row["option"] if row["category"] == "EXECUTIVE STAFF" else row["category"]
            require(category_key in CATEGORIES, f"Unknown staff category: {epf}")
            row["category_code"], row["policy"] = CATEGORIES[category_key]
            for field in seen:
                normalized = row[field].upper()
                require(normalized not in seen[field], f"Duplicate workbook {field}: {epf}")
                seen[field].add(normalized)
            result.append(row)
        require(seen["epf"] == EXPECTED_EPFS, "Workbook employee set has changed")
        return result, digest
    finally:
        workbook.close()


@contextlib.contextmanager
def connection():
    ssh = ["ssh", "-i", str(Path.home() / ".ssh/wecare_prod_deploy"),
           "-o", "IdentitiesOnly=yes", "-o", "BatchMode=yes", "-o", "ConnectTimeout=8",
           "-o", "StrictHostKeyChecking=yes"]
    remote = """import subprocess,json
c=json.loads(subprocess.check_output(['docker','inspect','care-admin']))[0]
e=dict(v.split('=',1) for v in c['Config']['Env'] if '=' in v)
print(json.dumps({k:e.get(k) for k in ['SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD']}))
"""
    response = subprocess.run(ssh + ["-T", f"wecare-deploy@{HOST}", "python3", "-"],
                              input=remote, text=True, capture_output=True, timeout=20)
    require(response.returncode == 0, "SSH configuration read failed")
    config = json.loads(response.stdout)
    require(config["SPRING_DATASOURCE_URL"].startswith(
        "jdbc:mysql://host.docker.internal:3306/sgcs_care?"), "Unexpected database target")
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        port = sock.getsockname()[1]
    tunnel = subprocess.Popen(ssh + ["-N", "-o", "ExitOnForwardFailure=yes", "-L",
                                    f"127.0.0.1:{port}:127.0.0.1:3306", f"wecare-deploy@{HOST}"],
                              stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                              creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    db = None
    try:
        for _ in range(40):
            require(tunnel.poll() is None, "SSH tunnel exited")
            try:
                with socket.create_connection(("127.0.0.1", port), timeout=.2):
                    break
            except OSError:
                time.sleep(.15)
        db = mysql.connector.connect(host="127.0.0.1", port=port, database=DATABASE,
                                     user=config["SPRING_DATASOURCE_USERNAME"],
                                     password=config["SPRING_DATASOURCE_PASSWORD"],
                                     use_pure=True, connection_timeout=10, autocommit=False)
        yield db
    finally:
        if db is not None:
            db.rollback()
            db.close()
        tunnel.terminate()
        tunnel.wait(timeout=10)


def query(cursor, sql, args=()):
    cursor.execute(sql, args)
    return cursor.fetchall()


def preflight(cursor, rows):
    for table, codes in (("company_types", {"DTECH"}), ("staff_type", {"PER"}),
                         ("staff_category", {r["category_code"] for r in rows}),
                         ("insurance_policy", {r["policy"] for r in rows})):
        for code in codes:
            found = query(cursor, f"SELECT code FROM {table} WHERE code=%s AND status='ACTIVE'", (code,))
            require(len(found) == 1, f"Missing active reference: {table}/{code}")
    engines = query(cursor, "SELECT TABLE_NAME,ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=%s "
                    "AND TABLE_NAME IN ('user_address','user_company_details','user_personal_details')", (DATABASE,))
    require(len(engines) == 3 and all(r[1] == "InnoDB" for r in engines), "Non-transactional tables")
    triggers = query(cursor, "SELECT TRIGGER_NAME FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=%s "
                     "AND EVENT_OBJECT_TABLE IN ('user_address','user_company_details','user_personal_details') "
                     "AND EVENT_MANIPULATION='INSERT'", (DATABASE,))
    require(not triggers, "Insert triggers require review")
    require(not query(cursor, "SELECT id FROM user_personal_details WHERE created_user=%s", (ACTOR,)),
            "Batch already exists; stop rather than repeat")
    history = []
    for row in rows:
        matches = query(cursor, "SELECT p.id,p.epf_no,p.nic,p.email,p.user_status,c.company_type "
                        "FROM user_personal_details p JOIN user_company_details c ON c.id=p.user_company_details "
                        "WHERE (UPPER(TRIM(c.company_type))='DTECH' AND TRIM(p.epf_no)=%s) "
                        "OR UPPER(TRIM(p.nic))=%s OR LOWER(TRIM(p.email))=LOWER(%s)",
                        (row["epf"], row["nic"], row["email"]))
        for old in matches:
            require(not (old[5].strip().upper() == "DTECH" and str(old[1]).strip() == row["epf"]),
                    f"Existing DTECH EPF {row['epf']}: {old[0]}")
            require(old[4].strip().upper() != "ACTIVE", f"Active NIC/email conflict for EPF {row['epf']}: {old[0]}")
            history.append({"epf": row["epf"], "old_personal_id": old[0], "status": old[4]})
    return history


def insert(cursor, table, fields):
    columns = list(fields)
    cursor.execute(f"INSERT INTO {table} ({','.join(columns)}) VALUES ({','.join(['%s'] * len(columns))})",
                   tuple(fields[c] for c in columns))
    require(cursor.rowcount == 1, f"Unexpected insert count: {table}")
    return cursor.lastrowid


def apply(cursor, rows):
    records = []
    audit = {"created_user": ACTOR, "last_modified_user": ACTOR}
    for row in rows:
        address = insert(cursor, "user_address", dict(audit, city=row["city"], street_1=row["street_1"],
                         street_2=row["street_2"] or None, street_no=row["street_no"]))
        company = insert(cursor, "user_company_details", dict(audit, designation=row["designation"],
                         permanent_date=row["appointment"], company_type="DTECH", insurance_policy=row["policy"],
                         staff_category=row["category_code"], staff_type="PER", facility="BOTH"))
        personal = insert(cursor, "user_personal_details", dict(audit, dob=row["dob"], email=row["email"],
                          epf_no=row["epf"], first_name=row["first_name"], initials=row["initials"],
                          last_name=row["last_name"], marital_status=row["marital"], mobile_no=row["mobile"],
                          nic=row["nic"], user_status="ACTIVE", user_address=address, user_company_details=company,
                          gender=row["gender"], title=row["title"], is_temp=0))
        saved = query(cursor, "SELECT p.epf_no,p.initials,p.nic,p.user_status,DATE(c.permanent_date),"
                      "c.company_type,c.staff_category,c.insurance_policy,c.facility,p.user_address "
                      "FROM user_personal_details p JOIN user_company_details c ON c.id=p.user_company_details "
                      "WHERE p.id=%s", (personal,))
        require(saved == [(row["epf"], row["initials"], row["nic"], "ACTIVE", row["appointment"],
                           "DTECH", row["category_code"], row["policy"], "BOTH", address)],
                f"Inserted record verification failed for EPF {row['epf']}")
        records.append({"epf": row["epf"], "personal_id": personal, "company_details_id": company,
                        "address_id": address, "staff_category": row["category_code"],
                        "permanent_date": row["appointment"].isoformat()})
    for table in ("user_address", "user_company_details", "user_personal_details"):
        require(query(cursor, f"SELECT COUNT(*) FROM {table} WHERE created_user=%s", (ACTOR,))[0][0] == len(rows),
                f"Batch count mismatch: {table}")
    return records


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--excel", type=Path, required=True)
    parser.add_argument("--execute-sha256", help="Commit exactly the preflighted workbook")
    args = parser.parse_args()
    rows, digest = load_rows(args.excel)
    print(json.dumps({"workbook_sha256": digest, "employees": len(rows), "excluded_epf": sorted(EXCLUDED)}))
    if args.execute_sha256:
        require(args.execute_sha256 == digest, "Workbook changed after preflight")
    with connection() as db:
        cursor = db.cursor()
        cursor.execute("SET time_zone='+05:30'")
        db.commit()
        db.start_transaction(readonly=not bool(args.execute_sha256))
        try:
            if args.execute_sha256:
                require(query(cursor, "SELECT GET_LOCK('dtech_master_20261029',5)")[0][0] == 1,
                        "Import lock unavailable")
            protected = query(cursor, "SELECT p.* FROM user_personal_details p "
                              "JOIN user_company_details c ON c.id=p.user_company_details WHERE c.company_type='DTECH' "
                              "AND p.epf_no IN ('50001','0603') ORDER BY p.id" +
                              (" FOR UPDATE" if args.execute_sha256 else ""))
            history = preflight(cursor, rows)
            print(json.dumps({"preflight": "PASS", "ready": len(rows), "inactive_history_preserved": history}))
            if not args.execute_sha256:
                db.rollback()
                return
            records = apply(cursor, rows)
            after = query(cursor, "SELECT p.* FROM user_personal_details p "
                          "JOIN user_company_details c ON c.id=p.user_company_details WHERE c.company_type='DTECH' "
                          "AND p.epf_no IN ('50001','0603') ORDER BY p.id")
            require(after == protected, "Protected records changed")
            db.commit()
            print(json.dumps({"result": "COMMITTED", "database": DATABASE, "actor": ACTOR,
                              "inserted": len(records), "application_users_created": 0, "records": records}))
        except Exception:
            db.rollback()
            raise
        finally:
            cursor.close()


if __name__ == "__main__":
    main()
