package com.hsbc.tms.rules.repository;

import com.hsbc.tms.rules.entity.RuleFeatureRequest;
import java.util.List;
import java.util.Optional;

public interface RuleFeatureRequestRepository {
    List<RuleFeatureRequest> findAll();
    Optional<RuleFeatureRequest> findById(Long id);
    RuleFeatureRequest save(RuleFeatureRequest request);
    void updateStatus(Long id, String status, String adminNote);
    void delete(Long id);
}

