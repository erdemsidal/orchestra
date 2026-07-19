package com.orchestra.job.infrastructure.web;

import com.orchestra.job.application.CreateJobService;
import com.orchestra.job.application.ExecuteJobService;
import com.orchestra.job.application.GetJobService;
import com.orchestra.job.domain.Job;
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

    private final CreateJobService createJobService;
    private final ExecuteJobService executeJobService;
    private final GetJobService getJobService;

    public JobController(CreateJobService createJobService,
                         ExecuteJobService executeJobService,
                         GetJobService getJobService) {
        this.createJobService = createJobService;
        this.executeJobService = executeJobService;
        this.getJobService = getJobService;
    }

    /**
     * Yeni iş oluşturur ve SENKRON olarak çalıştırır.
     *
     * Faz 1 tercihi (bkz. yol haritası): işi isteğin İÇİNDE çalıştırıyoruz.
     * Yani 5 saniyelik bir iş = 5 saniye bekleyen bir HTTP isteği, ve cevap
     * zaten DONE/FAILED döner (kullanıcı PENDING'i göremez). Bu bilinçli bir
     * "problem" — Faz 3'te araya kuyruk koyup create ve execute'i ayıracağız;
     * o zaman istek anında dönecek, işi arka planda worker çalıştıracak.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Yeni iş oluştur ve çalıştır",
            description = "İşi oluşturur, senkron çalıştırır ve son durumuyla (DONE/FAILED) döner")
    public JobResponse create(@Valid @RequestBody CreateJobRequest request) {
        Job created = createJobService.create(request.type());
        Job finished = executeJobService.execute(created.getId());
        return JobResponse.from(finished);
    }

    /**
     * İşin durumunu sorgular.
     * İş yoksa GetJobService JobNotFoundException fırlatır ->
     * GlobalExceptionHandler bunu 404'e çevirir.
     *
     * @Cacheable: bu metodun sonucunu "jobs" cache bölgesine, anahtar olarak id ile
     * yazar. İkinci kez aynı id gelirse metot HİÇ çalışmaz — Spring cevabı doğrudan
     * Redis'ten döner, DB'ye gidilmez. Cache invalidation'a gerek yok çünkü
     * cache'lediğimiz iş terminal (değişmez); TTL (5 dk) emniyet kemeri. (Bkz. ADR 0004.)
     */
    @Cacheable(cacheNames = "jobs", key = "#id")
    @GetMapping("/{id}")
    @Operation(summary = "İş durumunu sorgula", description = "jobId ile işin güncel durumunu döner")
    public JobResponse getById(@PathVariable UUID id) {
        return JobResponse.from(getJobService.getById(id));
    }
}
