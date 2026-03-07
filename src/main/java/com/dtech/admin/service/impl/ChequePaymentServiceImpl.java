package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.ChequePaymentCreateDTO;
import com.dtech.admin.dto.request.ChequePaymentDocumentDTO;
import com.dtech.admin.dto.request.DocumentUploadRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.ChequePaymentDocumentResponseDTO;
import com.dtech.admin.dto.response.ChequePaymentResponseDTO;
import com.dtech.admin.dto.search.ChequePaymentSearchDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.DocType;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.model.ChequePayment;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.Document;
import com.dtech.admin.model.StaffCategories;
import com.dtech.admin.repository.ChequePaymentRepository;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.DocumentRepository;
import com.dtech.admin.repository.StaffCategoriesRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.ChequePaymentService;
import com.dtech.admin.specifications.ChequePaymentSpecification;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.DateTimeUtil;
import com.dtech.admin.util.PaginationUtil;
import com.dtech.admin.util.ResponseMessageUtil;
import com.dtech.admin.util.ResponseUtil;
import com.dtech.admin.util.MultipartFileUtil;
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
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Log4j2
@RequiredArgsConstructor
public class ChequePaymentServiceImpl implements ChequePaymentService {

    private static final String PAGE_CREATE = "RPLP_CRE";
    private static final String PAGE_VIEW = "RPLP_VIW";

    @Autowired
    private final ChequePaymentRepository chequePaymentRepository;

    @Autowired
    private final CompanyTypeRepository companyTypeRepository;

    @Autowired
    private final StaffCategoriesRepository staffCategoriesRepository;

    @Autowired
    private final DocumentRepository documentRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final AuditLogService auditLogService;

    @Autowired
    private final CommonPrivilegeGetter commonPrivilegeGetter;

    @Autowired
    private final Gson gson;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceData(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Cheque payment reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_VIEW);

            responseMap.put("privileges", privileges);
            responseMap.put("company", loadCompanyTypes());
            responseMap.put("staffCategories", loadStaffCategories());
            responseMap.put("months", buildMonthList());
            responseMap.put("years", buildYearList());

            auditLogService.log(PAGE_VIEW, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.CHEQUE_PAYMENT_REFERENCE_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to load cheque payment reference data", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> create(ChequePaymentCreateDTO createDTO, Locale locale) {
        try {
            log.info("Cheque payment create {}", createDTO);
            List<String> months = normalizeMonths(createDTO.getMonths());
            if (months.isEmpty()) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1001,
                        messageSource.getMessage(ResponseMessageUtil.CHEQUE_PAYMENT_MONTH_REQUIRED, null, locale)));
            }

            ChequePayment payment = new ChequePayment();
            payment.setCompanyCode(createDTO.getCompany());
            payment.setStaffCategoryCode(createDTO.getStaffCategory());
            payment.setYear(createDTO.getYear());
            payment.setMonths(months);
            payment.setChequeNo(createDTO.getChequeNo());
            payment.setChequeBank(createDTO.getChequeBank());
            payment.setChequeBranch(createDTO.getChequeBranch());
            payment.setChequeDate(createDTO.getChequeDate());
            payment.setReceivedDate(createDTO.getReceivedDate());
            payment.setAmount(parseAmount(createDTO.getAmount()));
            payment.setDocuments(uploadDocuments(createDTO.getDocuments()));

            payment = chequePaymentRepository.saveAndFlush(payment);

            Map<String, String> companyDescriptions = loadCompanyDescriptions();
            Map<String, String> staffCategoryDescriptions = loadStaffCategoryDescriptions();
            ChequePaymentResponseDTO responseDTO = mapToResponse(payment, companyDescriptions, staffCategoryDescriptions, true);

            auditLogService.log(PAGE_CREATE, WebTask.ADD.name(),
                    AuditTask.ADD_DATA.getDescription(), createDTO.getIp(),
                    createDTO.getUserAgent(), gson.toJson(responseDTO), null, createDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                    messageSource.getMessage(ResponseMessageUtil.CHEQUE_PAYMENT_CREATED_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to create cheque payment", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ChequePaymentSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Cheque payment filter list {}", paginationRequest);
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            ChequePaymentSearchDTO search = Optional.ofNullable(paginationRequest.getSearch())
                    .orElseGet(ChequePaymentSearchDTO::new);

            Page<ChequePayment> page = chequePaymentRepository.findAll(
                    ChequePaymentSpecification.getSpecification(search), pageable);
            long total = chequePaymentRepository.count(ChequePaymentSpecification.getSpecification(search));

            Map<String, String> companyDescriptions = loadCompanyDescriptions();
            Map<String, String> staffCategoryDescriptions = loadStaffCategoryDescriptions();
            List<ChequePaymentResponseDTO> rows = page.stream()
                    .map(payment -> mapToResponse(payment, companyDescriptions, staffCategoryDescriptions, true))
                    .toList();

            PagingResult<ChequePaymentResponseDTO> result = new PagingResult<>(rows, rows.size(), total);

            auditLogService.log(PAGE_VIEW, WebTask.SEARCH.name(),
                    AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(rows), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) result,
                    messageSource.getMessage(ResponseMessageUtil.CHEQUE_PAYMENT_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter cheque payments", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(ChannelRequestDTO requestDTO, Long id, Locale locale) {
        try {
            log.info("Cheque payment view {}", id);
            return chequePaymentRepository.findById(id).map(payment -> {
                Map<String, String> companyDescriptions = loadCompanyDescriptions();
                Map<String, String> staffCategoryDescriptions = loadStaffCategoryDescriptions();
                ChequePaymentResponseDTO responseDTO = mapToResponse(payment, companyDescriptions, staffCategoryDescriptions, true);

                auditLogService.log(PAGE_VIEW, WebTask.VIEW.name(),
                        AuditTask.VIEW_DATA.getDescription(), requestDTO.getIp(),
                        requestDTO.getUserAgent(), gson.toJson(responseDTO), null, requestDTO.getUsername());

                return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                        messageSource.getMessage(ResponseMessageUtil.CHEQUE_PAYMENT_VIEW_SUCCESS, null, locale)));
            }).orElseGet(() -> ResponseEntity.ok().body(responseUtil.error(null, 404,
                    messageSource.getMessage(ResponseMessageUtil.CHEQUE_PAYMENT_NOT_FOUND, new Object[]{id}, locale))));
        } catch (Exception e) {
            log.error("Failed to view cheque payment {}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> export(PaginationRequest<ChequePaymentSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Cheque payment export {}", paginationRequest);
            ChequePaymentSearchDTO search = paginationRequest.getSearch();
            List<ChequePayment> rows = Objects.nonNull(search)
                    ? chequePaymentRepository.findAll(ChequePaymentSpecification.getSpecification(search))
                    : chequePaymentRepository.findAll(ChequePaymentSpecification.getSpecification());

            Map<String, String> companyDescriptions = loadCompanyDescriptions();
            Map<String, String> staffCategoryDescriptions = loadStaffCategoryDescriptions();
            byte[] excelBytes = buildExcel(rows, companyDescriptions, staffCategoryDescriptions);

            auditLogService.log(PAGE_VIEW, WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(search), null, paginationRequest.getUsername());

            String fileName = "cheque-payment-report.xlsx";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to export cheque payments", e);
            throw e;
        }
    }

    private List<SimpleBaseDTO> loadCompanyTypes() {
        return companyTypeRepository.findAllByStatus(Status.ACTIVE).stream()
                .map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription()))
                .collect(Collectors.toList());
    }

    private List<SimpleBaseDTO> loadStaffCategories() {
        return staffCategoriesRepository.findAllByStatus(Status.ACTIVE).stream()
                .map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription()))
                .collect(Collectors.toList());
    }

    private Map<String, String> loadCompanyDescriptions() {
        return companyTypeRepository.findAll().stream()
                .collect(Collectors.toMap(CompanyTypes::getCode, CompanyTypes::getDescription, (a, b) -> a));
    }

    private Map<String, String> loadStaffCategoryDescriptions() {
        return staffCategoriesRepository.findAll().stream()
                .collect(Collectors.toMap(StaffCategories::getCode, StaffCategories::getDescription, (a, b) -> a));
    }

    private List<SimpleBaseDTO> buildMonthList() {
        return IntStream.rangeClosed(1, 12)
                .mapToObj(Month::of)
                .map(month -> new SimpleBaseDTO(String.format("%02d", month.getValue()), month.name()))
                .collect(Collectors.toList());
    }

    private List<SimpleBaseDTO> buildYearList() {
        int currentYear = DateTimeUtil.getCurrentYear();
        return IntStream.rangeClosed(currentYear - 5, currentYear)
                .mapToObj(String::valueOf)
                .sorted(Comparator.reverseOrder())
                .map(val -> new SimpleBaseDTO(val, val))
                .collect(Collectors.toList());
    }

    private List<String> normalizeMonths(List<String> months) {
        if (months == null) {
            return List.of();
        }
        return months.stream()
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private BigDecimal parseAmount(String amount) {
        if (!hasText(amount)) {
            return null;
        }
        String normalized = amount.replace(",", "");
        return new BigDecimal(normalized);
    }

    private List<Document> uploadDocuments(List<ChequePaymentDocumentDTO> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream()
                .map(this::uploadDocument)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Document uploadDocument(ChequePaymentDocumentDTO doc) {
        if (doc == null || !hasText(doc.getFile()) || !hasText(doc.getFileType()) || !hasText(doc.getFileName())) {
            return null;
        }
        try {
            DocType docType = resolveDocType(doc.getType());
            MultipartFile multipartFile = MultipartFileUtil.convertToMultipartFile(
                    doc.getFile(), doc.getFileType(), doc.getFileName());

            DocumentUploadRequestDTO dto = new DocumentUploadRequestDTO();
            dto.setType(docType.name());
            dto.setDocument(doc.getFile().getBytes());
            dto.setFileName(multipartFile.getOriginalFilename());
            dto.setFileType(multipartFile.getContentType());

            Document document = gson.fromJson(gson.toJson(dto), Document.class);
            document.setType(docType);
            document.setDoc(doc.getFile());
            document = documentRepository.saveAndFlush(document);
            return document;
        } catch (Exception e) {
            log.error("Failed to upload cheque document {}", doc.getFileName(), e);
            throw new RuntimeException(e);
        }
    }

    private DocType resolveDocType(String type) {
        if (!hasText(type)) {
            return DocType.DOCUMENT;
        }
        try {
            return DocType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return DocType.DOCUMENT;
        }
    }

    private ChequePaymentResponseDTO mapToResponse(ChequePayment payment,
                                                   Map<String, String> companyDescriptions,
                                                   Map<String, String> staffCategoryDescriptions,
                                                   boolean includeDocuments) {
        ChequePaymentResponseDTO dto = new ChequePaymentResponseDTO();
        dto.setId(payment.getId());
        dto.setCompanyCode(payment.getCompanyCode());
        dto.setCompanyDescription(companyDescriptions.get(payment.getCompanyCode()));
        dto.setStaffCategoryCode(payment.getStaffCategoryCode());
        dto.setStaffCategoryDescription(staffCategoryDescriptions.get(payment.getStaffCategoryCode()));
        dto.setYear(payment.getYear());
        dto.setMonths(payment.getMonths());
        dto.setMonthDescriptions(buildMonthDescriptions(payment.getMonths()));
        dto.setChequeNo(payment.getChequeNo());
        dto.setChequeBank(payment.getChequeBank());
        dto.setChequeBranch(payment.getChequeBranch());
        dto.setChequeDate(payment.getChequeDate());
        dto.setAmount(payment.getAmount());
        dto.setReceivedDate(payment.getReceivedDate());
        if (includeDocuments) {
            dto.setDocuments(payment.getDocuments().stream().map(this::mapDocument).toList());
        }
        return dto;
    }

    private ChequePaymentDocumentResponseDTO mapDocument(Document document) {
        ChequePaymentDocumentResponseDTO dto = new ChequePaymentDocumentResponseDTO();
        dto.setType(document.getType() != null ? document.getType().name() : null);
        dto.setDoc(document.getDoc());
        dto.setFileName(document.getFileName());
        dto.setFileType(document.getFileType());
        return dto;
    }

    private byte[] buildExcel(List<ChequePayment> rows,
                              Map<String, String> companyDescriptions,
                              Map<String, String> staffCategoryDescriptions) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Cheque Payments");

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            applyBorders(headerStyle);

            CellStyle dataStyle = workbook.createCellStyle();
            applyBorders(dataStyle);

            sheet.setColumnWidth(0, 6 * 256);
            sheet.setColumnWidth(1, 20 * 256);
            sheet.setColumnWidth(2, 20 * 256);
            sheet.setColumnWidth(3, 10 * 256);
            sheet.setColumnWidth(4, 16 * 256);
            sheet.setColumnWidth(5, 16 * 256);
            sheet.setColumnWidth(6, 16 * 256);
            sheet.setColumnWidth(7, 16 * 256);
            sheet.setColumnWidth(8, 14 * 256);
            sheet.setColumnWidth(9, 12 * 256);
            sheet.setColumnWidth(10, 14 * 256);

            int rowIndex = 0;
            Row row = sheet.createRow(rowIndex++);
            Cell titleCell = row.createCell(0);
            titleCell.setCellValue("Cheque Payments");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 10));

            rowIndex++;
            row = sheet.createRow(rowIndex++);
            createStringCell(row, 0, "#", headerStyle);
            createStringCell(row, 1, "Company", headerStyle);
            createStringCell(row, 2, "Staff Category", headerStyle);
            createStringCell(row, 3, "Year", headerStyle);
            createStringCell(row, 4, "Months", headerStyle);
            createStringCell(row, 5, "Cheque No", headerStyle);
            createStringCell(row, 6, "Cheque Bank", headerStyle);
            createStringCell(row, 7, "Cheque Branch", headerStyle);
            createStringCell(row, 8, "Cheque Date", headerStyle);
            createStringCell(row, 9, "Amount", headerStyle);
            createStringCell(row, 10, "Received Date", headerStyle);

            int lineNo = 1;
            for (ChequePayment payment : rows) {
                row = sheet.createRow(rowIndex++);
                String companyCode = payment.getCompanyCode();
                String companyDescription = companyDescriptions.get(companyCode);
                String staffCode = payment.getStaffCategoryCode();
                String staffDescription = staffCategoryDescriptions.get(staffCode);

                createStringCell(row, 0, String.valueOf(lineNo++), dataStyle);
                createStringCell(row, 1, buildDisplay(companyCode, companyDescription), dataStyle);
                createStringCell(row, 2, buildDisplay(staffCode, staffDescription), dataStyle);
                createStringCell(row, 3, payment.getYear(), dataStyle);
                createStringCell(row, 4, String.join(", ", payment.getMonths()), dataStyle);
                createStringCell(row, 5, payment.getChequeNo(), dataStyle);
                createStringCell(row, 6, payment.getChequeBank(), dataStyle);
                createStringCell(row, 7, payment.getChequeBranch(), dataStyle);
                createStringCell(row, 8, formatDate(payment.getChequeDate()), dataStyle);
                createStringCell(row, 9, payment.getAmount() != null ? payment.getAmount().toPlainString() : "", dataStyle);
                createStringCell(row, 10, formatDate(payment.getReceivedDate()), dataStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate cheque payment excel", e);
        }
    }

    private void applyBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void createStringCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private String buildDisplay(String code, String description) {
        if (!hasText(code)) {
            return "";
        }
        if (!hasText(description)) {
            return code;
        }
        return code + " - " + description;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private List<String> buildMonthDescriptions(List<String> months) {
        if (months == null || months.isEmpty()) {
            return List.of();
        }
        return months.stream()
                .map(this::resolveMonthDescription)
                .toList();
    }

    private String resolveMonthDescription(String monthValue) {
        if (!hasText(monthValue)) {
            return "";
        }
        try {
            int month = Integer.parseInt(monthValue.trim());
            return Month.of(month).name();
        } catch (Exception e) {
            return monthValue;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
