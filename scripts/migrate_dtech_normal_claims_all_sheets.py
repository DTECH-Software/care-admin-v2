"""Import valid DTECH Normal Staff legacy claims from the three-sheet workbook.

Default mode is a production read-only preflight. Apply mode requires the
combined workbook digest printed by dry run and writes in one transaction.
Exact duplicate source rows are imported once; unmatched and out-of-period
rows are reported but never inferred or inserted.
"""
import argparse
import hashlib
import io
import json
import sys
from collections import defaultdict
from datetime import date, datetime, time
from decimal import Decimal
from pathlib import Path

import openpyxl

sys.dont_write_bytecode = True
import import_dtech_employee_master as m

SOURCE = Path(r'C:\Users\abhisheks\Documents\We Care Data\New folder\Normal Staff Claims Details  - DTECH & S-LOGISTICS.xlsx')
COMPANY = 'D TECH (PVT) LTD'
TAG = '[MIGRATION:DTECH-NS-ALL-SHEETS-R1]'

# Source sheet row, expected employee, period, treatment, category, approved.
# Row 74 is an exact duplicate of row 73 across all claim fields and is listed
# in DUPLICATE_ROWS so the source is retained in the audit summary but inserted
# only once.
SHEETS = {
    'Normal Staff 2024-2025': 10,
    'Normal Staff 2025-2026': 3,
    'Normal Staff 2026-2027': 12,
}
CANDIDATES = [
    ('Normal Staff 2024-2025', 7, '117', 10, 'OUTDOOR', 'OTHER', '1700'),
    ('Normal Staff 2024-2025', 8, '117', 10, 'OUTDOOR', 'OTHER', '2550'),
    ('Normal Staff 2024-2025', 9, '117', 10, 'OUTDOOR', 'OTHER', '2356'),
    ('Normal Staff 2024-2025', 10, '117', 10, 'OUTDOOR', 'OTHER', '1894'),
    ('Normal Staff 2025-2026', 6, '117', 3, 'OUTDOOR', 'OTHER', '900'),
    ('Normal Staff 2025-2026', 7, '117', 3, 'OUTDOOR', 'OTHER', '3780'),
    ('Normal Staff 2026-2027', 72, '117', 12, 'OUTDOOR', 'OTHER', '4584'),
    ('Normal Staff 2026-2027', 73, '117', 12, 'OUTDOOR', 'OTHER', '2080'),
    ('Normal Staff 2026-2027', 75, '117', 12, 'OUTDOOR', 'OTHER', '256'),
]
DUPLICATE_ROWS = [('Normal Staff 2026-2027', 74, 73)]
ALREADY_IMPORTED = [('Normal Staff 2025-2026', 9, 752)]
EXCLUDED = [
    ('Normal Staff 2024-2025', 5, 'EPF 95 has no DTECH employee record'),
    ('Normal Staff 2024-2025', 6, 'EPF 116 has no DTECH employee record'),
    ('Normal Staff 2025-2026', 5, 'EPF 95 has no DTECH employee record'),
    ('Normal Staff 2025-2026', 8, 'Apply date 2026-02-26 is outside NS period 2025-01-21 to 2026-01-20'),
    ('Normal Staff 2025-2026', 10, 'EPF 135 has no DTECH employee record'),
    ('Normal Staff 2025-2026', 11, 'EPF 135 has no DTECH employee record'),
    ('Normal Staff 2025-2026', 12, 'EPF 135 has no DTECH employee record'),
]


def source_rows():
    raw = SOURCE.read_bytes()
    digest = hashlib.sha256(raw).hexdigest()
    workbook = openpyxl.load_workbook(io.BytesIO(raw), data_only=True, read_only=True)
    try:
        results = []
        for sheet, row_number, epf, period, treatment, category, approved_text in CANDIDATES:
            values = [cell.value for cell in workbook[sheet][row_number]][:11]
            actual_epf = m.text(values[0])
            company = m.text(values[1]).upper()
            staff = m.text(values[2]).upper()
            actual_treatment = m.text(values[3]).upper()
            payment_type = m.text(values[4]).lower()
            actual_category = {'opd': 'OTHER', 'dental': 'DENTAL', 'eye': 'SPECTACLE'}.get(payment_type)
            m.require(actual_epf == epf and company == COMPANY and staff == 'NORMAL STAFF',
                      f'Source employee/company changed at {sheet}:{row_number}')
            m.require(actual_treatment == treatment and actual_category == category,
                      f'Source treatment/category changed at {sheet}:{row_number}')
            m.require(m.text(values[9]).lower() == 'done', f'Unpaid source row {sheet}:{row_number}')

            applied = values[5]
            if isinstance(applied, datetime):
                applied = applied.date()
            elif isinstance(applied, date):
                pass
            else:
                applied = datetime.strptime(m.text(applied), '%Y-%m-%d').date()
            requested = Decimal(str(values[6])).quantize(Decimal('.01'))
            approved = Decimal(str(values[7])).quantize(Decimal('.01'))
            m.require(approved == Decimal(approved_text) and requested >= approved > 0,
                      f'Source amounts changed at {sheet}:{row_number}')
            policy_bounds = {
                10: (date(2024, 1, 21), date(2025, 1, 20)),
                3: (date(2025, 1, 21), date(2026, 1, 20)),
                12: (date(2026, 1, 21), date(2027, 1, 20)),
            }[period]
            m.require(policy_bounds[0] <= applied <= policy_bounds[1],
                      f'Apply date outside selected policy at {sheet}:{row_number}')
            cheque = m.text(values[8])
            source_remark = m.text(values[10])
            request_id = f'TC/HC/DTECH/NS/{applied.year}/{row_number:04d}'
            remark = (f'Imported legacy claim {TAG} SOURCE:{sheet}:{row_number}; '
                      f'cheque_date={cheque}; source_remark={source_remark or "(none)"}; '
                      'treatment date not supplied in source')
            m.require(len(remark) <= 255, f'Remark too long at {sheet}:{row_number}')
            results.append(dict(sheet=sheet, source_row=row_number, epf=epf, period=period,
                                treatment=treatment, category=category, applied=applied,
                                requested=requested, approved=approved, request_id=request_id,
                                remark=remark))
        return results, digest
    finally:
        workbook.close()


def prepare(cursor, rows):
    approved_adds = defaultdict(lambda: Decimal('0'))
    for row in rows:
        users = m.query(cursor, "SELECT au.id,p.id,p.user_status,c.company_type,c.staff_category,c.insurance_policy,"
                         "c.permanent_date FROM application_user au "
                         "JOIN user_personal_details p ON p.id=au.user_personal_details "
                         "JOIN user_company_details c ON c.id=p.user_company_details "
                         "WHERE c.company_type='DTECH' AND p.epf_no=%s", (row['epf'],))
        m.require(len(users) == 1, f'Missing/ambiguous DTECH user EPF {row["epf"]}')
        user = users[0]
        m.require(user[2:6] == ('ACTIVE', 'DTECH', 'NS', 'P-NORMAL'),
                  f'Employee status/category/policy mismatch EPF {row["epf"]}')
        row['user_id'], row['permanent_date'] = user[0], user[6]
        periods = m.query(cursor, "SELECT from_date,to_date,status,staff_category FROM insurance_staff_category_period "
                          "WHERE id=%s", (row['period'],))
        m.require(len(periods) == 1 and periods[0][2:] == ('ACTIVE', 'NS') and
                  periods[0][0] <= row['applied'] <= periods[0][1],
                  f'No matching policy date for EPF {row["epf"]}')
        limits = m.query(cursor, "SELECT id,global_limit,status FROM insurance_details_limit "
                         "WHERE insurance_staff_category_period=%s AND insurance_policy='P-NORMAL' "
                         "AND treatment=%s", (row['period'], row['treatment']))
        m.require(len(limits) == 1 and limits[0][2] == 'ACTIVE',
                  f'Missing active treatment limit for EPF {row["epf"]}')
        row['limit_id'], row['global_limit'] = limits[0][:2]
        quarters = m.query(cursor, "SELECT id,quarter_limit,from_date,to_date FROM insurance_quarter "
                           "WHERE insurance_details_id=%s AND treatment_category_code=%s "
                           "ORDER BY from_date", (row['limit_id'], row['category']))
        m.require(bool(quarters), f'Missing {row["category"]} category limit EPF {row["epf"]}')
        matching = [q for q in quarters if q[2] is not None and q[2] <= row['applied'] <= q[3]]
        m.require(len(matching) == 1, f'Missing/overlapping claim-date quarter EPF {row["epf"]}')
        row['quarter_id'] = matching[0][0]

        m.require(not m.query(cursor, 'SELECT id FROM claims_request WHERE request_id=%s',
                              (row['request_id'],)), f'Request ID already exists {row["request_id"]}')
        m.require(not m.query(cursor, "SELECT cr.id FROM claims_request cr JOIN insurance_claims_details d "
                              "ON d.id=cr.insurance_claims_details WHERE cr.employee=%s "
                              "AND cr.insurance_details_limit_id=%s AND DATE(cr.created_date)=%s "
                              "AND cr.request_amount=%s AND cr.approved_amount=%s "
                              "AND d.treatment=%s AND d.treatment_category=%s",
                              (row['user_id'], row['limit_id'], row['applied'], row['requested'],
                               row['approved'], row['treatment'], row['category'])),
                  f'Possible duplicate already in database at {row["sheet"]}:{row["source_row"]}')

        # The application uses the employee's effective permanent date to
        # select the applicable OTHER quarter fund cap. For legacy users whose
        # permanent date precedes the policy start, it resolves to the first
        # configured quarter. Legacy utilization is validated against approved
        # amounts, as agreed for these migrated claims.
        first_quarter = quarters[0]
        perm = row['permanent_date']
        effective = perm.date() if isinstance(perm, datetime) else perm
        if effective is None or effective < first_quarter[2]:
            effective_quarter = first_quarter
        else:
            in_range = [q for q in quarters if q[2] is not None and q[2] <= effective <= q[3]]
            effective_quarter = in_range[0] if in_range else quarters[-1]
        row['effective_quarter_limit'] = effective_quarter[1]
        row['effective_quarter_id'] = effective_quarter[0]
        key = row['user_id'], row['limit_id'], row['category']
        approved_adds[key] += row['approved']

        prior = m.query(cursor, "SELECT COALESCE(SUM(COALESCE(cr.approved_amount,0)),0) FROM claims_request cr "
                          "JOIN insurance_claims_details d ON d.id=cr.insurance_claims_details "
                          "WHERE cr.employee=%s AND cr.insurance_details_limit_id=%s "
                          "AND cr.request_status='APPROVED' AND d.treatment_category=%s",
                          (row['user_id'], row['limit_id'], row['category']))[0][0]
        m.require(prior + approved_adds[key] <= min(row['global_limit'], row['effective_quarter_limit']),
                  f'Utilization exceeds limit EPF {row["epf"]} period {row["period"]} '
                  f'{row["category"]}: existing={prior}, batch={approved_adds[key]}, '
                  f'global={row["global_limit"]}, effective_quarter={row["effective_quarter_limit"]}')


def apply(cursor, rows):
    inserted = []
    for row in rows:
        timestamp = datetime.combine(row['applied'], time(12, 0))
        detail_id = m.insert(cursor, 'insurance_claims_details', dict(
            created_date=timestamp, last_modified_date=timestamp,
            disease='Legacy DTECH claim import', from_treatment_date=None,
            to_treatment_date=row['applied'], treatment=row['treatment'],
            treatment_category=row['category'], insurance_staff_category_period=row['period']))
        claim_id = m.insert(cursor, 'claims_request', dict(
            created_date=timestamp, last_modified_date=timestamp,
            remark=row['remark'], request_amount=row['requested'],
            request_id=row['request_id'], request_status='APPROVED',
            dependent=None, employee=row['user_id'], insurance_claims_details=detail_id,
            approval_work_flow_id=None, insurance_details_limit_id=row['limit_id'],
            insurance_quarter_id=row['quarter_id'], approval_level='LEVEL02',
            approved_amount=row['approved']))
        inserted.append(dict(epf=row['epf'], period=row['period'], treatment=row['treatment'],
                             category=row['category'], requested=str(row['requested']),
                             approved=str(row['approved']), claim_id=claim_id,
                             request_id=row['request_id'], source=f'{row["sheet"]}:{row["source_row"]}'))
    expected = defaultdict(lambda: Decimal('0'))
    for row in rows:
        expected[row['epf'], row['period'], row['treatment'], row['category']] += row['approved']
    actual_total = m.query(cursor, 'SELECT COUNT(*) FROM claims_request WHERE remark LIKE %s',
                          (f'%{TAG}%',))[0][0]
    m.require(actual_total == len(rows), 'Post-insert claim count mismatch')
    for (epf, period, treatment, category), approved in expected.items():
        actual = m.query(cursor, "SELECT COALESCE(SUM(cr.approved_amount),0) FROM claims_request cr "
                          "JOIN application_user au ON au.id=cr.employee "
                          "JOIN user_personal_details p ON p.id=au.user_personal_details "
                          "JOIN user_company_details co ON co.id=p.user_company_details "
                          "JOIN insurance_claims_details d ON d.id=cr.insurance_claims_details "
                          "WHERE co.company_type='DTECH' AND p.epf_no=%s AND cr.request_status='APPROVED' "
                          "AND d.insurance_staff_category_period=%s AND d.treatment=%s "
                          "AND d.treatment_category=%s AND cr.remark LIKE %s",
                          (epf, period, treatment, category, f'%{TAG}%'))[0][0]
        m.require(actual == approved, f'Post-insert total mismatch EPF {epf}')
    return inserted


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply-digest', help='combined SHA256 value printed by dry run')
    args = parser.parse_args()
    rows, digest = source_rows()
    batch_digest = hashlib.sha256(json.dumps({'source': digest}, sort_keys=True).encode()).hexdigest()
    approved_total = sum((row['approved'] for row in rows), Decimal('0'))
    print(json.dumps(dict(source_sha256=digest, batch_digest=batch_digest,
                          new_claims=len(rows), approved_total=str(approved_total),
                          exact_duplicate_skipped=DUPLICATE_ROWS,
                          already_imported=ALREADY_IMPORTED, unmatched_or_invalid=EXCLUDED)), flush=True)
    if args.apply_digest:
        m.require(args.apply_digest == batch_digest, 'Source workbook changed since preflight')
    with m.connection() as db:
        cursor = db.cursor()
        cursor.execute("SET time_zone='+05:30'")
        db.commit()
        db.start_transaction(readonly=not bool(args.apply_digest))
        try:
            if args.apply_digest:
                m.require(m.query(cursor, "SELECT GET_LOCK('dtech_normal_all_sheets_claims',5)")[0][0] == 1,
                          'Migration lock unavailable')
            m.require(not m.query(cursor, 'SELECT id FROM claims_request WHERE remark LIKE %s',
                                  (f'%{TAG}%',)), 'This migration batch was already applied')
            for table in ('claims_request', 'insurance_claims_details'):
                m.require(m.query(cursor, 'SELECT ENGINE FROM information_schema.TABLES '
                                  'WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s', (m.DATABASE, table)) == [('InnoDB',)],
                          f'Unexpected storage engine {table}')
            prepare(cursor, rows)
            print(f'PREFLIGHT PASS: {len(rows)} DTECH claims; all employee, period, duplicate, '
                  'and available-balance checks passed', flush=True)
            if args.apply_digest:
                saved = apply(cursor, rows)
                db.commit()
                print(json.dumps(dict(result='COMMITTED', claims=saved)), flush=True)
            else:
                db.rollback()
        except Exception:
            db.rollback()
            raise
        finally:
            cursor.close()


if __name__ == '__main__':
    main()
