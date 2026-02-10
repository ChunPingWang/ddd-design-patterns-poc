package com.automfg.manufacturing.infrastructure.adapter.inbound;

import com.automfg.manufacturing.application.usecase.CompleteReworkUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rework-orders")
public class ReworkController {

    private final CompleteReworkUseCase completeReworkUseCase;

    public ReworkController(CompleteReworkUseCase completeReworkUseCase) {
        this.completeReworkUseCase = completeReworkUseCase;
    }

    @PostMapping("/{reworkOrderId}/complete")
    public ResponseEntity<Void> completeRework(@PathVariable UUID reworkOrderId) {
        var command = new CompleteReworkUseCase.CompleteReworkCommand(reworkOrderId);
        completeReworkUseCase.execute(command);
        return ResponseEntity.ok().build();
    }
}
