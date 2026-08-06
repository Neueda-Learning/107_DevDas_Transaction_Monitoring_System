package com.hsbc.tms.rules.service;

import com.hsbc.tms.rules.dto.FeatureRequestCreateRequest;
import com.hsbc.tms.rules.dto.FeatureRequestResponse;
import com.hsbc.tms.rules.dto.FeatureRequestStatusUpdateRequest;
import com.hsbc.tms.rules.entity.RuleFeatureRequest;
import com.hsbc.tms.rules.model.FeatureRequestStatus;
import com.hsbc.tms.rules.repository.RuleFeatureRequestRepository;
import com.hsbc.tms.common.exception.BadRequestException;
import com.hsbc.tms.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FeatureRequestService {

    private final RuleFeatureRequestRepository repository;

    public FeatureRequestService(RuleFeatureRequestRepository repository) {
        this.repository = repository;
    }

    public List<FeatureRequestResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public FeatureRequestResponse getById(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Feature request not found: " + id));
    }

    public FeatureRequestResponse create(FeatureRequestCreateRequest req) {
        RuleFeatureRequest entity = new RuleFeatureRequest();
        entity.setTitle(req.title());
        entity.setDescription(req.description());
        entity.setRequestedBy(req.requestedBy());
        entity.setStatus(FeatureRequestStatus.REQUESTED.name());
        return toResponse(repository.save(entity));
    }

    public FeatureRequestResponse updateStatus(Long id, FeatureRequestStatusUpdateRequest req) {
        RuleFeatureRequest existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feature request not found: " + id));

        // Validate status transition
        FeatureRequestStatus newStatus;
        try {
            newStatus = FeatureRequestStatus.valueOf(req.status());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + req.status());
        }

        String current = existing.getStatus();
        if (FeatureRequestStatus.WITHDRAWN.name().equals(current) ||
                FeatureRequestStatus.IMPLEMENTED.name().equals(current)) {
            throw new BadRequestException("Cannot change status of a " + current.toLowerCase() + " request");
        }

        repository.updateStatus(id, newStatus.name(), req.adminNote());
        return getById(id);
    }

    public FeatureRequestResponse withdraw(Long id, String requestedBy) {
        RuleFeatureRequest existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feature request not found: " + id));

        String current = existing.getStatus();
        if (FeatureRequestStatus.IMPLEMENTED.name().equals(current)) {
            throw new BadRequestException("Cannot withdraw an already implemented request");
        }
        if (FeatureRequestStatus.WITHDRAWN.name().equals(current)) {
            throw new BadRequestException("Request is already withdrawn");
        }

        repository.updateStatus(id, FeatureRequestStatus.WITHDRAWN.name(), "Withdrawn by " + requestedBy);
        return getById(id);
    }

    private FeatureRequestResponse toResponse(RuleFeatureRequest entity) {
        return new FeatureRequestResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getRequestedBy(),
                entity.getStatus(),
                entity.getAdminNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}




