package com.orchestra.job.application;

import com.orchestra.job.domain.Job;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JobRepository port'unun TEST için sahte (fake) implementasyonu.
 *
 * "Veritabanı" burada sadece bir HashMap. Postgres yok, JPA yok, Docker yok.
 * Priz analojisinde bu, santral yerine taktığımız küçük jeneratör.
 *
 * Not: Bu sınıf src/test altında — üretim koduna hiç karışmaz, sadece testler görür.
 */
class FakeJobRepository implements JobRepository {

    private final Map<UUID, Job> db = new HashMap<>();

    @Override
    public Job save(Job job) {
        db.put(job.getId(), job);
        return job;
    }

    @Override
    public Optional<Job> findById(UUID id) {
        return Optional.ofNullable(db.get(id));
    }

    /** Testlerde işe yarayan küçük yardımcı: kaç iş kaydedildi? */
    int kayitSayisi() {
        return db.size();
    }
}
