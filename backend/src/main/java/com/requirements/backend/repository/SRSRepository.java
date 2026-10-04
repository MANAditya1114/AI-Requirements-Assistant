package com.requirements.backend.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.requirements.backend.model.SRS;

public interface SRSRepository
        extends MongoRepository<SRS, String> {

    Optional<SRS> findByInterviewId(
            String interviewId
    );
}