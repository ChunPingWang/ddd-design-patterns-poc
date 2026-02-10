package com.automfg.manufacturing.domain.service;

import com.automfg.manufacturing.domain.model.BomLineItem;
import com.automfg.manufacturing.domain.model.BomSnapshot;
import com.automfg.manufacturing.domain.port.MaterialAvailabilityGateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure domain service for expanding a Bill of Materials (BOM) for a vehicle model
 * and checking material availability. No framework dependencies.
 */
public class BomExpansionService {

    private final MaterialAvailabilityGateway materialAvailabilityGateway;

    // Hardcoded BOM mappings for PoC purposes
    private static final Map<String, List<BomLineItemSpec>> MODEL_BOM = Map.of(
        "MODEL-S", List.of(
            new BomLineItemSpec("CHS-001", "Chassis Frame Assembly", 1, "UNIT"),
            new BomLineItemSpec("ENG-001", "Electric Motor Unit", 1, "UNIT"),
            new BomLineItemSpec("BAT-001", "Battery Pack 100kWh", 1, "UNIT"),
            new BomLineItemSpec("BRK-001", "Brake System Kit", 1, "SET"),
            new BomLineItemSpec("SUS-001", "Suspension Assembly", 4, "UNIT"),
            new BomLineItemSpec("WHL-001", "Wheel Assembly 19in", 4, "UNIT"),
            new BomLineItemSpec("BDY-001", "Body Panel Set", 1, "SET"),
            new BomLineItemSpec("INT-001", "Interior Trim Package", 1, "SET"),
            new BomLineItemSpec("ELC-001", "Electrical Wiring Harness", 1, "SET"),
            new BomLineItemSpec("GLZ-001", "Glass Set (Windshield + Windows)", 1, "SET")
        ),
        "MODEL-X", List.of(
            new BomLineItemSpec("CHS-002", "SUV Chassis Frame Assembly", 1, "UNIT"),
            new BomLineItemSpec("ENG-002", "Dual Motor Powertrain", 1, "UNIT"),
            new BomLineItemSpec("BAT-002", "Battery Pack 120kWh", 1, "UNIT"),
            new BomLineItemSpec("BRK-001", "Brake System Kit", 1, "SET"),
            new BomLineItemSpec("SUS-002", "Heavy-Duty Suspension Assembly", 4, "UNIT"),
            new BomLineItemSpec("WHL-002", "Wheel Assembly 22in", 4, "UNIT"),
            new BomLineItemSpec("BDY-002", "SUV Body Panel Set", 1, "SET"),
            new BomLineItemSpec("INT-002", "Premium Interior Trim Package", 1, "SET"),
            new BomLineItemSpec("ELC-001", "Electrical Wiring Harness", 1, "SET"),
            new BomLineItemSpec("GLZ-002", "Panoramic Glass Set", 1, "SET")
        )
    );

    private static final Map<String, List<BomLineItemSpec>> OPTION_BOM = Map.of(
        "PREMIUM-AUDIO", List.of(
            new BomLineItemSpec("AUD-001", "Premium Speaker System", 1, "SET"),
            new BomLineItemSpec("AUD-002", "Amplifier Unit", 1, "UNIT")
        ),
        "AUTOPILOT", List.of(
            new BomLineItemSpec("AP-001", "Autopilot Computer Module", 1, "UNIT"),
            new BomLineItemSpec("AP-002", "Camera Array Kit", 1, "SET"),
            new BomLineItemSpec("AP-003", "Ultrasonic Sensor Kit", 1, "SET")
        ),
        "SPORT-PACKAGE", List.of(
            new BomLineItemSpec("SPT-001", "Sport Suspension Upgrade", 4, "UNIT"),
            new BomLineItemSpec("SPT-002", "Performance Brake Kit", 1, "SET")
        ),
        "TOW-PACKAGE", List.of(
            new BomLineItemSpec("TOW-001", "Tow Hitch Assembly", 1, "UNIT"),
            new BomLineItemSpec("TOW-002", "Trailer Wiring Harness", 1, "SET")
        )
    );

    public BomExpansionService(MaterialAvailabilityGateway materialAvailabilityGateway) {
        this.materialAvailabilityGateway = Objects.requireNonNull(
            materialAvailabilityGateway, "MaterialAvailabilityGateway must not be null");
    }

    /**
     * Expands the BOM for a given vehicle model and option packages, then checks
     * material availability for each line item.
     */
    public BomSnapshot expandBom(String vehicleModelCode, List<String> optionPackageCodes) {
        Objects.requireNonNull(vehicleModelCode, "Vehicle model code must not be null");
        Objects.requireNonNull(optionPackageCodes, "Option package codes must not be null");

        List<BomLineItemSpec> specs = new ArrayList<>();

        // Base model BOM
        List<BomLineItemSpec> modelSpecs = MODEL_BOM.get(vehicleModelCode);
        if (modelSpecs == null) {
            // Default BOM for unknown models in PoC
            modelSpecs = MODEL_BOM.get("MODEL-S");
        }
        specs.addAll(modelSpecs);

        // Option packages BOM
        for (String optionCode : optionPackageCodes) {
            List<BomLineItemSpec> optionSpecs = OPTION_BOM.get(optionCode);
            if (optionSpecs != null) {
                specs.addAll(optionSpecs);
            }
        }

        // Check availability for each line item
        List<BomLineItem> lineItems = specs.stream()
            .map(spec -> {
                boolean available = materialAvailabilityGateway.checkAvailability(
                    spec.partNumber(), spec.quantity());
                return new BomLineItem(
                    spec.partNumber(), spec.description(), spec.quantity(),
                    spec.unitOfMeasure(), available);
            })
            .toList();

        return new BomSnapshot(lineItems);
    }

    /**
     * Internal record for BOM line item specification (before availability check).
     */
    private record BomLineItemSpec(
        String partNumber,
        String description,
        int quantity,
        String unitOfMeasure
    ) {}
}
