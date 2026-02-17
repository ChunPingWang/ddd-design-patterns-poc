package com.automfg.manufacturing.domain.port;

public interface MaterialAvailabilityGateway {
    boolean checkAvailability(String partNumber, int quantity);
}
