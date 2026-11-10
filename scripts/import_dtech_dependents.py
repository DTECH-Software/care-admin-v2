"""Scoped, atomic dependent import; dry-run by default. No SMS is sent."""
import argparse
import base64
import hashlib
import io
import json
import re
import secrets
import sys
from datetime import date, datetime
from pathlib import Path
sys.dont_write_bytecode = True
import openpyxl
import import_dtech_employee_master as m

ACTOR = 'DTECH_DEPENDENTS_20261029'


def load(path):
    raw = path.read_bytes()
    w = openpyxl.load_workbook(io.BytesIO(raw), read_only=True, data_only=True)
    result, held, excluded, seen = [], [], [], set()
    try:
        for n, r in enumerate(w.active.iter_rows(values_only=True), 1):
            if n == 1 or not any(m.text(v) for v in r):
                continue
            epf = m.text(r[2])
            m.require(m.text(r[1]) == 'DTECH', f'Unexpected company row {n}')
            if epf == '112':
                excluded.append(n)
                continue
            m.require(epf in m.EXPECTED_EPFS, f'Unknown employee row {n}')
            if epf == '124':
                held.append(n)
                continue
            dob = r[9]
            if isinstance(dob, datetime):
                dob = dob.date()
            elif not isinstance(dob, date):
                parsed = None
                for fmt in ('%d.%m.%Y', '%Y.%m.%d', '%d/%m/%Y', '%m/%d/%Y', '%Y-%m-%d'):
                    try:
                        parsed = datetime.strptime(m.text(dob), fmt).date()
                        break
                    except ValueError:
                        pass
                m.require(parsed is not None, f'Invalid DOB row {n}')
                dob = parsed
            m.require(dob <= date(2026, 10, 29), f'Future DOB row {n}')
            relation = m.text(r[4]).upper()
            relation = {'SON': 'CHILD', 'DAUGHTER': 'CHILD'}.get(relation, relation)
            category = {'FATHER':'PARENTS','MOTHER':'PARENTS','WIFE':'SPOUSE','HUSBAND':'SPOUSE',
                        'BROTHER':'SIBLING','SISTER':'SIBLING','CHILD':'CHILDREN'}[relation]
            supplied = {'PARENT':'PARENTS','SIBILINGS':'SIBLING'}.get(m.text(r[3]).upper(),m.text(r[3]).upper())
            m.require(category == supplied, f'Category mismatch row {n}')
            gender = m.text(r[10]).upper().replace('FEMA;E','FEMALE')
            expected_gender = {'FATHER':'MALE','MOTHER':'FEMALE','BROTHER':'MALE','SISTER':'FEMALE','WIFE':'FEMALE','HUSBAND':'MALE'}
            m.require(gender in ('MALE','FEMALE') and expected_gender.get(relation,gender)==gender,f'Gender mismatch row {n}')
            facility = {'DDF':'DEATH','BOTH':'BOTH'}[m.text(r[13]).upper()]
            alive = {'LIVE':True,'NOT LIVE':False}[m.text(r[14]).upper()]
            nic = m.text(r[11]).upper()
            nic = None if nic in ('','N/A') else nic
            m.require(nic is None or re.fullmatch(r'(?:\d{12}|\d{9}[VX])',nic), f'Invalid NIC row {n}')
            fields = dict(dependent_category=category,dob=dob,eligible_facility=facility,
                          first_name=m.text(r[7]),last_name=m.text(r[8]),initials=m.text(r[6]),
                          gender=gender,nic=nic,relation_category=relation,job_title=m.text(r[12]) or None,
                          title='MR' if gender=='MALE' else 'MS',live_status=alive,status='APPROVED')
            m.require(fields['first_name'] and fields['last_name'],f'Missing name row {n}')
            for value in fields.values():
                m.require(not isinstance(value,str) or len(value)<=255,f'Too long row {n}')
            keys=[(epf,'identity',fields['first_name'].upper(),fields['last_name'].upper(),dob,relation)]
            if nic: keys.append((epf,'nic',nic))
            for key in keys:
                m.require(key not in seen,f'Duplicate workbook dependent row {n}')
                seen.add(key)
            result.append(dict(row=n,epf=epf,fields=fields))
    finally:
        w.close()
    return result,held,excluded,hashlib.sha256(raw).hexdigest()


def run(args):
    rows,held,excluded,digest=load(args.excel)
    print(json.dumps(dict(sha256=digest,valid=len(rows),held_rows=held,excluded_rows=excluded)),flush=True)
    if args.execute_sha256:
        m.require(digest==args.execute_sha256,'Workbook changed')
    with m.connection() as db:
        c=db.cursor()
        c.execute("SET time_zone='+05:30'")
        db.commit()
        db.start_transaction(readonly=not bool(args.execute_sha256))
        if args.execute_sha256:
            m.require(m.query(c,"SELECT GET_LOCK('dtech_dependent_import',5)")[0][0]==1,'Import busy')
        tables=('application_user','onboarding_request','claims_dependents')
        for table in tables:
            m.require(m.query(c,'SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s',(m.DATABASE,table))==[('InnoDB',)],'Nontransactional table')
        m.require(not m.query(c,'SELECT id FROM claims_dependents WHERE approved_user=%s',(ACTOR,)),'Batch already imported')
        users={}
        for epf in sorted({r['epf'] for r in rows}):
            found=m.query(c,"SELECT p.id,p.dob,p.first_name,p.email,p.mobile_no FROM user_personal_details p JOIN user_company_details co ON co.id=p.user_company_details WHERE co.company_type='DTECH' AND p.epf_no=%s AND p.user_status='ACTIVE'"+(' FOR UPDATE' if args.execute_sha256 else ''),(epf,))
            m.require(len(found)==1,f'Employee missing/ambiguous {epf}')
            pid,dob,first,email,mobile=found[0]
            existing=m.query(c,'SELECT id FROM application_user WHERE user_personal_details=%s',(pid,))
            m.require(len(existing)<=1,f'Multiple accounts EPF {epf}')
            username=re.sub('[^A-Z0-9]','',(first+epf+'DTECH').upper())
            if not existing:
                m.require(not m.query(c,'SELECT id FROM application_user WHERE LOWER(TRIM(username))=LOWER(%s) OR LOWER(TRIM(primary_email))=LOWER(%s)',(username,email)),f'Username/email account conflict EPF {epf}')
            users[epf]=dict(pid=pid,dob=dob,email=email,mobile=mobile,username=username,app=existing[0][0] if existing else None)
        skipped=[]; ready=[]
        for row in rows:
            f=row['fields']; u=users[row['epf']]
            if f['dependent_category']=='PARENTS':m.require(f['dob']<u['dob'],f'Parent age invalid row {row["row"]}')
            if f['dependent_category']=='CHILDREN':m.require(f['dob']>u['dob'],f'Child age invalid row {row["row"]}')
            duplicates=[]
            if u['app']:
                duplicates=m.query(c,"SELECT id FROM claims_dependents WHERE application_user=%s AND status IN ('ACTIVE','APPROVED','UNDER_REVIEW') AND ((%s IS NOT NULL AND UPPER(TRIM(nic))=%s) OR (UPPER(TRIM(first_name))=UPPER(%s) AND UPPER(TRIM(last_name))=UPPER(%s) AND dob=%s AND relation_category=%s))",(u['app'],f['nic'],f['nic'],f['first_name'],f['last_name'],f['dob'],f['relation_category']))
            (skipped if duplicates else ready).append(row)
        print(json.dumps(dict(preflight='PASS',dependents_ready=len(ready),duplicates_skipped=len(skipped),accounts_needed=sum(not u['app'] for u in users.values()))),flush=True)
        if not args.execute_sha256:
            db.rollback();return
        now=m.query(c,'SELECT NOW()')[0][0]
        accounts=[]; inserted=[]
        for epf in sorted({r['epf'] for r in ready}):
            u=users[epf]
            if u['app']:continue
            salt=secrets.token_hex(32)
            hashed=base64.b64encode(hashlib.sha256((salt+secrets.token_urlsafe(32)).encode()).digest()).decode()
            onboarding=m.insert(c,'onboarding_request',dict(request_status='ACTIVE',user_custom_details='{}'))
            u['app']=m.insert(c,'application_user',dict(attempt_count=0,expecting_dependents_register=False,
                expecting_first_time_loging=True,is_reset=True,login_status='ACTIVE',otp_attempt_count=0,
                password=hashed,password_expired_date=now,primary_email=u['email'],primary_mobile=u['mobile'],
                user_key=salt,username=u['username'],onboarding_request=onboarding,user_personal_details=u['pid'],
                facility_id='PENDING'))
            c.execute('UPDATE application_user SET facility_id=%s WHERE id=%s',('0'+str(u['app']),u['app']))
            accounts.append(dict(epf=epf,application_user_id=u['app'],username=u['username']))
        for row in ready:
            fields=dict(row['fields'],application_user=users[row['epf']]['app'],approved_date=now,
                        approved_user=ACTOR,remark=f"{ACTOR}; source row {row['row']}")
            did=m.insert(c,'claims_dependents',fields)
            saved=m.query(c,'SELECT '+','.join(fields)+' FROM claims_dependents WHERE id=%s',(did,))
            m.require(saved==[tuple(fields.values())],f'Verification failed row {row["row"]}')
            inserted.append(dict(row=row['row'],epf=row['epf'],dependent_id=did))
        m.require(m.query(c,'SELECT COUNT(*) FROM claims_dependents WHERE approved_user=%s',(ACTOR,))[0][0]==len(ready),'Count mismatch')
        db.commit()
        print(json.dumps(dict(result='COMMITTED',dependents_inserted=len(inserted),accounts_created=accounts,records=inserted,held_rows=held,excluded_rows=excluded)),flush=True)


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--excel',type=Path,required=True)
    p.add_argument('--execute-sha256')
    run(p.parse_args())
