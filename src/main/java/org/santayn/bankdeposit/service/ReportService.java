package org.santayn.bankdeposit.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositContractStatus;
import org.santayn.bankdeposit.models.DepositOperation;
import org.santayn.bankdeposit.models.DepositOperationType;
import org.santayn.bankdeposit.models.DepositProduct;
import org.santayn.bankdeposit.repository.CustomerRepository;
import org.santayn.bankdeposit.repository.DepositOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для построения отчётов по вкладам и операциям.
 *
 * Дополнительно:
 * - Экспорт отчётов в Excel (.xlsx) через Apache POI.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final CustomerRepository customerRepository;
    private final DepositContractService depositContractService;
    private final DepositOperationRepository depositOperationRepository;

    /**
     * Возвращает список договоров выбранного клиента.
     * Включает как открытые, так и закрытые вклады.
     */
    @Transactional(readOnly = true)
    public List<DepositContract> getContractsByCustomer(Long customerId) {
        return depositContractService.getContractsByCustomer(customerId);
    }

    /**
     * Возвращает только активные (открытые) вклады клиента.
     */
    @Transactional(readOnly = true)
    public List<DepositContract> getActiveContractsByCustomer(Long customerId) {
        return depositContractService.getContractsByCustomer(customerId).stream()
                .filter(c -> c.getStatus() == DepositContractStatus.OPEN)
                .toList();
    }

    /**
     * Возвращает операции по всем договорам за период [fromDate; toDate] включительно.
     */
    @Transactional(readOnly = true)
    public List<DepositOperation> getOperationsByPeriod(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.plusDays(1).atStartOfDay().minusNanos(1);

        List<DepositOperation> list = depositOperationRepository.findByOperationDateTimeBetween(from, to);

        initializeContractsForUi(list);

        return list;
    }

    /**
     * Возвращает операции указанного типа за период.
     * Если type == null, возвращает все операции за период.
     */
    @Transactional(readOnly = true)
    public List<DepositOperation> getOperationsByPeriodAndType(
            LocalDate fromDate,
            LocalDate toDate,
            DepositOperationType type
    ) {
        if (type == null) {
            return getOperationsByPeriod(fromDate, toDate);
        }

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.plusDays(1).atStartOfDay().minusNanos(1);

        List<DepositOperation> list =
                depositOperationRepository.findByTypeAndOperationDateTimeBetween(type, from, to);

        initializeContractsForUi(list);

        return list;
    }

    /**
     * Операции за период + фильтр по типу + фильтр по клиенту.
     *
     * Логика:
     * - если customerId == null -> работаем как обычный отчёт по периоду/типу
     * - если type == null -> фильтруем только по клиенту и периоду
     * - иначе -> фильтруем по клиенту + типу + периоду
     *
     * ВАЖНО:
     * Здесь специально "прогреваем" contractNumber внутри транзакции,
     * чтобы UI мог отобразить № договора без LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public List<DepositOperation> getOperationsByPeriodAndTypeForCustomer(
            Long customerId,
            LocalDate fromDate,
            LocalDate toDate,
            DepositOperationType type
    ) {
        if (customerId == null) {
            return getOperationsByPeriodAndType(fromDate, toDate, type);
        }

        List<DepositOperation> base = getOperationsByPeriodAndType(fromDate, toDate, type);

        List<DepositOperation> filtered = base.stream()
                .filter(op -> op != null
                        && op.getContract() != null
                        && op.getContract().getCustomer() != null
                        && customerId.equals(op.getContract().getCustomer().getId()))
                .toList();

        initializeContractsForUi(filtered);

        return filtered;
    }

    /**
     * Возвращает всех клиентов (для выбора в отчётах).
     */
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    // =====================================================================
    // ======================= EXCEL EXPORT (XLSX) ==========================
    // =====================================================================

    /**
     * Экспорт в Excel одного или двух отчётов сразу:
     * - если contracts != null -> лист "Договоры"
     * - если operations != null -> лист "Операции"
     *
     * Удобно вызывать прямо из UI: передать items из TableView.
     */
    public void exportReportsToExcel(
            List<DepositContract> contracts,
            List<DepositOperation> operations,
            File file
    ) {
        if (file == null) {
            throw new InvalidOperationException("Не указан файл для экспорта");
        }

        try (Workbook wb = new XSSFWorkbook()) {
            CreationHelper helper = wb.getCreationHelper();

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dateStyle = createDateStyle(wb, helper);
            CellStyle dateTimeStyle = createDateTimeStyle(wb, helper);
            CellStyle moneyStyle = createMoneyStyle(wb);

            if (contracts != null) {
                writeContractsSheet(wb, "Договоры", contracts, headerStyle, dateStyle, moneyStyle);
            }
            if (operations != null) {
                // На всякий случай прогреем контракт (если список пришёл извне)
                initializeContractsForUi(operations);
                writeOperationsSheet(wb, "Операции", operations, headerStyle, dateTimeStyle, moneyStyle);
            }

            // Если оба списка null — чтобы файл не был пустым без листов
            if (contracts == null && operations == null) {
                Sheet sheet = wb.createSheet("Отчёт");
                Row row = sheet.createRow(0);
                Cell cell = row.createCell(0);
                cell.setCellValue("Нет данных для экспорта");
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }

        } catch (IOException e) {
            throw new InvalidOperationException("Ошибка сохранения Excel-файла: " + e.getMessage());
        }
    }

    /**
     * Экспорт только договоров в Excel (один лист).
     */
    public void exportContractsToExcel(List<DepositContract> contracts, File file) {
        exportReportsToExcel(contracts, null, file);
    }

    /**
     * Экспорт только операций в Excel (один лист).
     */
    public void exportOperationsToExcel(List<DepositOperation> operations, File file) {
        exportReportsToExcel(null, operations, file);
    }

    /**
     * Экспорт договоров клиента (включая закрытые) в Excel.
     * Делается внутри транзакции, поэтому данные стабильно читаются.
     */
    @Transactional(readOnly = true)
    public void exportCustomerContractsToExcel(Long customerId, File file) {
        List<DepositContract> contracts = getContractsByCustomer(customerId);
        exportReportsToExcel(contracts, null, file);
    }

    /**
     * Экспорт активных договоров клиента в Excel.
     */
    @Transactional(readOnly = true)
    public void exportActiveCustomerContractsToExcel(Long customerId, File file) {
        List<DepositContract> contracts = getActiveContractsByCustomer(customerId);
        exportReportsToExcel(contracts, null, file);
    }

    /**
     * Экспорт операций по параметрам (клиент может быть null => все клиенты),
     * период обязателен.
     */
    @Transactional(readOnly = true)
    public void exportOperationsByParamsToExcel(
            Long customerId,
            LocalDate fromDate,
            LocalDate toDate,
            DepositOperationType type,
            File file
    ) {
        if (fromDate == null || toDate == null) {
            throw new InvalidOperationException("Для экспорта операций требуется указать период (дату с/по)");
        }
        if (toDate.isBefore(fromDate)) {
            throw new InvalidOperationException("Дата 'по' не может быть раньше даты 'с'");
        }

        List<DepositOperation> operations = getOperationsByPeriodAndTypeForCustomer(customerId, fromDate, toDate, type);
        exportReportsToExcel(null, operations, file);
    }

    // -------------------- Excel helpers --------------------

    private void writeContractsSheet(
            Workbook wb,
            String sheetName,
            List<DepositContract> contracts,
            CellStyle headerStyle,
            CellStyle dateStyle,
            CellStyle moneyStyle
    ) {
        Sheet sheet = wb.createSheet(sheetName);

        int r = 0;

        // Header
        Row header = sheet.createRow(r++);
        int c = 0;

        createCell(header, c++, "№ договора", headerStyle);
        createCell(header, c++, "Клиент", headerStyle);
        createCell(header, c++, "Продукт", headerStyle);
        createCell(header, c++, "Статус", headerStyle);
        createCell(header, c++, "Дата открытия", headerStyle);
        createCell(header, c++, "Дата закрытия", headerStyle);
        createCell(header, c++, "Начальная сумма", headerStyle);
        createCell(header, c++, "Текущий баланс", headerStyle);
        createCell(header, c++, "% годовых", headerStyle);

        if (contracts != null) {
            for (DepositContract dc : contracts) {
                Row row = sheet.createRow(r++);
                int col = 0;

                String contractNumber = dc != null ? safe(dc.getContractNumber()) : "";
                createCell(row, col++, contractNumber, null);

                Customer customer = dc != null ? dc.getCustomer() : null;
                createCell(row, col++, formatCustomerShort(customer), null);

                DepositProduct product = dc != null ? dc.getProduct() : null;
                createCell(row, col++, product != null ? safe(product.getName()) : "", null);

                DepositContractStatus status = dc != null ? dc.getStatus() : null;
                createCell(row, col++, status != null ? status.name() : "", null);

                LocalDate openDate = dc != null ? dc.getOpenDate() : null;
                createDateCell(row, col++, openDate, dateStyle);

                LocalDate closeDate = dc != null ? dc.getCloseDate() : null;
                createDateCell(row, col++, closeDate, dateStyle);

                BigDecimal initialAmount = dc != null ? dc.getInitialAmount() : null;
                createMoneyCell(row, col++, initialAmount, moneyStyle);

                BigDecimal currentBalance = dc != null ? dc.getCurrentBalance() : null;
                createMoneyCell(row, col++, currentBalance, moneyStyle);

                BigDecimal rate = dc != null ? dc.getInterestRate() : null;
                if (rate == null) {
                    createCell(row, col++, "", null);
                } else {
                    createCell(row, col++, rate.toPlainString(), null);
                }
            }
        }

        autosizeColumns(sheet, 9);
    }

    private void writeOperationsSheet(
            Workbook wb,
            String sheetName,
            List<DepositOperation> operations,
            CellStyle headerStyle,
            CellStyle dateTimeStyle,
            CellStyle moneyStyle
    ) {
        Sheet sheet = wb.createSheet(sheetName);

        int r = 0;

        // Header
        Row header = sheet.createRow(r++);
        int c = 0;

        createCell(header, c++, "Дата и время", headerStyle);
        createCell(header, c++, "Тип операции", headerStyle);
        createCell(header, c++, "Сумма", headerStyle);
        createCell(header, c++, "№ договора", headerStyle);
        createCell(header, c++, "Клиент", headerStyle);
        createCell(header, c++, "Описание", headerStyle);

        if (operations != null) {
            for (DepositOperation op : operations) {
                Row row = sheet.createRow(r++);
                int col = 0;

                LocalDateTime dt = op != null ? op.getOperationDateTime() : null;
                createDateTimeCell(row, col++, dt, dateTimeStyle);

                DepositOperationType type = op != null ? op.getType() : null;
                createCell(row, col++, type != null ? type.name() : "", null);

                BigDecimal amount = op != null ? op.getAmount() : null;
                createMoneyCell(row, col++, amount, moneyStyle);

                DepositContract contract = op != null ? op.getContract() : null;
                String contractNumber = contract != null ? safe(contract.getContractNumber()) : "";
                createCell(row, col++, contractNumber, null);

                Customer customer = contract != null ? contract.getCustomer() : null;
                createCell(row, col++, formatCustomerShort(customer), null);

                String desc = op != null ? safe(op.getDescription()) : "";
                createCell(row, col++, desc, null);
            }
        }

        autosizeColumns(sheet, 6);
    }

    private void autosizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value != null ? value : "");
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void createDateCell(Row row, int index, LocalDate value, CellStyle style) {
        Cell cell = row.createCell(index);
        if (value == null) {
            cell.setCellValue("");
            return;
        }
        cell.setCellValue(java.sql.Date.valueOf(value));
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void createDateTimeCell(Row row, int index, LocalDateTime value, CellStyle style) {
        Cell cell = row.createCell(index);
        if (value == null) {
            cell.setCellValue("");
            return;
        }
        cell.setCellValue(java.sql.Timestamp.valueOf(value));
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void createMoneyCell(Row row, int index, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(index);
        if (value == null) {
            cell.setCellValue(0d);
        } else {
            cell.setCellValue(value.doubleValue());
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);

        CellStyle style = wb.createCellStyle();
        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private CellStyle createDateStyle(Workbook wb, CreationHelper helper) {
        CellStyle style = wb.createCellStyle();
        DataFormat df = helper.createDataFormat();
        style.setDataFormat(df.getFormat("dd.mm.yyyy"));
        return style;
    }

    private CellStyle createDateTimeStyle(Workbook wb, CreationHelper helper) {
        CellStyle style = wb.createCellStyle();
        DataFormat df = helper.createDataFormat();
        style.setDataFormat(df.getFormat("dd.mm.yyyy hh:mm"));
        return style;
    }

    private CellStyle createMoneyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat df = wb.createDataFormat();
        style.setDataFormat(df.getFormat("#,##0.00"));
        return style;
    }

    private String formatCustomerShort(Customer c) {
        if (c == null) {
            return "";
        }
        String middle = c.getMiddleName() != null && !c.getMiddleName().isBlank()
                ? " " + c.getMiddleName().trim()
                : "";
        return (safe(c.getLastName()) + " " + safe(c.getFirstName()) + middle).trim();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Принудительная инициализация поля contract для безопасного отображения
     * в JavaFX таблицах после выхода из транзакции.
     *
     * Это обходной путь.
     * Более правильный вариант — сделать join fetch в репозитории.
     */
    private void initializeContractsForUi(List<DepositOperation> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        for (DepositOperation op : list) {
            if (op == null) {
                continue;
            }

            DepositContract contract = op.getContract();
            if (contract != null) {
                // Трогаем contractNumber для инициализации прокси.
                // Этого достаточно для отображения № договора в отчётах.
                contract.getContractNumber();

                // Дополнительно прогреем клиента, чтобы в Excel/UI тоже было стабильно:
                if (contract.getCustomer() != null) {
                    contract.getCustomer().getId();
                }
            }
        }
    }
}
