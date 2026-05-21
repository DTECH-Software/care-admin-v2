package com.dtech.admin.service.impl;

import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.DailyTaskDdfRowDTO;
import com.dtech.admin.dto.response.DailyTaskMedicalRowDTO;
import com.dtech.admin.dto.response.DailyTaskReportResponseDTO;
import com.dtech.admin.dto.response.DailyTaskReportStageDTO;
import com.dtech.admin.dto.search.DailyTaskReportSearchDTO;
import com.dtech.admin.enums.ApprovalLevel;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.PaymentAdviceType;
import com.dtech.admin.enums.PaymentAttachmentClaimState;
import com.dtech.admin.enums.PaymentAttachmentStatus;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.ChequePayment;
import com.dtech.admin.model.ChequePaymentDdf;
import com.dtech.admin.model.DeathClaimRequest;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.PaymentAdvice;
import com.dtech.admin.model.PaymentAdviceAttachment;
import com.dtech.admin.model.PaymentAdviceDeathClaim;
import com.dtech.admin.model.PaymentAttachment;
import com.dtech.admin.model.PaymentAttachmentClaim;
import com.dtech.admin.repository.ChequePaymentDdfRepository;
import com.dtech.admin.repository.ChequePaymentRepository;
import com.dtech.admin.repository.DeathClaimRequestRepository;
import com.dtech.admin.repository.InsuranceClaimsRequestRepository;
import com.dtech.admin.repository.PaymentAdviceAttachmentRepository;
import com.dtech.admin.repository.PaymentAdviceDeathClaimRepository;
import com.dtech.admin.repository.PaymentAdviceRepository;
import com.dtech.admin.repository.PaymentAttachmentClaimRepository;
import com.dtech.admin.repository.PaymentAttachmentRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.DailyTaskReportService;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.DateTimeUtil;
import com.dtech.admin.util.ResponseMessageUtil;
import com.dtech.admin.util.ResponseUtil;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class DailyTaskReportServiceImpl implements DailyTaskReportService {

    private static final String PAGE_DAILY_TASK_REPORT = WebPage.RPRT_DTR.name();
    private static final String CLAIM_TYPE_ALL = "ALL";
    private static final String CLAIM_TYPE_MEDICAL = "MEDICAL";
    private static final String CLAIM_TYPE_DEATH = "DEATH";
    private static final String MEDICAL_STAFF_TYPE = "All Staff";
    private static final String DDF_STAFF_TYPE = "DDF";
    private static final List<StaffGroup> MEDICAL_STAFF_GROUPS = List.of(
            new StaffGroup("NS", "Normal staff"),
            new StaffGroup("EX-OP1", "Executive Op 01"),
            new StaffGroup("EX-OP2", "Executive Op 02"),
            new StaffGroup("MM", "Middle Management"),
            new StaffGroup("SNR", "Senior staff")
    );

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final CommonPrivilegeGetter commonPrivilegeGetter;

    @Autowired
    private final AuditLogService auditLogService;

    @Autowired
    private final Gson gson;

    @Autowired
    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Autowired
    private final DeathClaimRequestRepository deathClaimRequestRepository;

    @Autowired
    private final PaymentAttachmentClaimRepository paymentAttachmentClaimRepository;

    @Autowired
    private final PaymentAttachmentRepository paymentAttachmentRepository;

    @Autowired
    private final PaymentAdviceAttachmentRepository paymentAdviceAttachmentRepository;

    @Autowired
    private final PaymentAdviceDeathClaimRepository paymentAdviceDeathClaimRepository;

    @Autowired
    private final PaymentAdviceRepository paymentAdviceRepository;

    @Autowired
    private final ChequePaymentRepository chequePaymentRepository;

    @Autowired
    private final ChequePaymentDdfRepository chequePaymentDdfRepository;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Daily task report reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();
            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_DAILY_TASK_REPORT);

            responseMap.put("privileges", privileges);
            responseMap.put("claimTypes", List.of(
                    new SimpleBaseDTO(CLAIM_TYPE_ALL, "All"),
                    new SimpleBaseDTO(CLAIM_TYPE_MEDICAL, "Medical"),
                    new SimpleBaseDTO(CLAIM_TYPE_DEATH, "DDF")
            ));

            auditLogService.log(PAGE_DAILY_TASK_REPORT, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.DAILY_TASK_REPORT_REFERENCE_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to load daily task report reference data", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<DailyTaskReportSearchDTO> paginationRequest,
                                                          Locale locale) {
        try {
            log.info("Daily task report filter list {}", paginationRequest);
            DailyTaskReportResponseDTO responseDTO = buildReport(paginationRequest.getSearch());

            auditLogService.log(PAGE_DAILY_TASK_REPORT, WebTask.SEARCH.name(),
                    AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(responseDTO), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                    messageSource.getMessage(ResponseMessageUtil.DAILY_TASK_REPORT_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter daily task report", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> export(PaginationRequest<DailyTaskReportSearchDTO> paginationRequest,
                                         Locale locale) {
        try {
            log.info("Daily task report export {}", paginationRequest);
            DailyTaskReportResponseDTO responseDTO = buildReport(paginationRequest.getSearch());
            byte[] excelBytes = buildExcel(responseDTO);

            auditLogService.log(PAGE_DAILY_TASK_REPORT, WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(paginationRequest.getSearch()), null,
                    paginationRequest.getUsername());

            String fileName = "daily-task-report-" + responseDTO.getPeriod().replace(" / ", "-") + ".xlsx";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to export daily task report", e);
            throw e;
        }
    }

    private DailyTaskReportResponseDTO buildReport(DailyTaskReportSearchDTO search) {
        DateRange dateRange = resolveDateRange(search);
        String claimType = normalizeClaimType(search);

        DailyTaskReportResponseDTO dto = new DailyTaskReportResponseDTO();
        dto.setPeriod(dateRange.periodText());
        dto.setMedical(CLAIM_TYPE_DEATH.equals(claimType) ? null : buildMedicalRow(dateRange, search));
        dto.setDdf(CLAIM_TYPE_MEDICAL.equals(claimType) ? null : buildDdfRow(dateRange, search));
        return dto;
    }

    private DailyTaskMedicalRowDTO buildMedicalRow(DateRange dateRange, DailyTaskReportSearchDTO search) {
        List<InsuranceClaimsRequest> receivedClaims = insuranceClaimsRequestRepository
                .findAllByCreatedDateBetween(dateRange.startOfDay(), dateRange.endOfDay()).stream()
                .filter(claim -> matchesCompany(claim, search))
                .toList();

        List<InsuranceClaimsRequest> allClaims = insuranceClaimsRequestRepository.findAll().stream()
                .filter(claim -> matchesCompany(claim, search))
                .toList();

        DailyTaskReportStageDTO firstCheckComplete = workflowStage(allClaims, ApprovalLevel.LEVEL01,
                Set.of(Workflow.APPROVED, Workflow.REJECTED), dateRange);

        DailyTaskReportStageDTO haveToCompleteFinalCheck = stageFromStaffSummary(allClaims.stream()
                .filter(claim -> Workflow.UNDER_REVIEW.equals(claim.getRequestStatus()))
                .filter(claim -> Set.of(ApprovalLevel.LEVEL02, ApprovalLevel.LEVEL03).contains(claim.getApprovalLevel()))
                .toList());

        List<InsuranceClaimsRequest> finalDecisionClaims = workflowClaims(allClaims,
                Set.of(ApprovalLevel.LEVEL02, ApprovalLevel.LEVEL03), Set.of(Workflow.APPROVED, Workflow.REJECTED), null);
        DailyTaskReportStageDTO haveToPreparePaymentAttachments = stageFromStaffSummary(finalDecisionClaims.stream()
                .filter(claim -> !paymentAttachmentClaimRepository.existsByInsuranceClaimsRequestAndState(
                        claim, PaymentAttachmentClaimState.ACTIVE))
                .toList());

        List<PaymentAttachmentClaim> createdAttachmentClaims = paymentAttachmentClaimRepository
                .findAllByCreatedDateBetween(dateRange.startOfDay(), dateRange.endOfDay()).stream()
                .filter(claim -> matchesCompany(claim.getInsuranceClaimsRequest(), search))
                .toList();
        DailyTaskReportStageDTO preparePaymentAttachments = stagePaymentAttachments(createdAttachmentClaims);

        List<PaymentAdviceAttachment> adviceAttachments = paymentAdviceAttachmentRepository
                .findAllByCreatedDateBetween(dateRange.startOfDay(), dateRange.endOfDay()).stream()
                .filter(adviceAttachment -> PaymentAdviceType.MEDICAL.equals(adviceAttachment.getPaymentAdvice().getType()))
                .filter(adviceAttachment -> adviceAttachment.getPaymentAttachment().getClaims().stream()
                        .anyMatch(claim -> matchesCompany(claim.getInsuranceClaimsRequest(), search)))
                .toList();
        DailyTaskReportStageDTO paymentsCompleted = stagePaymentAdvices(adviceAttachments);

        List<PaymentAttachmentClaim> pendingPaymentClaims = paymentAttachmentClaimRepository.findAll().stream()
                .filter(claim -> PaymentAttachmentClaimState.ACTIVE.equals(claim.getState()))
                .filter(claim -> matchesCompany(claim.getInsuranceClaimsRequest(), search))
                .filter(claim -> claim.getPaymentAttachment() != null)
                .filter(claim -> !paymentAdviceAttachmentRepository.existsByPaymentAttachment(claim.getPaymentAttachment()))
                .toList();

        DailyTaskMedicalRowDTO row = new DailyTaskMedicalRowDTO();
        row.setStaffType(MEDICAL_STAFF_TYPE);
        row.setDate(dateRange.periodText());
        row.setClaimsReceived(receivedClaims.size());
        row.setClaimsReceivedDetails(formatStaffSummary(countClaimsByStaff(receivedClaims)));
        row.setNotYetProcessed(countUnderReviewAtLevel(allClaims, ApprovalLevel.LEVEL01));
        row.setNotYetProcessedDetails(formatStaffSummary(countClaimsByStaff(allClaims.stream()
                .filter(claim -> Workflow.UNDER_REVIEW.equals(claim.getRequestStatus()))
                .filter(claim -> ApprovalLevel.LEVEL01.equals(claim.getApprovalLevel()))
                .toList())));
        row.setFirstCheckComplete(firstCheckComplete);
        row.setPendingRequirementClaims(0);
        row.setHaveToPreparePaymentAttachments(haveToPreparePaymentAttachments);
        row.setPreparePaymentAttachments(preparePaymentAttachments);
        row.setHaveToHandoverForFinalCheck(emptyStage());
        row.setHandoverForFinalCheck(emptyStage());
        row.setHaveToCompleteFinalCheck(haveToCompleteFinalCheck);
        row.setFinalCheckComplete(workflowStage(allClaims,
                Set.of(ApprovalLevel.LEVEL02, ApprovalLevel.LEVEL03), Set.of(Workflow.APPROVED, Workflow.REJECTED), dateRange));
        row.setHaveToInputToCurrentSystem(emptyStage());
        row.setInputToCurrentSystem(emptyStage());
        row.setHaveToPaymentsComplete(stageFromStaffSummary(pendingPaymentClaims.stream()
                .map(PaymentAttachmentClaim::getInsuranceClaimsRequest)
                .filter(Objects::nonNull)
                .toList()));
        row.setPaymentsCompleted(paymentsCompleted);
        row.setOtherWorks(search != null ? search.getMedicalOtherWorks() : null);
        return row;
    }

    private DailyTaskDdfRowDTO buildDdfRow(DateRange dateRange, DailyTaskReportSearchDTO search) {
        List<DeathClaimRequest> receivedClaims = deathClaimRequestRepository
                .findAllByCreatedDateBetween(dateRange.startOfDay(), dateRange.endOfDay()).stream()
                .filter(claim -> matchesCompany(claim, search))
                .toList();

        List<DeathClaimRequest> allClaims = deathClaimRequestRepository.findAll().stream()
                .filter(claim -> matchesCompany(claim, search))
                .toList();

        DailyTaskReportStageDTO firstCheckComplete = workflowStageDeath(allClaims, ApprovalLevel.LEVEL01,
                Set.of(Workflow.APPROVED, Workflow.REJECTED), dateRange);
        long pendingRequirementClaims = workflowClaimsDeath(allClaims, ApprovalLevel.LEVEL01,
                Set.of(Workflow.REJECTED), dateRange).size();
        DailyTaskReportStageDTO haveToCompleteFinalCheck = stageFromDeathClaims(receivedClaims.stream()
                .filter(claim -> Workflow.UNDER_REVIEW.equals(claim.getRequestStatus()))
                .filter(claim -> ApprovalLevel.LEVEL02.equals(claim.getApprovalLevel()))
                .toList());
        DailyTaskReportStageDTO finalCheckComplete = workflowStageDeath(allClaims,
                Set.of(ApprovalLevel.LEVEL02), Set.of(Workflow.APPROVED, Workflow.REJECTED), dateRange);

        List<DeathClaimRequest> approvedClaims = workflowClaimsDeath(allClaims,
                Set.of(ApprovalLevel.LEVEL02), Set.of(Workflow.APPROVED), null);
        List<DeathClaimRequest> paymentPendingClaims = approvedClaims.stream()
                .filter(claim -> !paymentAdviceDeathClaimRepository.existsByDeathClaim(claim))
                .toList();
        DailyTaskReportStageDTO haveToHandoverToAuthorizedPerson = stageFromIds(paymentPendingClaims.stream()
                .map(DeathClaimRequest::getRequestId)
                .toList());

        List<PaymentAdviceDeathClaim> adviceClaims = paymentAdviceDeathClaimRepository
                .findAllByCreatedDateBetween(dateRange.startOfDay(), dateRange.endOfDay()).stream()
                .filter(adviceClaim -> matchesCompany(adviceClaim.getDeathClaim(), search))
                .toList();
        DailyTaskReportStageDTO handoverToAuthorizedPerson = stageByUser(adviceClaims.stream()
                .map(adviceClaim -> new StageEntry(resolveCreatedUser(adviceClaim.getPaymentAdvice()),
                        adviceClaim.getRequestId()))
                .toList());

        List<PaymentAdvice> deathAdvices = paymentAdviceRepository
                .findAllByCreatedDateBetweenAndType(dateRange.startOfDay(), dateRange.endOfDay(), PaymentAdviceType.DEATH);
        DailyTaskReportStageDTO paymentAdviceChecked = stageByUser(deathAdvices.stream()
                .map(advice -> new StageEntry(resolveCreatedUser(advice), advice.getAdviceNo()))
                .toList());

        List<ChequePaymentDdf> ddfChequePayments = chequePaymentDdfRepository
                .findAllByCreatedDateBetween(dateRange.startOfDay(), dateRange.endOfDay());
        DailyTaskReportStageDTO paymentsCompleted = stageByUser(ddfChequePayments.stream()
                .map(payment -> new StageEntry(resolveCreatedUser(payment), payment.getChequeNo()))
                .toList());

        DailyTaskDdfRowDTO row = new DailyTaskDdfRowDTO();
        row.setStaffType(DDF_STAFF_TYPE);
        row.setDate(dateRange.periodText());
        row.setClaimsReceived(receivedClaims.size());
        row.setClaimsReceivedDetails(formatCount(receivedClaims.size()));
        row.setNotYetProcessed(countUnderReviewAtLevelDeath(allClaims, ApprovalLevel.LEVEL01));
        row.setNotYetProcessedDetails(formatCount(row.getNotYetProcessed()));
        row.setFirstCheckComplete(firstCheckComplete);
        row.setPendingRequirementClaims(pendingRequirementClaims);
        row.setHaveToHandoverToAuthorizedPerson(haveToHandoverToAuthorizedPerson);
        row.setHandoverToAuthorizedPerson(handoverToAuthorizedPerson);
        row.setHaveToHandoverToFinalCheck(emptyStage());
        row.setHandoverToFinalCheck(paymentAdviceChecked);
        row.setHaveToCompleteFinalCheck(haveToCompleteFinalCheck);
        row.setFinalCheckComplete(finalCheckComplete);
        row.setHaveToPreparePayment(stageFromDeathClaims(paymentPendingClaims));
        row.setHaveToCheckedPaymentAdviceAndFundTransfer(new DailyTaskReportStageDTO(Math.max(0, deathAdvices.size() - ddfChequePayments.size()), ""));
        row.setPaymentAdviceAndFundTransferChecked(paymentAdviceChecked);
        row.setReturnedClaims(receivedClaims.stream().filter(claim -> Workflow.REJECTED.equals(claim.getRequestStatus())).count());
        row.setHaveToPaymentsCompleted(new DailyTaskReportStageDTO(Math.max(0, deathAdvices.size() - ddfChequePayments.size()), ""));
        row.setPaymentsCompleted(paymentsCompleted);
        row.setOtherWorks(search != null ? search.getDdfOtherWorks() : null);
        return row;
    }

    private DailyTaskMedicalRowDTO emptyMedical(DateRange dateRange, DailyTaskReportSearchDTO search) {
        DailyTaskMedicalRowDTO row = new DailyTaskMedicalRowDTO();
        row.setStaffType(MEDICAL_STAFF_TYPE);
        row.setDate(dateRange.periodText());
        row.setClaimsReceivedDetails(formatStaffSummary(emptyStaffCountMap()));
        row.setNotYetProcessedDetails(formatStaffSummary(emptyStaffCountMap()));
        row.setFirstCheckComplete(emptyStage());
        row.setHaveToPreparePaymentAttachments(emptyStage());
        row.setPreparePaymentAttachments(emptyStage());
        row.setHaveToHandoverForFinalCheck(emptyStage());
        row.setHandoverForFinalCheck(emptyStage());
        row.setHaveToCompleteFinalCheck(emptyStage());
        row.setFinalCheckComplete(emptyStage());
        row.setHaveToInputToCurrentSystem(emptyStage());
        row.setInputToCurrentSystem(emptyStage());
        row.setHaveToPaymentsComplete(emptyStage());
        row.setPaymentsCompleted(emptyStage());
        row.setOtherWorks(search != null ? search.getMedicalOtherWorks() : null);
        return row;
    }

    private DailyTaskDdfRowDTO emptyDdf(DateRange dateRange, DailyTaskReportSearchDTO search) {
        DailyTaskDdfRowDTO row = new DailyTaskDdfRowDTO();
        row.setStaffType(DDF_STAFF_TYPE);
        row.setDate(dateRange.periodText());
        row.setClaimsReceivedDetails(formatCount(0));
        row.setNotYetProcessedDetails(formatCount(0));
        row.setFirstCheckComplete(emptyStage());
        row.setHaveToHandoverToAuthorizedPerson(emptyStage());
        row.setHandoverToAuthorizedPerson(emptyStage());
        row.setHaveToHandoverToFinalCheck(emptyStage());
        row.setHandoverToFinalCheck(emptyStage());
        row.setHaveToCompleteFinalCheck(emptyStage());
        row.setFinalCheckComplete(emptyStage());
        row.setHaveToPreparePayment(emptyStage());
        row.setHaveToCheckedPaymentAdviceAndFundTransfer(emptyStage());
        row.setPaymentAdviceAndFundTransferChecked(emptyStage());
        row.setHaveToPaymentsCompleted(emptyStage());
        row.setPaymentsCompleted(emptyStage());
        row.setOtherWorks(search != null ? search.getDdfOtherWorks() : null);
        return row;
    }

    private DailyTaskReportStageDTO workflowStage(List<InsuranceClaimsRequest> claims,
                                                  ApprovalLevel level,
                                                  Set<Workflow> statuses,
                                                  DateRange dateRange) {
        return workflowStage(claims, Set.of(level), statuses, dateRange);
    }

    private DailyTaskReportStageDTO workflowStage(List<InsuranceClaimsRequest> claims,
                                                  Set<ApprovalLevel> levels,
                                                  Set<Workflow> statuses,
                                                  DateRange dateRange) {
        return stageByUser(claims.stream()
                .flatMap(claim -> matchingWorkflows(claim.getApprovalWorkFlows(), levels, statuses, dateRange).stream()
                        .map(workflow -> new StageEntry(resolveApprovedUser(workflow), claim.getRequestId())))
                .toList());
    }

    private List<InsuranceClaimsRequest> workflowClaims(List<InsuranceClaimsRequest> claims,
                                                        Set<ApprovalLevel> levels,
                                                        Set<Workflow> statuses,
                                                        DateRange dateRange) {
        return claims.stream()
                .filter(claim -> !matchingWorkflows(claim.getApprovalWorkFlows(), levels, statuses, dateRange).isEmpty())
                .toList();
    }

    private List<InsuranceClaimsRequest> workflowClaims(List<InsuranceClaimsRequest> claims,
                                                        ApprovalLevel level,
                                                        Set<Workflow> statuses,
                                                        DateRange dateRange) {
        return workflowClaims(claims, Set.of(level), statuses, dateRange);
    }

    private DailyTaskReportStageDTO workflowStageDeath(List<DeathClaimRequest> claims,
                                                       ApprovalLevel level,
                                                       Set<Workflow> statuses,
                                                       DateRange dateRange) {
        return workflowStageDeath(claims, Set.of(level), statuses, dateRange);
    }

    private DailyTaskReportStageDTO workflowStageDeath(List<DeathClaimRequest> claims,
                                                       Set<ApprovalLevel> levels,
                                                       Set<Workflow> statuses,
                                                       DateRange dateRange) {
        return stageByUser(claims.stream()
                .flatMap(claim -> matchingWorkflows(claim.getApprovalWorkFlows(), levels, statuses, dateRange).stream()
                        .map(workflow -> new StageEntry(resolveApprovedUser(workflow), claim.getRequestId())))
                .toList());
    }

    private List<DeathClaimRequest> workflowClaimsDeath(List<DeathClaimRequest> claims,
                                                       Set<ApprovalLevel> levels,
                                                       Set<Workflow> statuses,
                                                       DateRange dateRange) {
        return claims.stream()
                .filter(claim -> !matchingWorkflows(claim.getApprovalWorkFlows(), levels, statuses, dateRange).isEmpty())
                .toList();
    }

    private List<DeathClaimRequest> workflowClaimsDeath(List<DeathClaimRequest> claims,
                                                       ApprovalLevel level,
                                                       Set<Workflow> statuses,
                                                       DateRange dateRange) {
        return workflowClaimsDeath(claims, Set.of(level), statuses, dateRange);
    }

    private List<ApprovalWorkFlow> matchingWorkflows(List<ApprovalWorkFlow> workflows,
                                                    Set<ApprovalLevel> levels,
                                                    Set<Workflow> statuses,
                                                    DateRange dateRange) {
        return Objects.requireNonNullElse(workflows, List.<ApprovalWorkFlow>of()).stream()
                .filter(workflow -> levels.contains(workflow.getApprovalLevel()))
                .filter(workflow -> statuses.contains(workflow.getStatus()))
                .filter(workflow -> dateRange == null || isBetween(workflow.getApprovedDate(), dateRange))
                .toList();
    }

    private long countUnderReviewAtLevel(List<InsuranceClaimsRequest> claims, ApprovalLevel level) {
        return claims.stream()
                .filter(claim -> Workflow.UNDER_REVIEW.equals(claim.getRequestStatus()))
                .filter(claim -> level.equals(claim.getApprovalLevel()))
                .count();
    }

    private long countUnderReviewAtLevelDeath(List<DeathClaimRequest> claims, ApprovalLevel level) {
        return claims.stream()
                .filter(claim -> Workflow.UNDER_REVIEW.equals(claim.getRequestStatus()))
                .filter(claim -> level.equals(claim.getApprovalLevel()))
                .count();
    }

    private DailyTaskReportStageDTO stageByUser(List<StageEntry> entries) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        entries.stream()
                .filter(entry -> hasText(entry.value()))
                .sorted(Comparator.comparing(StageEntry::user).thenComparing(StageEntry::value))
                .forEach(entry -> grouped.computeIfAbsent(entry.user(), ignored -> new ArrayList<>()).add(entry.value()));

        String details = grouped.entrySet().stream()
                .map(entry -> {
                    List<String> values = entry.getValue().stream().distinct().toList();
                    return entry.getKey() + " - " + values.size() + " (" + String.join(", ", values) + ")";
                })
                .collect(Collectors.joining("\n"));
        long count = grouped.values().stream().mapToLong(values -> values.stream().distinct().count()).sum();
        return new DailyTaskReportStageDTO(count, details);
    }

    private DailyTaskReportStageDTO stageFromIds(List<String> ids) {
        List<String> distinct = ids.stream()
                .filter(this::hasText)
                .distinct()
                .sorted()
                .toList();
        return new DailyTaskReportStageDTO(distinct.size(), distinct.isEmpty() ? "" : "(" + String.join(", ", distinct) + ")");
    }

    private DailyTaskReportStageDTO stageFromStaffSummary(List<InsuranceClaimsRequest> claims) {
        Map<String, Long> counts = countClaimsByStaff(claims);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new DailyTaskReportStageDTO(total, formatStaffSummary(counts));
    }

    private DailyTaskReportStageDTO stageFromStaffSummaryDeath(List<DeathClaimRequest> claims) {
        Map<String, Long> counts = countDeathClaimsByStaff(claims);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new DailyTaskReportStageDTO(total, formatStaffSummary(counts));
    }

    private DailyTaskReportStageDTO stageFromDeathClaims(List<DeathClaimRequest> claims) {
        long count = Objects.requireNonNullElse(claims, List.<DeathClaimRequest>of()).size();
        return new DailyTaskReportStageDTO(count, "");
    }

    private Map<String, Long> countClaimsByStaff(List<InsuranceClaimsRequest> claims) {
        Map<String, Long> counts = emptyStaffCountMap();
        Objects.requireNonNullElse(claims, List.<InsuranceClaimsRequest>of()).forEach(claim -> {
            String label = resolveStaffLabel(resolveStaffCategoryCode(claim));
            counts.computeIfPresent(label, (key, value) -> value + 1);
        });
        return counts;
    }

    private Map<String, Long> countDeathClaimsByStaff(List<DeathClaimRequest> claims) {
        Map<String, Long> counts = emptyStaffCountMap();
        Objects.requireNonNullElse(claims, List.<DeathClaimRequest>of()).forEach(claim -> {
            String label = resolveStaffLabel(resolveStaffCategoryCode(claim));
            counts.computeIfPresent(label, (key, value) -> value + 1);
        });
        return counts;
    }

    private Map<String, Long> emptyStaffCountMap() {
        Map<String, Long> counts = new LinkedHashMap<>();
        MEDICAL_STAFF_GROUPS.forEach(group -> counts.put(group.label(), 0L));
        return counts;
    }

    private String formatStaffSummary(Map<String, Long> counts) {
        Map<String, Long> safeCounts = counts != null ? counts : emptyStaffCountMap();
        return MEDICAL_STAFF_GROUPS.stream()
                .map(group -> group.label() + ": " + safeCounts.getOrDefault(group.label(), 0L))
                .collect(Collectors.joining("\n"));
    }

    private String resolveStaffCategoryCode(InsuranceClaimsRequest claim) {
        if (claim == null) {
            return null;
        }
        if (claim.getInsuranceDetailsLimit() != null
                && claim.getInsuranceDetailsLimit().getInsuranceStaffCategoryPeriod() != null
                && claim.getInsuranceDetailsLimit().getInsuranceStaffCategoryPeriod().getStaffCategories() != null) {
            return claim.getInsuranceDetailsLimit().getInsuranceStaffCategoryPeriod().getStaffCategories().getCode();
        }
        if (claim.getInsuranceClaimsDetails() != null
                && claim.getInsuranceClaimsDetails().getInsuranceStaffCategoryPeriod() != null
                && claim.getInsuranceClaimsDetails().getInsuranceStaffCategoryPeriod().getStaffCategories() != null) {
            return claim.getInsuranceClaimsDetails().getInsuranceStaffCategoryPeriod().getStaffCategories().getCode();
        }
        if (claim.getEmployee() != null
                && claim.getEmployee().getUserPersonalDetails() != null
                && claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails() != null
                && claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getStaffCategories() != null) {
            return claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode();
        }
        return null;
    }

    private String resolveStaffCategoryCode(DeathClaimRequest claim) {
        if (claim == null
                || claim.getEmployee() == null
                || claim.getEmployee().getUserPersonalDetails() == null
                || claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails() == null
                || claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getStaffCategories() == null) {
            return null;
        }
        return claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode();
    }

    private String resolveStaffLabel(String staffCategoryCode) {
        if (!hasText(staffCategoryCode)) {
            return MEDICAL_STAFF_GROUPS.get(0).label();
        }
        String normalized = staffCategoryCode.trim().toUpperCase(Locale.ROOT);
        return MEDICAL_STAFF_GROUPS.stream()
                .filter(group -> group.code().equalsIgnoreCase(normalized))
                .map(StaffGroup::label)
                .findFirst()
                .orElse(MEDICAL_STAFF_GROUPS.get(0).label());
    }

    private DailyTaskReportStageDTO stagePaymentAttachments(List<PaymentAttachmentClaim> claims) {
        Map<String, Map<String, Long>> grouped = new LinkedHashMap<>();
        Objects.requireNonNullElse(claims, List.<PaymentAttachmentClaim>of()).stream()
                .filter(claim -> claim.getPaymentAttachment() != null)
                .forEach(claim -> {
                    String user = resolveCreatedUser(claim.getPaymentAttachment());
                    String attachmentNo = claim.getPaymentAttachment().getAttachmentNo();
                    if (!hasText(attachmentNo)) {
                        attachmentNo = claim.getRequestId();
                    }
                    if (!hasText(attachmentNo)) {
                        return;
                    }
                    grouped.computeIfAbsent(user, ignored -> new LinkedHashMap<>())
                            .merge(attachmentNo, 1L, Long::sum);
                });
        return stageFromGroupedReferenceCounts(grouped);
    }

    private DailyTaskReportStageDTO stagePaymentAdvices(List<PaymentAdviceAttachment> adviceAttachments) {
        Map<String, Map<String, Long>> grouped = new LinkedHashMap<>();
        Objects.requireNonNullElse(adviceAttachments, List.<PaymentAdviceAttachment>of()).stream()
                .filter(adviceAttachment -> adviceAttachment.getPaymentAdvice() != null)
                .filter(adviceAttachment -> adviceAttachment.getPaymentAttachment() != null)
                .forEach(adviceAttachment -> {
                    String user = resolveCreatedUser(adviceAttachment.getPaymentAdvice());
                    String adviceNo = adviceAttachment.getPaymentAdvice().getAdviceNo();
                    if (!hasText(adviceNo)) {
                        adviceNo = adviceAttachment.getAttachmentNo();
                    }
                    long claimCount = adviceAttachment.getPaymentAttachment().getClaims() != null
                            ? adviceAttachment.getPaymentAttachment().getClaims().stream()
                            .filter(claim -> PaymentAttachmentClaimState.ACTIVE.equals(claim.getState()))
                            .count()
                            : 1L;
                    grouped.computeIfAbsent(user, ignored -> new LinkedHashMap<>())
                            .merge(adviceNo, Math.max(1L, claimCount), Long::sum);
                });
        return stageFromGroupedReferenceCounts(grouped);
    }

    private DailyTaskReportStageDTO stageFromGroupedReferenceCounts(Map<String, Map<String, Long>> grouped) {
        long total = grouped.values().stream()
                .flatMap(referenceCounts -> referenceCounts.values().stream())
                .mapToLong(Long::longValue)
                .sum();
        String details = grouped.entrySet().stream()
                .map(entry -> entry.getKey() + " - " + entry.getValue().values().stream().mapToLong(Long::longValue).sum()
                        + " (" + String.join(", ", entry.getValue().keySet()) + ")")
                .collect(Collectors.joining("\n"));
        return new DailyTaskReportStageDTO(total, details);
    }

    private DailyTaskReportStageDTO emptyStage() {
        return new DailyTaskReportStageDTO(0, "");
    }

    private boolean matchesCompany(InsuranceClaimsRequest claim, DailyTaskReportSearchDTO search) {
        if (claim == null || search == null || !hasText(search.getCompanyCode())) {
            return true;
        }
        return claim.getEmployee() != null
                && claim.getEmployee().getUserPersonalDetails() != null
                && claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails() != null
                && claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes() != null
                && search.getCompanyCode().equalsIgnoreCase(
                claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode());
    }

    private boolean matchesCompany(DeathClaimRequest claim, DailyTaskReportSearchDTO search) {
        if (claim == null || search == null || !hasText(search.getCompanyCode())) {
            return true;
        }
        return claim.getEmployee() != null
                && claim.getEmployee().getUserPersonalDetails() != null
                && claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails() != null
                && claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes() != null
                && search.getCompanyCode().equalsIgnoreCase(
                claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode());
    }

    private String resolveApprovedUser(ApprovalWorkFlow workflow) {
        return hasText(workflow.getApprovedUser()) ? workflow.getApprovedUser() : "Unknown";
    }

    private String resolveCreatedUser(Object entity) {
        if (entity instanceof PaymentAttachment attachment && hasText(attachment.getCreatedBy())) {
            return attachment.getCreatedBy();
        }
        if (entity instanceof PaymentAdvice advice && hasText(advice.getCreatedBy())) {
            return advice.getCreatedBy();
        }
        if (entity instanceof ChequePayment payment && hasText(payment.getCreatedBy())) {
            return payment.getCreatedBy();
        }
        if (entity instanceof ChequePaymentDdf payment && hasText(payment.getCreatedBy())) {
            return payment.getCreatedBy();
        }
        return "Unknown";
    }

    private String resolveLastModifiedUser(PaymentAttachment attachment) {
        return hasText(attachment.getLastModifiedBy()) ? attachment.getLastModifiedBy() : resolveCreatedUser(attachment);
    }

    private boolean isBetween(Date date, DateRange range) {
        return date != null && !date.before(range.startOfDay()) && !date.after(range.endOfDay());
    }

    private DateRange resolveDateRange(DailyTaskReportSearchDTO search) {
        if (search == null || !hasText(search.getFromDate()) || !hasText(search.getToDate())) {
            throw new IllegalArgumentException("fromDate and toDate are required");
        }
        try {
            Date start = DateTimeUtil.getStartOfDay(normalizeDate(search.getFromDate()));
            Date end = DateTimeUtil.getEndOfDay(normalizeDate(search.getToDate()));
            if (start.after(end)) {
                throw new IllegalArgumentException("fromDate must be before or equal to toDate");
            }
            return new DateRange(start, end);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date range", e);
        }
    }

    private String normalizeClaimType(DailyTaskReportSearchDTO search) {
        if (search == null || !hasText(search.getClaimType())) {
            return CLAIM_TYPE_ALL;
        }
        String claimType = search.getClaimType().trim().toUpperCase(Locale.ROOT);
        if ("DDF".equals(claimType)) {
            return CLAIM_TYPE_DEATH;
        }
        return Set.of(CLAIM_TYPE_ALL, CLAIM_TYPE_MEDICAL, CLAIM_TYPE_DEATH).contains(claimType)
                ? claimType
                : CLAIM_TYPE_ALL;
    }

    private String normalizeDate(String value) {
        return value.contains("-") ? value.replace("-", "/") : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private byte[] buildExcel(DailyTaskReportResponseDTO report) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Daily Task Report");
            sheet.setDefaultRowHeightInPoints(20);

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle sectionStyle = createSectionStyle(workbook);
            CellStyle medicalHeaderStyle = createHeaderStyle(workbook, IndexedColors.LAVENDER.getIndex());
            CellStyle ddfHeaderStyle = createHeaderStyle(workbook, IndexedColors.LAVENDER.getIndex());
            CellStyle staffStyle = createStaffStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);

            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.setHeightInPoints(24);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DAILY TASK REPORT");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 18));

            rowIndex++;
            if (report.getMedical() != null) {
                rowIndex = writeMedicalSection(sheet, rowIndex, report.getMedical(), sectionStyle, medicalHeaderStyle, staffStyle, bodyStyle);
                rowIndex += 2;
            }
            if (report.getDdf() != null) {
                writeDdfSection(sheet, rowIndex, report.getDdf(), sectionStyle, ddfHeaderStyle, staffStyle, bodyStyle);
            }

            int[] widths = {12, 16, 12, 14, 42, 18, 34, 42, 16, 18, 16, 28, 14, 14, 16, 16, 26, 16, 26};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build daily task report Excel", e);
        }
    }

    private int writeMedicalSection(Sheet sheet,
                                    int startRow,
                                    DailyTaskMedicalRowDTO rowDTO,
                                    CellStyle sectionStyle,
                                    CellStyle headerStyle,
                                    CellStyle staffStyle,
                                    CellStyle bodyStyle) {
        Row sectionRow = sheet.createRow(startRow++);
        Cell sectionCell = sectionRow.createCell(0);
        sectionCell.setCellValue("Medical (ALL STAFF )");
        sectionCell.setCellStyle(sectionStyle);

        Row headerRow = sheet.createRow(startRow++);
        headerRow.setHeightInPoints(55);
        String[] headers = {
                "Staff Type", "Date", "Claims Received", "Not Yet Processed", "1st Check Complete",
                "Have to complete final check", "Final check complete", "Have to Prepare Payment attachments",
                "Prepare Payment attachments", "Have to Payments Completed", "Payments Completed", "Other Works"
        };
        writeHeaders(headerRow, headers, headerStyle);

        Row row = sheet.createRow(startRow++);
        row.setHeightInPoints(110);
        writeCell(row, 0, rowDTO.getStaffType(), staffStyle);
        writeCell(row, 1, rowDTO.getDate(), bodyStyle);
        writeCell(row, 2, rowDTO.getClaimsReceivedDetails(), bodyStyle);
        writeCell(row, 3, rowDTO.getNotYetProcessedDetails(), bodyStyle);
        writeCell(row, 4, stageText(rowDTO.getFirstCheckComplete()), bodyStyle);
        writeCell(row, 5, stageText(rowDTO.getHaveToCompleteFinalCheck()), bodyStyle);
        writeCell(row, 6, stageText(rowDTO.getFinalCheckComplete()), bodyStyle);
        writeCell(row, 7, stageText(rowDTO.getHaveToPreparePaymentAttachments()), bodyStyle);
        writeCell(row, 8, stageText(rowDTO.getPreparePaymentAttachments()), bodyStyle);
        writeCell(row, 9, stageText(rowDTO.getHaveToPaymentsComplete()), bodyStyle);
        writeCell(row, 10, stageText(rowDTO.getPaymentsCompleted()), bodyStyle);
        writeCell(row, 11, rowDTO.getOtherWorks(), bodyStyle);
        return startRow;
    }

    private int writeDdfSection(Sheet sheet,
                                int startRow,
                                DailyTaskDdfRowDTO rowDTO,
                                CellStyle sectionStyle,
                                CellStyle headerStyle,
                                CellStyle staffStyle,
                                CellStyle bodyStyle) {
        Row sectionRow = sheet.createRow(startRow++);
        Cell sectionCell = sectionRow.createCell(0);
        sectionCell.setCellValue("DDF");
        sectionCell.setCellStyle(sectionStyle);

        Row headerRow = sheet.createRow(startRow++);
        headerRow.setHeightInPoints(55);
        String[] headers = {
                "Staff Type", "Date", "Claims Received", "Not Yet Processed", "1st Check Complete",
                "Have to complete final check", "Final check complete", "Have to Prepare Payment",
                "Prepare Payment", "Have to Payments Completed", "Payments Completed", "Other Works"
        };
        writeHeaders(headerRow, headers, headerStyle);

        Row row = sheet.createRow(startRow++);
        row.setHeightInPoints(110);
        writeCell(row, 0, rowDTO.getStaffType(), staffStyle);
        writeCell(row, 1, rowDTO.getDate(), bodyStyle);
        writeCell(row, 2, rowDTO.getClaimsReceivedDetails(), bodyStyle);
        writeCell(row, 3, rowDTO.getNotYetProcessedDetails(), bodyStyle);
        writeCell(row, 4, stageText(rowDTO.getFirstCheckComplete()), bodyStyle);
        writeCell(row, 5, stageText(rowDTO.getHaveToCompleteFinalCheck()), bodyStyle);
        writeCell(row, 6, stageText(rowDTO.getFinalCheckComplete()), bodyStyle);
        writeCell(row, 7, stageText(rowDTO.getHaveToPreparePayment()), bodyStyle);
        writeCell(row, 8, stageText(rowDTO.getHandoverToAuthorizedPerson()), bodyStyle);
        writeCell(row, 9, stageText(rowDTO.getHaveToPaymentsCompleted()), bodyStyle);
        writeCell(row, 10, stageText(rowDTO.getPaymentsCompleted()), bodyStyle);
        writeCell(row, 11, rowDTO.getOtherWorks(), bodyStyle);
        return startRow;
    }

    private void writeHeaders(Row row, String[] headers, CellStyle style) {
        for (int i = 0; i < headers.length; i++) {
            writeCell(row, i, headers[i], style);
        }
    }

    private void writeCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private String stageText(DailyTaskReportStageDTO stage) {
        if (stage == null || stage.getCount() == 0) {
            return "00";
        }
        return hasText(stage.getDetails()) ? stage.getDetails() : String.valueOf(stage.getCount());
    }

    private String formatCount(long count) {
        return count == 0 ? "00" : String.valueOf(count);
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setUnderline(Font.U_SINGLE);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createSectionStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setUnderline(Font.U_SINGLE);
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook, short color) {
        CellStyle style = createBorderedStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 8);
        style.setFont(font);
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createStaffStyle(Workbook workbook) {
        CellStyle style = createBodyStyle(workbook);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createBodyStyle(Workbook workbook) {
        CellStyle style = createBorderedStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createBorderedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private record StageEntry(String user, String value) {
        private StageEntry {
            user = user != null && !user.isBlank() ? user : "Unknown";
        }
    }

    private record StaffGroup(String code, String label) {
    }

    private record DateRange(Date startOfDay, Date endOfDay) {
        private String periodText() {
            return formatDate(startOfDay) + " / " + formatDate(endOfDay);
        }

        private static String formatDate(Date date) {
            return date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toString();
        }
    }
}
