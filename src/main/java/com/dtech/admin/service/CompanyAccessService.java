package com.dtech.admin.service;

import com.dtech.admin.enums.Status;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.WebUser;
import com.dtech.admin.repository.WebUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyAccessService {
    private final WebUserRepository webUserRepository;

    @Transactional(readOnly = true)
    public List<CompanyTypes> activeCompanies(String username) {
        String effectiveUsername = effectiveUsername(username);
        if (!StringUtils.hasText(effectiveUsername)) return List.of();
        return webUserRepository.findByUsernameAndStatus(effectiveUsername, Status.ACTIVE)
                .map(WebUser::getCompanies)
                .orElseGet(LinkedHashSet::new).stream()
                .filter(company -> company != null && company.getStatus() == Status.ACTIVE)
                .sorted(Comparator.comparing(CompanyTypes::getDescription, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<String> activeCompanyCodes(String username) {
        return activeCompanies(username).stream().map(CompanyTypes::getCode)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional(readOnly = true)
    public boolean canAccess(String username, String companyCode) {
        if (!StringUtils.hasText(companyCode)) return false;
        return activeCompanyCodes(username).stream()
                .anyMatch(code -> code.equalsIgnoreCase(companyCode.trim()));
    }

    private String effectiveUsername(String requestedUsername) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && StringUtils.hasText(authentication.getName())
                && !"anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return authentication.getName().trim();
        }
        return StringUtils.hasText(requestedUsername) ? requestedUsername.trim() : null;
    }
}
