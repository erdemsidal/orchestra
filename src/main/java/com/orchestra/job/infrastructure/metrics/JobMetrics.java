package com.orchestra.job.infrastructure.metrics;

import com.orchestra.job.domain.JobStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * İşe ÖZEL metrikler (Micrometer sayaçları).
 *
 * Genel metrikler (HTTP, JVM) Spring'den otomatik gelir; ama "kaç iş geldi/bitti"
 * gibi iş mantığına özel şeyleri biz kaydetmeliyiz. Bu bileşen infrastructure'da
 * çünkü Micrometer bir framework detayı — domain/application temiz kalsın.
 *
 * Sayaçlar /actuator/prometheus'ta şu adlarla çıkar (Prometheus/Grafana'da):
 *   jobs_submitted_total
 *   jobs_completed_total{result="done"|"failed"}
 *   jobs_dead_lettered_total
 */
@Component
public class JobMetrics {

    private final MeterRegistry registry;
    private final Counter submitted;
    private final Counter deadLettered;

    public JobMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.submitted = Counter.builder("jobs.submitted")
                .description("Gönderilen (kuyruğa bırakılan) iş sayısı")
                .register(registry);
        this.deadLettered = Counter.builder("jobs.dead_lettered")
                .description("DLQ'ya düşen iş sayısı")
                .register(registry);
    }

    /** Her POST /jobs'ta. */
    public void recordSubmitted() {
        submitted.increment();
    }

    /** Bir iş terminal (DONE/FAILED) olunca. result etiketiyle ayrılır. */
    public void recordCompleted(JobStatus status) {
        // Locale.ROOT: makineye giden etiket; Türkçe locale'de "FAILED" -> "faıled"
        // olmasın (I -> ı). Locale'e bağlı toLowerCase yalnızca kullanıcı metni için.
        registry.counter("jobs.completed", "result", status.name().toLowerCase(Locale.ROOT)).increment();
    }

    /** Bir iş DLQ'ya düşünce. */
    public void recordDeadLettered() {
        deadLettered.increment();
    }
}
