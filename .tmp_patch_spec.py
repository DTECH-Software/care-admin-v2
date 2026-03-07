from pathlib import Path as P
p = P(r'src/main/java/com/dtech/admin/specifications/EmployeeSummarySpecification.java')
l = p.read_text(encoding='utf-8').splitlines()
l.insert(5, 'import jakarta.persistence.criteria.JoinType;')
l[42:46] = [
    "            if (searchDTO.getPeriodId() != null) {",
    "                Join<Object, Object> detailsLimit = root.join(\"insuranceDetailsLimit\", JoinType.LEFT);",
    "                Join<Object, Object> limitPeriod = detailsLimit.join(\"insuranceStaffCategoryPeriod\", JoinType.LEFT);",
    "                Join<Object, Object> details = root.join(\"insuranceClaimsDetails\", JoinType.LEFT);",
    "                Join<Object, Object> detailsPeriod = details.join(\"insuranceStaffCategoryPeriod\", JoinType.LEFT);",
    "",
    "                predicates.add(cb.equal(",
    "                        cb.coalesce(limitPeriod.get(\"id\"), detailsPeriod.get(\"id\")),",
    "                        searchDTO.getPeriodId()));",
    "            }",
]
p.write_text('\\n'.join(l)+'\\n', encoding='utf-8')
