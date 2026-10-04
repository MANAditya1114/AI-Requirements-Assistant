package com.requirements.backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.requirements.backend.model.Interview;

public interface InterviewRepository
        extends MongoRepository<Interview, String> {

    List<Interview> findByStakeholderUserId(
            String stakeholderUserId);

    List<Interview> findByBusinessAnalystUserId(
            String businessAnalystUserId);
}