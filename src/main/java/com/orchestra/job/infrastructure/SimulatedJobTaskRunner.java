package com.orchestra.job.infrastructure;

import com.orchestra.job.application.JobTaskRunner;
import com.orchestra.job.domain.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * JobTaskRunner port'unun ÇALIŞMA ANINDAKİ implementasyonu.
 *
 * Gerçek bir iş yapmıyoruz (bu bir vitrin projesi); bir işi SİMÜLE ediyoruz:
 * biraz bekle, sonra belirli bir olasılıkla başarısız ol. Bu, PENDING -> RUNNING
 * -> DONE/FAILED akışının tamamını görmemize yetiyor. Faz 3'te retry, idempotency
 * gibi konuları da bu simülasyonla test edeceğiz.
 *
 * Değerler application.yml'den ayarlanabilir (varsayılanlar aşağıda); böylece
 * demoda başarısızlık oranını yükseltip FAILED durumunu kolayca görebiliriz.
 */
@Component
public class SimulatedJobTaskRunner implements JobTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(SimulatedJobTaskRunner.class);

    private final long minMs;
    private final long maxMs;
    private final double failureRate;

    public SimulatedJobTaskRunner(
            @Value("${app.job.simulated.min-ms:200}") long minMs,
            @Value("${app.job.simulated.max-ms:800}") long maxMs,
            @Value("${app.job.simulated.failure-rate:0.3}") double failureRate) {
        this.minMs = minMs;
        this.maxMs = maxMs;
        this.failureRate = failureRate;
    }

    @Override
    public void run(Job job) {
        long sure = ThreadLocalRandom.current().nextLong(minMs, maxMs + 1);
        log.info("İş çalışıyor: id={} type={} (~{} ms)", job.getId(), job.getType(), sure);

        try {
            Thread.sleep(sure);   // işi yapıyormuş gibi bekle
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("İş kesintiye uğradı", e);
        }

        // Belirli bir olasılıkla "iş başarısız oldu" diye patla.
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new RuntimeException("Simüle edilmiş iş hatası");
        }
    }
}
