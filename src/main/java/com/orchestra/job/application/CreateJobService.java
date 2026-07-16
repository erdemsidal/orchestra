package com.orchestra.job.application;

import com.orchestra.job.domain.Job;

import java.util.UUID;

/**
 * USE CASE: Yeni bir iş oluştur.
 *
 * Neden burada @Service anotasyonu YOK?
 *   Bilinçli tercih: application katmanını Spring'den tamamen bağımsız tutuyoruz.
 *   Bu sınıfı Spring'e "bean" olarak tanıtma işini infrastructure'daki bir
 *   @Configuration sınıfı yapacak (JobBeanConfig). Böylece:
 *     - application katmanı sıfır framework bağımlılığı taşır,
 *     - testte "new CreateJobService(sahteRepo)" deyip Spring'siz test ederiz,
 *     - hangi parçanın neye bağlandığı tek bir yerde, açıkça görünür.
 *
 * Bağımlılık constructor'dan geliyor (constructor injection). Alan `final`:
 * nesne kurulduktan sonra bağımlılığı değişemez.
 */
public class CreateJobService {

    private final JobRepository jobRepository;

    public CreateJobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    /**
     * Verilen türde yeni bir iş oluşturur ve kaydeder.
     * İş PENDING olarak doğar — bu kuralı biz burada değil, Job constructor'ı uygular.
     * (İş kuralı domain'in sorumluluğu; use case sadece akışı yönetir.)
     */
    public Job create(String type) {
        Job job = new Job(UUID.randomUUID(), type);
        return jobRepository.save(job);
    }
}
