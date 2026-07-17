package com.orchestra.job.application;

import com.orchestra.job.domain.Job;
import com.orchestra.job.domain.JobNotFoundException;
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

        // RUNNING'e geç ve HEMEN kaydet. Neden run()'dan önce kaydediyoruz?
        //  - taskRunner uzun sürebilir; bu sırada biri GET yaparsa işi RUNNING
        //    görmeli, hâlâ PENDING değil.
        //  - Süreç run() ortasında çökerse durum RUNNING kalır — "hiç başlamamış"
        //    gibi PENDING'de takılı kalmaz. (Faz 3'te bu, yarım kalan işleri
        //    tespit etmek için önemli olacak.)
        job.start();
        jobRepository.save(job);

        try {
            taskRunner.run(job);   // asıl iş burada; başarısızsa RuntimeException fırlatır
            job.markDone();
        } catch (RuntimeException e) {
            // İşin başarısız olması bizim için bir HATA DEĞİL, beklenen bir sonuç.
            // O yüzden istisnayı yukarı fırlatmıyoruz (yoksa çağıran 500 alırdı);
            // işi FAILED işaretleyip normal dönüyoruz. API "iş başarısız oldu"
            // diyen geçerli bir cevap döner, "sunucu patladı" değil.
            log.warn("İş başarısız oldu: id={} sebep={}", jobId, e.getMessage());
            job.markFailed();
        }

        return jobRepository.save(job);
    }
}
