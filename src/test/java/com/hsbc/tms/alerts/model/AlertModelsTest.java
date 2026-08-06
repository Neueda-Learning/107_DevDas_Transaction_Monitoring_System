package com.hsbc.tms.alerts.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AlertModelsTest {

    @Test
    void alertStatus_containsExpectedValues() {
        assertThat(AlertStatus.values())
                .containsExactly(
                        AlertStatus.OPEN,
                        AlertStatus.ACKNOWLEDGED,
                        AlertStatus.INVESTIGATING,
                        AlertStatus.CLOSED,
                        AlertStatus.DISMISSED);
        assertThat(AlertStatus.valueOf("OPEN")).isEqualTo(AlertStatus.OPEN);
    }
}

