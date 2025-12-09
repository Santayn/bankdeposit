package org.santayn.bankdeposit.service;

import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositContractStatus;
import org.santayn.bankdeposit.models.DepositOperation;
import org.santayn.bankdeposit.models.DepositOperationType;
import org.santayn.bankdeposit.repository.CustomerRepository;
import org.santayn.bankdeposit.repository.DepositOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для построения отчётов по вкладам и операциям.
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
            }
        }
    }
}
