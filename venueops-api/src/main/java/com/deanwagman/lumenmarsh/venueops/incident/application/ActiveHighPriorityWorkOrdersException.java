package com.deanwagman.lumenmarsh.venueops.incident.application;

import java.util.List;

public class ActiveHighPriorityWorkOrdersException extends RuntimeException {

    private final List<String> workOrderNumbers;

    public ActiveHighPriorityWorkOrdersException(List<String> workOrderNumbers) {
        super("Resolving this incident requires confirmation because high-priority work orders are still active: "
                + String.join(", ", workOrderNumbers));
        this.workOrderNumbers = List.copyOf(workOrderNumbers);
    }

    public List<String> workOrderNumbers() {
        return workOrderNumbers;
    }
}
