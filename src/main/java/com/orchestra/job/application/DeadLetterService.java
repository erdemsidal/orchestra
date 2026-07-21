package com.orchestra.job.application;

import com.orchestra.job.domain.Job;
import com.orchestra.job.domain.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * USE CASE: DLQ'ya düşen bir işi ele al.
 *
 * Bir mesaj 3 kez denenip hâlâ işlenemeyince SQS onu DLQ'ya taşır. O iş, retry'lar
 * boyunca RUNNING'de takılı kaldı. Burada onu kalıcı olarak FAILED işaretliyoruz
 * ki "sonsuza dek RUNNING" gibi görünmesin ve durumu net olsun.
 *
 * Gerçek sistemde burada ayrıca alarm/bildirim (ops ekibine "şu iş öldü") olurdu.
 */
public class DeadLetterService {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterService.class);

    private final JobRepository jobRepository;

    public DeadLetterService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public void handleDeadLetter(UUID jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("DLQ: iş bulunamadı, atlanıyor: id={}", jobId);
            return;
        }
        if (job.getStatus() == JobStatus.RUNNING) {
            job.markFailed();
            jobRepository.save(job);
        }
        log.error("İş DLQ'ya düştü — kalıcı hata, FAILED işaretlendi: id={}", jobId);
    }
}
