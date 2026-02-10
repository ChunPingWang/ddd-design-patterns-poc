package com.automfg.manufacturing.infrastructure.adapter.outbound;

import com.automfg.manufacturing.domain.model.ChecklistItemTemplate;
import com.automfg.manufacturing.domain.port.InspectionChecklistGateway;
import com.automfg.manufacturing.infrastructure.persistence.InspectionChecklistJpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Anti-Corruption Layer adapter that reads inspection checklist data from the
 * inspection_checklists table (owned by vehicle-config context) and translates
 * it into the manufacturing domain's ChecklistItemTemplate value objects.
 */
@Service
public class InspectionChecklistACLAdapter implements InspectionChecklistGateway {

    private final InspectionChecklistJpaRepository checklistJpaRepository;

    public InspectionChecklistACLAdapter(InspectionChecklistJpaRepository checklistJpaRepository) {
        this.checklistJpaRepository = checklistJpaRepository;
    }

    @Override
    public List<ChecklistItemTemplate> getChecklistForModel(String vehicleModelCode) {
        return checklistJpaRepository.findByModelCodeOrderByDisplayOrder(vehicleModelCode)
            .stream()
            .map(entity -> new ChecklistItemTemplate(
                entity.getItemDescription(),
                entity.isSafetyRelated()))
            .toList();
    }
}
