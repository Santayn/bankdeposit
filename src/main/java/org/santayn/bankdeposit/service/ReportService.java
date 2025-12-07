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
    public List<DepositContract> getContractsByCustomer(Long customerId) {
        return depositContractService.getContractsByCustomer(customerId);
    }

    /**
     * Возвращает только активные (открытые) вклады клиента.
     */
    public List<DepositContract> getActiveContractsByCustomer(Long customerId) {
        return depositContractService.getContractsByCustomer(customerId).stream()
                .filter(c -> c.getStatus() == DepositContractStatus.OPEN)
                .toList();
    }

    /**
     * Возвращает операции по всем договорам за период [fromDate; toDate] включительно.
     */
    public List<DepositOperation> getOperationsByPeriod(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.plusDays(1).atStartOfDay().minusNanos(1);
        return depositOperationRepository.findByOperationDateTimeBetween(from, to);
    }

    /**
     * Возвращает операции указанного типа за период.
     */
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
        return depositOperationRepository.findByTypeAndOperationDateTimeBetween(type, from, to);
    }

    /**
     * Возвращает всех клиентов (для выбора в отчётах).
     */
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
}
