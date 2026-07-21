package com.orchestra.job.application;

import com.orchestra.job.domain.Job;
import com.orchestra.job.domain.JobNotFoundException;
import com.orchestra.job.domain.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * USE CASE: Bir işi çalıştır.
 *
 * Domain'e yazdığımız start()/markDone()/markFailed() metotlarını İLK KEZ
 * burası çağırıyor. Yani "iş nasıl ilerliyor" sorusunun cevabı bu sınıf.
 *
 * Akış: PENDING işi bul -> RUNNING yap -> çalıştır -> DONE ya da FAILED.
 *
 * IDEMPOTENT (bkz. ADR 0007): SQS at-least-once teslimat yapar, aynı mesaj iki
 * kez gelebilir. İş zaten PENDING değilse (yani başlamış/bitmiş), tekrar
 * çalıştırmıyoruz — sessizce dönüyoruz. İşin durumu bizim idempotency
 * anahtarımız: bir iş yalnızca bir kez PENDING->RUNNING geçebilir.
 */
public class ExecuteJobService {

    private static final Logger log = LoggerFactory.getLogger(ExecuteJobService.class);

    private final JobRepository jobRepository;
    private final JobTaskRunner taskRunner;

    public ExecuteJobService(JobRepository jobRepository, JobTaskRunner taskRunner) {
        this.jobRepository = jobRepository;
        this.taskRunner = taskRunner;
    }

    public Job execute(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        // IDEMPOTENCY: terminal (DONE/FAILED) iş bir daha çalışmaz. Duplicate mesaj
        // gelirse sessizce atla (worker mesajı silsin). Bkz. ADR 0007.
        if (job.getStatus().isTerminal()) {
            log.info("Terminal iş atlandı (idempotency): id={} durum={}", jobId, job.getStatus());
            return job;
        }

        // PENDING ise başlat + kaydet. RUNNING ise bu bir RETRY'dır (önceki deneme
        // bitmeden mesaj tekrar geldi) — start() yalnızca PENDING'den çalışır, o
        // yüzden tekrar çağırmıyoruz; doğrudan yeniden çalıştırmayı deniyoruz.
        if (job.getStatus() == JobStatus.PENDING) {
            job.start();
            jobRepository.save(job);
        }

        try {
            taskRunner.run(job);   // asıl iş; başarısızsa istisna fırlatır
            job.markDone();
            return jobRepository.save(job);
        } catch (TransientJobException e) {
            // GEÇİCİ hata: FAILED yapma, YUKARI FIRLAT. Worker mesajı silmez;
            // SQS visibility timeout sonrası tekrar teslim eder (retry). 3 denemede
            // hâlâ başarısızsa redrive policy mesajı DLQ'ya taşır. İş RUNNING kalır.
            log.warn("Geçici hata, retry için yeniden fırlatılıyor: id={} sebep={}", jobId, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            // KALICI hata: tekrar denemek boşuna. FAILED işaretle, mesaj silinsin.
            log.warn("Kalıcı hata, iş FAILED: id={} sebep={}", jobId, e.getMessage());
            job.markFailed();
            return jobRepository.save(job);
        }
    }
}
