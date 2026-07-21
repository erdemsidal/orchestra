package com.orchestra.job.infrastructure.persistence;

import com.orchestra.job.application.JobRepository;
import com.orchestra.job.domain.Job;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * ADAPTER — JobRepository port'unun gerçek (JPA) implementasyonu.
 *
 * "Priz"e takılan santral bu. İki işi var:
 *   1. Port'un sözünü tutmak   -> implements JobRepository
 *   2. ÇEVİRMENLİK yapmak      -> domain Job  <->  JobEntity
 *
 * Bağımlılık oku içeri bakıyor: bu sınıf application'ı biliyor (port'u import ediyor),
 * ama application bu sınıfın varlığından haberdar bile değil. Hexagonal'ın kalbi bu.
 */
@Repository
public class JpaJobRepositoryAdapter implements JobRepository {

    private final SpringDataJobRepository springDataRepository;

    public JpaJobRepositoryAdapter(SpringDataJobRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Job save(Job job) {
        // domain -> entity çevir, kaydet, sonra entity -> domain geri çevir.
        JobEntity entity = toEntity(job);
        JobEntity kaydedilen = springDataRepository.save(entity);
        return toDomain(kaydedilen);
    }

    @Override
    public Optional<Job> findById(UUID id) {
        // Optional.map: içinde değer varsa çevir, yoksa boş Optional kalsın.
        return springDataRepository.findById(id).map(this::toDomain);
    }

    // ── Çeviri metotları ─────────────────────────────────────────────

    /** domain Job -> JPA entity */
    private JobEntity toEntity(Job job) {
        return new JobEntity(job.getId(), job.getType(), job.getStatus());
    }

    /**
     * JPA entity -> domain Job.
     * Job.reconstitute() ile geri kuruyoruz: DB'deki iş PENDING olmak zorunda
     * değil (RUNNING/DONE olabilir). Bu, adlandırılmış "yeniden kurma" yolu.
     */
    private Job toDomain(JobEntity entity) {
        return Job.reconstitute(entity.getId(), entity.getType(), entity.getStatus());
    }
}
