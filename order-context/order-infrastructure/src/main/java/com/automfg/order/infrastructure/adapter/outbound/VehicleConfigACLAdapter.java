package com.automfg.order.infrastructure.adapter.outbound;

import com.automfg.order.domain.port.VehicleConfigGateway;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class VehicleConfigACLAdapter implements VehicleConfigGateway {

    @PersistenceContext
    private EntityManager em;

    @Override
    public ValidationResult validateConfiguration(String modelCode, String colorCode,
                                                   List<String> optionCodes) {
        List<String> violations = new ArrayList<>();

        // Find vehicle config
        @SuppressWarnings("unchecked")
        List<Object[]> configs = em.createNativeQuery(
                "SELECT id, is_active FROM vehicle_configurations WHERE model_code = ?1")
                .setParameter(1, modelCode)
                .getResultList();

        if (configs.isEmpty()) {
            violations.add("Vehicle model not found: " + modelCode);
            return new ValidationResult(false, violations);
        }

        Object[] config = configs.get(0);
        UUID configId = (UUID) config[0];
        boolean isActive = (Boolean) config[1];

        if (!isActive) {
            violations.add("Vehicle model is not active: " + modelCode);
        }

        // Validate option packages exist
        @SuppressWarnings("unchecked")
        List<String> availableCodes = em.createNativeQuery(
                "SELECT package_code FROM option_packages WHERE vehicle_config_id = ?1")
                .setParameter(1, configId)
                .getResultList();

        for (String code : optionCodes) {
            if (!availableCodes.contains(code)) {
                violations.add("Option package '" + code + "' is not available for model " + modelCode);
            }
        }

        // Check compatibility rules
        @SuppressWarnings("unchecked")
        List<Object[]> rules = em.createNativeQuery(
                "SELECT option_code_a, option_code_b, rule_type, description FROM compatibility_rules WHERE vehicle_config_id = ?1")
                .setParameter(1, configId)
                .getResultList();

        for (Object[] rule : rules) {
            String codeA = (String) rule[0];
            String codeB = (String) rule[1];
            String ruleType = (String) rule[2];
            String description = (String) rule[3];

            if ("INCOMPATIBLE".equals(ruleType)) {
                boolean hasA = optionCodes.contains(codeA) || modelCode.equals(codeA);
                boolean hasB = optionCodes.contains(codeB) || modelCode.equals(codeB);
                if (hasA && hasB) {
                    violations.add(description != null ? description : codeA + " is incompatible with " + codeB);
                }
            }
        }

        return violations.isEmpty()
                ? new ValidationResult(true, List.of())
                : new ValidationResult(false, violations);
    }

    @Override
    public BigDecimal calculatePrice(String modelCode, List<String> optionCodes) {
        // Base price from model (for PoC, use 0 as base — price comes from option packages)
        BigDecimal basePrice = BigDecimal.ZERO;

        if (optionCodes == null || optionCodes.isEmpty()) {
            return basePrice;
        }

        @SuppressWarnings("unchecked")
        List<Object[]> configs = em.createNativeQuery(
                "SELECT id FROM vehicle_configurations WHERE model_code = ?1")
                .setParameter(1, modelCode)
                .getResultList();

        if (configs.isEmpty()) {
            throw new IllegalArgumentException("Vehicle model not found: " + modelCode);
        }

        UUID configId = (UUID) configs.get(0)[0];

        @SuppressWarnings("unchecked")
        List<BigDecimal> prices = em.createNativeQuery(
                "SELECT base_price FROM option_packages WHERE vehicle_config_id = ?1 AND package_code IN (?2)")
                .setParameter(1, configId)
                .setParameter(2, optionCodes)
                .getResultList();

        return prices.stream().reduce(basePrice, BigDecimal::add);
    }
}
