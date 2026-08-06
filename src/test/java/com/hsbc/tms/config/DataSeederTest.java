package com.hsbc.tms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.repository.MonitoringRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private MonitoringRuleRepository ruleRepository;

    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        dataSeeder = new DataSeeder();
    }

    @Test
    void seedRules_insertsFourDefaultsWhenRepositoryIsEmpty() throws Exception {
        when(ruleRepository.count()).thenReturn(0L);
        when(ruleRepository.save(any(MonitoringRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommandLineRunner runner = dataSeeder.seedRules(ruleRepository);
        runner.run();

        ArgumentCaptor<MonitoringRule> ruleCaptor = ArgumentCaptor.forClass(MonitoringRule.class);
        verify(ruleRepository, times(4)).save(ruleCaptor.capture());

        assertThat(ruleCaptor.getAllValues())
                .extracting(MonitoringRule::getName)
                .containsExactly(
                        "High Amount Threshold",
                        "Velocity 5 in 10 min",
                        "New Payee",
                        "Daily Limit");
    }

    @Test
    void seedRules_skipsInsertWhenRulesAlreadyExist() throws Exception {
        when(ruleRepository.count()).thenReturn(2L);

        CommandLineRunner runner = dataSeeder.seedRules(ruleRepository);
        runner.run();

        verify(ruleRepository, never()).save(any(MonitoringRule.class));
    }
}

