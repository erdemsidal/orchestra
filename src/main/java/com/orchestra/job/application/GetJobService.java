package com.orchestra.job.application;

import java.util.UUID;

import com.orchestra.job.domain.Job;
import com.orchestra.job.domain.JobNotFoundException;

/**
 * USE CASE: Bir işi id ile getir.
 *
 * CreateJobService ile aynı kalıp: Spring anotasyonu yok, bağımlılık
 * constructor'dan geliyor, port ile konuşuyor.
 */
public class GetJobService {

    private final JobRepository jobRepository;

    public GetJobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    /**
     * İşi id ile getirir.
     *
     * @throws JobNotFoundException iş bulunamazsa
     */
    public Job getById(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
    }
}
