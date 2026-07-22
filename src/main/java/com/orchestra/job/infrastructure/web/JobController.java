package com.orchestra.job.infrastructure.web;

import com.orchestra.job.application.GetJobService;
import com.orchestra.job.application.SubmitJobService;
import com.orchestra.job.domain.Job;
import com.orchestra.job.infrastructure.metrics.JobMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * İş API'si — HTTP dünyası ile use case'ler arasındaki köprü.
 *
 * Controller'ın işi SADECE çeviri ve yönlendirmedir:
 *   HTTP isteği -> use case çağrısı -> HTTP cevabı
 * Burada iş kuralı YOK, if/else YOK, hesaplama YOK. Kurallar domain'de,
 * akış application'da. Controller "ince" olmalı — buna "thin controller" denir.
 *
 * Sadece 2 endpoint var (CRUD değil): kullanıcı iş gönderir ve durumunu sorar.
 * Bilinçli olarak PUT/DELETE YOK: işin durumu dışarıdan set EDİLEMEZ, ancak
 * domain kurallarıyla (start/markDone/markFailed) ilerler.
 */
@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "İş oluşturma ve durum sorgulama")
public class JobController {

    private final SubmitJobService submitJobService;
    private final GetJobService getJobService;
    private final JobMetrics jobMetrics;

    public JobController(SubmitJobService submitJobService,
                         GetJobService getJobService,
                         JobMetrics jobMetrics) {
        this.submitJobService = submitJobService;
        this.getJobService = getJobService;
        this.jobMetrics = jobMetrics;
    }

    /**
     * Yeni iş gönderir (ASENKRON — Faz 3).
     *
     * İşi oluşturur (PENDING), kuyruğa bırakır ve ANINDA döner. İşi çalıştırmaz;
     * onu arka plandaki worker yapacak. Bu yüzden cevap her zaman PENDING'dir.
     *
     * HTTP 202 Accepted (201 Created değil): "isteğini kabul ettim, arka planda
     * işlenecek, sonucu henüz belli değil." Async iş gönderiminin doğru sinyali.
     * (201 "kaynak oluştu VE hazır" derdi; oysa iş henüz çalışmadı.)
     *
     * Kullanıcı dönen jobId ile GET yapıp durumu takip eder — ilk sorguda büyük
     * ihtimalle PENDING görür (worker daha almamış olabilir): eventual consistency.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Yeni iş gönder (asenkron)",
            description = "İşi PENDING oluşturur, kuyruğa bırakır ve jobId ile hemen döner")
    public JobResponse submit(@Valid @RequestBody CreateJobRequest request) {
        Job job = submitJobService.submit(request.type());
        jobMetrics.recordSubmitted();
        return JobResponse.from(job);
    }

    /**
     * İşin durumunu sorgular.
     * İş yoksa GetJobService JobNotFoundException fırlatır ->
     * GlobalExceptionHandler bunu 404'e çevirir.
     *
     * @Cacheable: sonucu "jobs" bölgesine id anahtarıyla yazar; ikinci kez aynı id
     * gelirse metot çalışmaz, cevap Redis'ten gelir.
     *
     * unless = "sadece TERMINAL işleri cache'le": iş async çalıştığı için (Faz 3)
     * GET'lendiğinde PENDING/RUNNING olabilir — bunlar değişkendir, cache'lersek
     * bayatlar (worker DONE yapınca API hâlâ RUNNING gösterirdi). Bu yüzden sadece
     * DONE/FAILED (değişmez) cache'leniyor; PENDING/RUNNING her seferinde DB'den
     * taze okunuyor. Böylece cache invalidation'a hâlâ gerek yok. (Bkz. ADR 0004.)
     */
    @Cacheable(cacheNames = "jobs", key = "#id", unless = "!#result.status.terminal")
    @GetMapping("/{id}")
    @Operation(summary = "İş durumunu sorgula", description = "jobId ile işin güncel durumunu döner")
    public JobResponse getById(@PathVariable UUID id) {
        return JobResponse.from(getJobService.getById(id));
    }
}
