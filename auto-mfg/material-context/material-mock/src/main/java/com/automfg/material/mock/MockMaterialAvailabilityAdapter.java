package com.automfg.material.mock;

import com.automfg.manufacturing.domain.port.MaterialAvailabilityGateway;
import org.springframework.stereotype.Service;

@Service
public class MockMaterialAvailabilityAdapter implements MaterialAvailabilityGateway {

    @Override
    public boolean checkAvailability(String partNumber, int quantity) {
        return true;
    }
}
