package com.hsbc.tms.config;

import com.hsbc.tms.rules.entity.MonitoringRule;
import com.hsbc.tms.rules.model.AlertSeverity;
import com.hsbc.tms.rules.model.RuleType;
import com.hsbc.tms.rules.repository.MonitoringRuleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedRules(MonitoringRuleRepository ruleRepository) {
        return args -> {
            if (ruleRepository.count() > 0) {
                return;
            }

            ruleRepository.save(buildAmountRule());
            ruleRepository.save(buildVelocityRule());
            ruleRepository.save(buildNewPayeeRule());
            ruleRepository.save(buildDailyLimitRule());
        };
    }

    private MonitoringRule buildAmountRule() {
        MonitoringRule rule = new MonitoringRule();
        rule.setName("High Amount Threshold");
        rule.setType(RuleType.AMOUNT_THRESHOLD);
        rule.setSeverity(AlertSeverity.HIGH);
        rule.setActive(true);
        rule.setAmountThreshold(new BigDecimal("10000.00"));
        rule.setCreatedAt(Instant.now());
        return rule;
    }

    private MonitoringRule buildVelocityRule() {
        MonitoringRule rule = new MonitoringRule();
        rule.setName("Velocity 5 in 10 min");
        rule.setType(RuleType.VELOCITY);
        rule.setSeverity(AlertSeverity.MEDIUM);
        rule.setActive(true);
        rule.setTransactionCountThreshold(5);
        rule.setTimeWindowMinutes(10);
        rule.setCreatedAt(Instant.now());
        return rule;
    }

    private MonitoringRule buildNewPayeeRule() {
        MonitoringRule rule = new MonitoringRule();
        rule.setName("New Payee");
        rule.setType(RuleType.NEW_PAYEE);
        rule.setSeverity(AlertSeverity.MEDIUM);
        rule.setActive(true);
        rule.setCreatedAt(Instant.now());
        return rule;
    }

    private MonitoringRule buildDailyLimitRule() {
        MonitoringRule rule = new MonitoringRule();
        rule.setName("Daily Limit");
        rule.setType(RuleType.DAILY_LIMIT);
        rule.setSeverity(AlertSeverity.HIGH);
        rule.setActive(true);
        rule.setAmountThreshold(new BigDecimal("50000.00"));
        rule.setCreatedAt(Instant.now());
        return rule;
    }
}
