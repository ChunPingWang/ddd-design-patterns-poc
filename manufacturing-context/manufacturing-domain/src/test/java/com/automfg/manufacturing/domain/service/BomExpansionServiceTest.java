package com.automfg.manufacturing.domain.service;

import com.automfg.manufacturing.domain.model.BomLineItem;
import com.automfg.manufacturing.domain.model.BomSnapshot;
import com.automfg.manufacturing.domain.port.MaterialAvailabilityGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BomExpansionServiceTest {

    @Test
    @DisplayName("expandBom with all materials available returns fully available BOM")
    void expand_bom_all_available() {
        MaterialAvailabilityGateway allAvailable = (partNumber, quantity) -> true;
        BomExpansionService service = new BomExpansionService(allAvailable);

        BomSnapshot snapshot = service.expandBom("MODEL-S", List.of("PREMIUM-AUDIO"));

        assertThat(snapshot.isFullyAvailable()).isTrue();
        assertThat(snapshot.getLineItems()).isNotEmpty();
        assertThat(snapshot.getMissingMaterials()).isEmpty();

        // Should include base model items + option items
        List<String> partNumbers = snapshot.getLineItems().stream()
            .map(BomLineItem::partNumber)
            .toList();
        assertThat(partNumbers).contains("CHS-001", "ENG-001", "AUD-001", "AUD-002");
    }

    @Test
    @DisplayName("expandBom with some unavailable materials detects missing items")
    void expand_bom_some_unavailable() {
        // Battery pack and engine are unavailable
        MaterialAvailabilityGateway partialAvailable = (partNumber, quantity) ->
            !partNumber.equals("BAT-001") && !partNumber.equals("ENG-001");

        BomExpansionService service = new BomExpansionService(partialAvailable);

        BomSnapshot snapshot = service.expandBom("MODEL-S", List.of());

        assertThat(snapshot.isFullyAvailable()).isFalse();
        assertThat(snapshot.getMissingMaterials()).hasSize(2);

        List<String> missingPartNumbers = snapshot.getMissingMaterials().stream()
            .map(BomLineItem::partNumber)
            .toList();
        assertThat(missingPartNumbers).containsExactlyInAnyOrder("BAT-001", "ENG-001");
    }
}
