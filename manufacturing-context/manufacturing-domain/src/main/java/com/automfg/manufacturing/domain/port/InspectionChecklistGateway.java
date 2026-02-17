package com.automfg.manufacturing.domain.port;

import com.automfg.manufacturing.domain.model.ChecklistItemTemplate;

import java.util.List;

public interface InspectionChecklistGateway {
    List<ChecklistItemTemplate> getChecklistForModel(String vehicleModelCode);
}
