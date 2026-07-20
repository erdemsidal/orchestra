package com.orchestra.job.infrastructure.messaging;

import com.orchestra.job.application.ExecuteJobService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * WORKER (consumer) — kuyruktan işleri alıp çalıştırır.
 *
 * @SqsListener: spring-cloud-aws bu metodu kuyruğu dinleyen bir consumer yapar.
 * Arka planda jobs-queue'yu sürekli yoklar; her mesaj için onMessage'ı çağırır.
 *   - Metot NORMAL biterse -> mesajı SQS'ten siler (acknowledge). İş bitti.
 *   - Metot EXCEPTION fırlatırsa -> mesajı SİLMEZ. Visibility timeout dolunca
 *     mesaj geri gelir ve tekrar denenir. İşte "at-least-once" garantisi budur:
 *     iş kaybolmaz, en kötü ihtimalle tekrar işlenir.
 *
 * Worker'ın işi ince: fişi (jobId) al, asıl çalıştırmayı ExecuteJobService'e
 * devret (o zaten start -> run -> markDone/markFailed yapıyor, Faz 1'den).
 *
 * Not: Şimdilik worker ana uygulamayla AYNI süreçte (@Component) çalışıyor.
 * Gerçek dağıtık kurulumda bunu ayrı bir servis olarak deploy edip bağımsız
 * ölçeklerdin (daha çok worker = kuyruk daha hızlı erir) — ama @SqsListener kodu
 * aynı kalır.
 */
@Component
public class JobWorker {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);

    private final ExecuteJobService executeJobService;

    public JobWorker(ExecuteJobService executeJobService) {
        this.executeJobService = executeJobService;
    }

    @SqsListener("${app.sqs.queue-name}")
    public void onMessage(String jobId) {
        log.info("Kuyruktan iş alındı: {}", jobId);
        executeJobService.execute(UUID.fromString(jobId));
        // Metot buraya sorunsuz geldiyse spring-cloud-aws mesajı otomatik siler.
    }
}
