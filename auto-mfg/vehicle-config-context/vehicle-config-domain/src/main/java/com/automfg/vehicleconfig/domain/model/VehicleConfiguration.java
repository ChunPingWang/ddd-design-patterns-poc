package com.automfg.vehicleconfig.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class VehicleConfiguration {

    private final VehicleConfigurationId id;
    private final String modelCode;
    private final String modelName;
    private final List<ColorOption> availableColors;
    private final List<OptionPackage> optionPackages;
    private final List<CompatibilityRule> compatibilityRules;
    private final boolean active;

    public VehicleConfiguration(
            VehicleConfigurationId id,
            String modelCode,
            String modelName,
            List<ColorOption> availableColors,
            List<OptionPackage> optionPackages,
            List<CompatibilityRule> compatibilityRules,
            boolean active) {
        this.id = Objects.requireNonNull(id);
        this.modelCode = Objects.requireNonNull(modelCode);
        this.modelName = Objects.requireNonNull(modelName);
        this.availableColors = List.copyOf(availableColors);
        this.optionPackages = List.copyOf(optionPackages);
        this.compatibilityRules = List.copyOf(compatibilityRules);
        this.active = active;
    }

    public CompatibilityValidationResult validateOptions(List<String> selectedOptionCodes) {
        List<String> violations = new ArrayList<>();

        for (CompatibilityRule rule : compatibilityRules) {
            if (rule.isViolatedBy(selectedOptionCodes, modelCode)) {
                violations.add(rule.getDescription() != null
                    ? rule.getDescription()
                    : rule.getOptionCodeA() + " is incompatible with " + rule.getOptionCodeB());
            }
        }

        // Validate all selected options exist for this model
        List<String> availableCodes = optionPackages.stream()
                .map(OptionPackage::getPackageCode)
                .toList();
        for (String code : selectedOptionCodes) {
            if (!availableCodes.contains(code)) {
                violations.add("Option package '" + code + "' is not available for model " + modelCode);
            }
        }

        return violations.isEmpty()
            ? CompatibilityValidationResult.pass()
            : CompatibilityValidationResult.fail(violations);
    }

    public boolean isColorAvailable(String colorCode) {
        return availableColors.stream().anyMatch(c -> c.code().equals(colorCode));
    }

    public BigDecimal calculatePrice(BigDecimal baseModelPrice, List<String> selectedOptionCodes) {
        BigDecimal optionTotal = optionPackages.stream()
                .filter(p -> selectedOptionCodes.contains(p.getPackageCode()))
                .map(OptionPackage::getBasePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return baseModelPrice.add(optionTotal);
    }

    public VehicleConfigurationId getId() { return id; }
    public String getModelCode() { return modelCode; }
    public String getModelName() { return modelName; }
    public List<ColorOption> getAvailableColors() { return availableColors; }
    public List<OptionPackage> getOptionPackages() { return optionPackages; }
    public List<CompatibilityRule> getCompatibilityRules() { return compatibilityRules; }
    public boolean isActive() { return active; }
}
