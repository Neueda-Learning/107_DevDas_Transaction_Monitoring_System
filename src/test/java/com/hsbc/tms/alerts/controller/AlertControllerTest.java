package com.hsbc.tms.alerts.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hsbc.tms.alerts.dto.AlertHistoryResponse;
import com.hsbc.tms.alerts.dto.AlertResponse;
import com.hsbc.tms.alerts.dto.AlertStatusUpdateRequest;
import com.hsbc.tms.alerts.dto.CreateAlertRequest;
import com.hsbc.tms.alerts.model.AlertStatus;
import com.hsbc.tms.alerts.service.AlertService;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock
    private AlertService alertService;

    private AlertController controller;

    @BeforeEach
    void setUp() {
        controller = new AlertController(alertService);
    }

    @Test
    void create_delegatesToService() {
        CreateAlertRequest request = new CreateAlertRequest(
                "High Amount",
                RuleType.AMOUNT_THRESHOLD,
                AlertSeverity.HIGH,
                "Threshold exceeded",
                "analyst-01",
                "created",
                List.of());
        AlertResponse response = sampleAlert();
        when(alertService.createAlert(request)).thenReturn(response);

        AlertResponse result = controller.create(request);

        assertThat(result).isEqualTo(response);
        verify(alertService).createAlert(request);
    }

    @Test
    void list_delegatesToService() {
        AlertResponse response = sampleAlert();
        when(alertService.getAlerts(AlertStatus.OPEN, AlertSeverity.MEDIUM, true)).thenReturn(List.of(response));

        List<AlertResponse> result = controller.list(AlertStatus.OPEN, AlertSeverity.MEDIUM, true);

        assertThat(result).containsExactly(response);
        verify(alertService).getAlerts(AlertStatus.OPEN, AlertSeverity.MEDIUM, true);
    }

    @Test
    void get_delegatesToService() {
        AlertResponse response = sampleAlert();
        when(alertService.getAlert(44L)).thenReturn(response);

        AlertResponse result = controller.get(44L);

        assertThat(result).isEqualTo(response);
        verify(alertService).getAlert(44L);
    }

    @Test
    void history_delegatesToService() {
        AlertHistoryResponse history = new AlertHistoryResponse(
                3L,
                AlertStatus.OPEN,
                AlertStatus.ACKNOWLEDGED,
                "Investigating",
                "analyst-02",
                Instant.parse("2026-08-06T10:00:00Z"));
        when(alertService.getHistory(99L)).thenReturn(List.of(history));

        List<AlertHistoryResponse> result = controller.history(99L);

        assertThat(result).containsExactly(history);
        verify(alertService).getHistory(99L);
    }

    @Test
    void updateStatus_delegatesToService() {
        AlertStatusUpdateRequest request = new AlertStatusUpdateRequest(AlertStatus.CLOSED, "analyst-01", "done");
        AlertResponse response = sampleAlert();
        when(alertService.updateStatus(5L, request)).thenReturn(response);

        AlertResponse result = controller.updateStatus(5L, request);

        assertThat(result).isEqualTo(response);
        verify(alertService).updateStatus(5L, request);
    }

    private AlertResponse sampleAlert() {
        return new AlertResponse(
                1L,
                "High Amount",
                RuleType.AMOUNT_THRESHOLD,
                AlertSeverity.HIGH,
                AlertStatus.OPEN,
                "Threshold exceeded",
                Instant.parse("2026-08-06T09:00:00Z"),
                Instant.parse("2026-08-06T09:00:00Z"),
                List.of(),
                List.of());
    }
}

