package org.santayn.bankdeposit.service;

import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.DepositProduct;

import org.santayn.bankdeposit.repository.DepositProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для работы с депозитными продуктами.
 */
@Service
@RequiredArgsConstructor
public class DepositProductService {

    private DepositProductRepository depositProductRepository;

    public List<DepositProduct> getAllProducts() {
        return depositProductRepository.findAll();
    }

    public DepositProduct getProductById(Long id) {
        return depositProductRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Депозитный продукт с id=" + id + " не найден"));
    }

    public DepositProduct createProduct(DepositProduct product) {
        product.setId(null);
        return depositProductRepository.save(product);
    }

    public DepositProduct updateProduct(Long id, DepositProduct updated) {
        DepositProduct existing = getProductById(id);

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setMinAmount(updated.getMinAmount());
        existing.setMaxAmount(updated.getMaxAmount());
        existing.setTermMonths(updated.getTermMonths());
        existing.setBaseInterestRate(updated.getBaseInterestRate());
        existing.setAllowReplenishment(updated.getAllowReplenishment());
        existing.setAllowPartialWithdrawal(updated.getAllowPartialWithdrawal());
        existing.setCapitalization(updated.getCapitalization());

        return depositProductRepository.save(existing);
    }

    public void deleteProduct(Long id) {
        if (!depositProductRepository.existsById(id)) {
            throw new EntityNotFoundException("Депозитный продукт с id=" + id + " не найден");
        }
        depositProductRepository.deleteById(id);
    }
}
