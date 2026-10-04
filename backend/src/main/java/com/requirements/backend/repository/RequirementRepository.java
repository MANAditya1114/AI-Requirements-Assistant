package com.requirements.backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.requirements.backend.model.Requirement;

public interface RequirementRepository
        extends MongoRepository<Requirement, String> {

    List<Requirement> findByInterviewId(String interviewId);
}