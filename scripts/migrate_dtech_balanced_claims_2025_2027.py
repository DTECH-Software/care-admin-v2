"""Import dated DTECH claims reconciled to the six supplied balance workbooks.

Default mode checks production without writing. Apply uses one transaction and
requires the SHA256 of both source claim workbooks printed by the dry run.
Only the ten explicitly listed source rows are in scope. Other company rows,
unmatched employees, zero-approved claims, and policy conflicts are excluded.
"""
import argparse
import hashlib
import io
import json
import sys
from collections import defaultdict
from datetime import datetime, time
from decimal import Decimal
from pathlib import Path

import openpyxl

sys.dont_write_bytecode = True
import import_dtech_employee_master as m

BASE = Path(r'C:\Users\abhisheks\Documents\We Care Data\New folder')
EX_FILE = 'EX,MIDDLE & SNR Claim details.xlsx'
NS_FILE = 'Normal Staff Claims Details  - DTECH & S-LOGISTICS.xlsx'
TAG = '[MIGRATION:DTECH-BALANCE-2025-2027-R1]'
COMPANY = 'D TECH (PVT) LTD'

# (file, sheet, row, EPF, staff, period, treatment, category, approved amount)
SPECS = [
    (EX_FILE, 'DTECH EX & MIDDLE  2025-2026', 30, '108', 'EX-OP1', 4, 'OUTDOOR', 'OTHER', '1400'),
    (EX_FILE, 'DTECH EX & MIDDLE  2025-2026', 31, '108', 'EX-OP1', 4, 'OUTDOOR', 'OTHER', '5619'),
    (EX_FILE, 'DTECH EX & MIDDLE  2025-2026', 32, '108', 'EX-OP1', 4, 'OUTDOOR', 'OTHER', '1500'),
    (EX_FILE, 'DTECH EX & MIDDLE  2025-2026', 33, '108', 'EX-OP1', 4, 'OUTDOOR', 'OTHER', '1300'),
    (EX_FILE, 'DTECH EX & MIDDLE  2025-2026', 38, '141', 'EX-OP1', 4, 'OUTDOOR', 'DENTAL', '5000'),
    (EX_FILE, 'DTECH EX & MIDDLE  2025-2026', 436, '136', 'MM', 6, 'OUTDOOR', 'OTHER', '25956'),
    (EX_FILE, 'DTECH EX & MIDDLE  2025-2026', 437, '136', 'MM', 6, 'OUTDOOR', 'OTHER', '4044'),
    (NS_FILE, 'Normal Staff 2025-2026', 15, '117', 'NS', 3, 'INDOOR', 'SPECTACLE', '10000'),
    (EX_FILE, 'DTECH EX & MIDDLE 2026-2027', 5, '90', 'EX-OP1', 17, 'OUTDOOR', 'OTHER', '7000'),
    (EX_FILE, 'DTECH EX & MIDDLE 2026-2027', 129, '136', 'MM', 19, 'OUTDOOR', 'OTHER', '13601'),
]

# Each amount is the approved utilization shown in a balance workbook.
TARGETS = {
    ('108', 4, 'OUTDOOR'): Decimal('9819'),
    ('141', 4, 'OUTDOOR'): Decimal('5000'),
    ('136', 6, 'OUTDOOR'): Decimal('30000'),
    ('117', 3, 'INDOOR'): Decimal('10000'),
    ('90', 17, 'OUTDOOR'): Decimal('7000'),
    ('136', 19, 'OUTDOOR'): Decimal('13601'),
}

POLICY_FOR_STAFF = {'EX-OP1': 'P-EXE-OP1', 'MM': 'P-MIDDLE', 'NS': 'P-NORMAL'}


def source_claims():
    workbooks = {}
    digests = {}
    try:
        for name in (EX_FILE, NS_FILE):
            raw = (BASE / name).read_bytes()
            digests[name] = hashlib.sha256(raw).hexdigest()
            workbooks[name] = openpyxl.load_workbook(io.BytesIO(raw), data_only=True, read_only=True)
        results = []
        for name, sheet, number, epf, staff, period, treatment, category, amount in SPECS:
            values = [x.value for x in workbooks[name][sheet][number]][:11]
            actual_epf = m.text(values[0])
            company = m.text(values[1]).upper()
            actual_treatment = m.text(values[3]).upper()
            payment_type = m.text(values[4]).lower()
            actual_category = {'opd': 'OTHER', 'dental': 'DENTAL', 'eye': 'SPECTACLE'}.get(payment_type)
            category_name = m.text(values[2]).upper()
            m.require(actual_epf == epf and company == COMPANY, f'Source identity changed at {name}:{number}')
            m.require(actual_treatment == treatment and actual_category == category,
                      f'Source treatment changed at {name}:{number}')
            m.require(('OPTION 1' in category_name if staff == 'EX-OP1' else
                       'MIDDLE MANAGEMENT' in category_name if staff == 'MM' else
                       category_name == 'NORMAL STAFF'), f'Source staff changed at {name}:{number}')
            m.require(m.text(values[9]).lower() == 'done', f'Unpaid source row {name}:{number}')
            submitted = values[5]
            if isinstance(submitted, datetime):
                submitted = submitted.date()
            else:
                submitted = datetime.strptime(m.text(submitted), '%Y-%m-%d').date()
            requested = Decimal(str(values[6])).quantize(Decimal('.01'))
            approved = Decimal(str(values[7])).quantize(Decimal('.01'))
            m.require(requested >= approved == Decimal(amount) > 0,
                      f'Source amount changed at {name}:{number}')
            request_id = f'TC/HC/DTECH/{staff}/{submitted.year}/{number:04d}'
            remark = f'Approved legacy claim {TAG} SOURCE:{name}:{sheet}:{number}; treatment date unavailable'
            m.require(len(remark) <= 255, f'Remark too long at {name}:{number}')
            results.append(dict(source=f'{name}:{sheet}:{number}', epf=epf, staff=staff, period=period,
                                treatment=treatment, category=category, submitted=submitted,
                                requested=requested, approved=approved, request_id=request_id, remark=remark))
        m.require(len({x['request_id'] for x in results}) == len(results), 'Duplicate generated request ID')
        expected = defaultdict(lambda: Decimal('0'))
        for row in results:
            expected[row['epf'], row['period'], row['treatment']] += row['approved']
        m.require(dict(expected) == TARGETS, 'Source rows no longer match balance targets')
        return results, digests
    finally:
        for workbook in workbooks.values():
            workbook.close()


def existing_totals(cursor, user_id, limit_id, quarter_id):
    all_claims = m.query(cursor, "SELECT cr.id,cr.request_status,cr.approved_amount,cr.insurance_quarter_id "
                         "FROM claims_request cr WHERE cr.employee=%s AND cr.insurance_details_limit_id=%s",
                         (user_id, limit_id))
    overall = sum((r[2] or Decimal('0') for r in all_claims if r[1] == 'APPROVED'), Decimal('0'))
    quarter = sum((r[2] or Decimal('0') for r in all_claims if r[1] == 'APPROVED' and r[3] == quarter_id), Decimal('0'))
    return overall, quarter


def prepare(cursor, rows):
    added_global = defaultdict(lambda: Decimal('0'))
    added_quarter = defaultdict(lambda: Decimal('0'))
    for row in rows:
        users = m.query(cursor, "SELECT au.id,p.id,p.user_status,c.company_type,c.staff_category,c.insurance_policy "
                        "FROM application_user au JOIN user_personal_details p ON p.id=au.user_personal_details "
                        "JOIN user_company_details c ON c.id=p.user_company_details "
                        "WHERE c.company_type='DTECH' AND p.epf_no=%s", (row['epf'],))
        m.require(len(users) == 1, f'Missing/ambiguous employee EPF {row["epf"]}')
        user = users[0]
        m.require(user[2:] == ('ACTIVE', 'DTECH', row['staff'], POLICY_FOR_STAFF[row['staff']]),
                  f'Employee category or policy mismatch EPF {row["epf"]}')
        row['user_id'] = user[0]
        periods = m.query(cursor, "SELECT from_date,to_date,status,staff_category "
                          "FROM insurance_staff_category_period WHERE id=%s", (row['period'],))
        m.require(len(periods) == 1 and periods[0][2:] == ('ACTIVE', row['staff']) and
                  periods[0][0] <= row['submitted'] <= periods[0][1],
                  f'Claim application date outside policy EPF {row["epf"]}')
        limits = m.query(cursor, "SELECT id,global_limit,status FROM insurance_details_limit "
                         "WHERE insurance_staff_category_period=%s AND insurance_policy=%s AND treatment=%s",
                         (row['period'], POLICY_FOR_STAFF[row['staff']], row['treatment']))
        m.require(len(limits) == 1 and limits[0][2] == 'ACTIVE', f'Missing active limit EPF {row["epf"]}')
        row['limit_id'], row['global_limit'] = limits[0][:2]
        quarters = m.query(cursor, "SELECT id,quarter_limit FROM insurance_quarter WHERE insurance_details_id=%s "
                           "AND treatment_category_code=%s AND ((from_date IS NULL AND to_date IS NULL) "
                           "OR (%s BETWEEN from_date AND to_date))",
                           (row['limit_id'], row['category'], row['submitted']))
        m.require(len(quarters) == 1, f'Missing or overlapping quarter EPF {row["epf"]}')
        row['quarter_id'], row['quarter_limit'] = quarters[0]
        m.require(not m.query(cursor, 'SELECT id FROM claims_request WHERE request_id=%s', (row['request_id'],)),
                  f'Request ID already exists: {row["request_id"]}')
        m.require(not m.query(cursor, "SELECT cr.id FROM claims_request cr JOIN insurance_claims_details icd "
                              "ON icd.id=cr.insurance_claims_details WHERE cr.employee=%s "
                              "AND cr.insurance_details_limit_id=%s AND DATE(cr.created_date)=%s "
                              "AND cr.request_amount=%s AND cr.approved_amount=%s "
                              "AND icd.treatment=%s AND icd.treatment_category=%s",
                              (row['user_id'], row['limit_id'], row['submitted'], row['requested'],
                               row['approved'], row['treatment'], row['category'])),
                  f'Possible existing duplicate at {row["source"]}')
        current_global, current_quarter = existing_totals(cursor, row['user_id'], row['limit_id'], row['quarter_id'])
        gkey = row['user_id'], row['limit_id']
        qkey = row['user_id'], row['quarter_id']
        added_global[gkey] += row['approved']
        added_quarter[qkey] += row['approved']
        m.require(current_global + added_global[gkey] <= row['global_limit'],
                  f'Global limit exceeded EPF {row["epf"]}')
        m.require(current_quarter + added_quarter[qkey] <= row['quarter_limit'],
                  f'Quarter limit exceeded EPF {row["epf"]}')


def apply(cursor, rows):
    inserted = []
    for row in rows:
        # The source gives an application date, not a treatment date. The
        # required legacy treatment-date field uses that source date, while
        # the remark explicitly records that treatment date was unavailable.
        timestamp = datetime.combine(row['submitted'], time(12, 0))
        detail_id = m.insert(cursor, 'insurance_claims_details', dict(
            created_date=timestamp, last_modified_date=timestamp,
            disease='The Care Data Migration', from_treatment_date=None,
            to_treatment_date=row['submitted'], treatment=row['treatment'],
            treatment_category=row['category'], insurance_staff_category_period=row['period']))
        claim_id = m.insert(cursor, 'claims_request', dict(
            created_date=timestamp, last_modified_date=timestamp,
            remark=row['remark'], request_amount=row['requested'],
            request_id=row['request_id'], request_status='APPROVED',
            dependent=None, employee=row['user_id'], insurance_claims_details=detail_id,
            approval_work_flow_id=None, insurance_details_limit_id=row['limit_id'],
            insurance_quarter_id=row['quarter_id'], approval_level='LEVEL02',
            approved_amount=row['approved']))
        inserted.append(dict(epf=row['epf'], claim_id=claim_id, request_id=row['request_id'],
                             approved=str(row['approved']), source=row['source']))
    m.require(m.query(cursor, 'SELECT COUNT(*) FROM claims_request WHERE remark LIKE %s',
                      (f'%{TAG}%',))[0][0] == len(rows), 'Migration row count mismatch')
    for (epf, period, treatment), amount in TARGETS.items():
        found = m.query(cursor, "SELECT COALESCE(SUM(cr.approved_amount),0) FROM claims_request cr "
                        "JOIN application_user au ON au.id=cr.employee "
                        "JOIN user_personal_details p ON p.id=au.user_personal_details "
                        "JOIN user_company_details co ON co.id=p.user_company_details "
                        "JOIN insurance_claims_details d ON d.id=cr.insurance_claims_details "
                        "WHERE co.company_type='DTECH' AND p.epf_no=%s AND cr.request_status='APPROVED' "
                        "AND d.insurance_staff_category_period=%s AND d.treatment=%s",
                        (epf, period, treatment))
        m.require(found[0][0] == amount, f'Balance mismatch EPF {epf}, period {period}, {treatment}')
    return inserted


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply-digest', help='combined SHA256 value printed by dry run')
    args = parser.parse_args()
    rows, digests = source_claims()
    batch_digest = hashlib.sha256(json.dumps(digests, sort_keys=True).encode()).hexdigest()
    print(json.dumps(dict(sources=digests, batch_digest=batch_digest, candidate_claims=len(rows),
                          approved_total=str(sum((x['approved'] for x in rows), Decimal('0'))))), flush=True)
    if args.apply_digest:
        m.require(args.apply_digest == batch_digest, 'Source workbook changed')
    with m.connection() as db:
        c = db.cursor()
        c.execute("SET time_zone='+05:30'")
        db.commit()
        db.start_transaction(readonly=not bool(args.apply_digest))
        try:
            if args.apply_digest:
                m.require(m.query(c, "SELECT GET_LOCK('dtech_balanced_claims_2025_2027',5)")[0][0] == 1,
                          'Migration lock unavailable')
            for table in ('claims_request', 'insurance_claims_details'):
                m.require(m.query(c, 'SELECT ENGINE FROM information_schema.TABLES '
                                  'WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s', (m.DATABASE, table)) == [('InnoDB',)],
                          f'Unexpected storage engine {table}')
            m.require(not m.query(c, 'SELECT id FROM claims_request WHERE remark LIKE %s',
                                  (f'%{TAG}%',)), 'Migration already applied')
            prepare(c, rows)
            print(f'PREFLIGHT PASS: {len(rows)} dated claims, staff, periods, limits, quarter caps and duplicates checked', flush=True)
            if args.apply_digest:
                inserted = apply(c, rows)
                db.commit()
                print(json.dumps(dict(result='COMMITTED', claims=inserted)), flush=True)
            else:
                db.rollback()
        except Exception:
            db.rollback()
            raise
        finally:
            c.close()


if __name__ == '__main__':
    main()
