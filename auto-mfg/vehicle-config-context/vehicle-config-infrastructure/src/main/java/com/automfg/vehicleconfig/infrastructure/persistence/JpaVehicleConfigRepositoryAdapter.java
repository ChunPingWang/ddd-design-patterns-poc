package com.automfg.vehicleconfig.infrastructure.persistence;

import com.automfg.vehicleconfig.domain.model.ColorOption;
import com.automfg.vehicleconfig.domain.model.CompatibilityRule;
import com.automfg.vehicleconfig.domain.model.InspectionChecklistEntry;
import com.automfg.vehicleconfig.domain.model.OptionPackage;
import com.automfg.vehicleconfig.domain.model.VehicleConfiguration;
import com.automfg.vehicleconfig.domain.model.VehicleConfigurationId;
import com.automfg.vehicleconfig.domain.port.VehicleConfigurationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JpaVehicleConfigRepositoryAdapter implements VehicleConfigurationRepository {

    private final VehicleConfigSpringDataRepository configRepository;
    private final ColorOptionSpringDataRepository colorOptionRepository;
    private final OptionPackageSpringDataRepository optionPackageRepository;
    private final CompatibilityRuleSpringDataRepository compatibilityRuleRepository;
    private final InspectionChecklistSpringDataRepository inspectionChecklistRepository;

    public JpaVehicleConfigRepositoryAdapter(
            VehicleConfigSpringDataRepository configRepository,
            ColorOptionSpringDataRepository colorOptionRepository,
            OptionPackageSpringDataRepository optionPackageRepository,
            CompatibilityRuleSpringDataRepository compatibilityRuleRepository,
            InspectionChecklistSpringDataRepository inspectionChecklistRepository) {
        this.configRepository = configRepository;
        this.colorOptionRepository = colorOptionRepository;
        this.optionPackageRepository = optionPackageRepository;
        this.compatibilityRuleRepository = compatibilityRuleRepository;
        this.inspectionChecklistRepository = inspectionChecklistRepository;
    }

    @Override
    public Optional<VehicleConfiguration> findByModelCode(String modelCode) {
        return configRepository.findByModelCode(modelCode)
                .map(this::toDomain);
    }

    @Override
    public List<InspectionChecklistEntry> findChecklistByModelCode(String modelCode) {
        return inspectionChecklistRepository.findByModelCodeOrderByDisplayOrderAsc(modelCode)
                .stream()
                .map(entity -> new InspectionChecklistEntry(
                        entity.getItemDescription(),
                        entity.isSafetyRelated(),
                        entity.getDisplayOrder()
                ))
                .toList();
    }

    private VehicleConfiguration toDomain(VehicleConfigJpaEntity entity) {
        List<ColorOption> colors = colorOptionRepository
                .findByVehicleConfigurationId(entity.getId())
                .stream()
                .map(c -> new ColorOption(c.getColorCode(), c.getColorName()))
                .toList();

        List<OptionPackage> packages = optionPackageRepository
                .findByVehicleConfigurationId(entity.getId())
                .stream()
                .map(p -> new OptionPackage(p.getPackageCode(), p.getPackageName(), p.getBasePrice()))
                .toList();

        List<CompatibilityRule> rules = compatibilityRuleRepository
                .findByVehicleConfigurationId(entity.getId())
                .stream()
                .map(r -> new CompatibilityRule(
                        r.getOptionCodeA(),
                        r.getOptionCodeB(),
                        CompatibilityRule.RuleType.valueOf(r.getRuleType()),
                        r.getDescription()
                ))
                .toList();

        return new VehicleConfiguration(
                new VehicleConfigurationId(entity.getId()),
                entity.getModelCode(),
                entity.getModelName(),
                colors,
                packages,
                rules,
                entity.isActive()
        );
    }
}
