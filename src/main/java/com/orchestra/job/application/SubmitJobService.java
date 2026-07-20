package com.orchestra.job.application;

import com.orchestra.job.domain.Job;

/**
 * USE CASE: Bir iş gönder (submit).
 *
 * Faz 3'te kullanıcının "iş gönderme" eylemi artık şu: işi oluştur (PENDING),
 * sonra kuyruğa bırak. İşi ÇALIŞTIRMAZ — onu worker yapacak. Böylece POST
 * anında dönebilir (senkron bekleme yok).
 *
 * Not (dual-write problemi): önce DB'ye kaydediyoruz, sonra kuyruğa atıyoruz.
 * İkisi ayrı sistem; kayıt başarılı olup enqueue patlarsa iş PENDING'de kalır
 * (kuyrukta mesajı olmadan). Gerçek çözümü "transactional outbox" desenidir;
 * şimdilik basit tutuyoruz, enqueue patlarsa POST hata döner. (İleride ADR.)
 */
public class SubmitJobService {

    private final CreateJobService createJobService;
    private final JobQueue jobQueue;

    public SubmitJobService(CreateJobService createJobService, JobQueue jobQueue) {
        this.createJobService = createJobService;
        this.jobQueue = jobQueue;
    }

    public Job submit(String type) {
        Job job = createJobService.create(type);  // PENDING oluştur + kaydet
        jobQueue.enqueue(job.getId());             // kuyruğa jobId bırak
        return job;                                 // PENDING olarak geri dön
    }
}
