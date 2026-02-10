package com.automfg.vehicleconfig.domain.model;

import java.util.Objects;

public class CompatibilityRule {

    public enum RuleType {
        INCOMPATIBLE, REQUIRES
    }

    private final String optionCodeA;
    private final String optionCodeB;
    private final RuleType ruleType;
    private final String description;

    public CompatibilityRule(String optionCodeA, String optionCodeB, RuleType ruleType, String description) {
        this.optionCodeA = Objects.requireNonNull(optionCodeA);
        this.optionCodeB = Objects.requireNonNull(optionCodeB);
        this.ruleType = Objects.requireNonNull(ruleType);
        this.description = description;
    }

    public boolean isViolatedBy(java.util.List<String> selectedOptions, String modelCode) {
        if (ruleType == RuleType.INCOMPATIBLE) {
            boolean hasA = selectedOptions.contains(optionCodeA) || modelCode.equals(optionCodeA);
            boolean hasB = selectedOptions.contains(optionCodeB) || modelCode.equals(optionCodeB);
            return hasA && hasB;
        }
        return false;
    }

    public String getOptionCodeA() { return optionCodeA; }
    public String getOptionCodeB() { return optionCodeB; }
    public RuleType getRuleType() { return ruleType; }
    public String getDescription() { return description; }
}
