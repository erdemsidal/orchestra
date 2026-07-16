package com.orchestra.job.infrastructure.web;

import com.orchestra.job.application.CreateJobService;
import com.orchestra.job.application.GetJobService;
import com.orchestra.job.domain.Job;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    private final GetJobService getJobService;

    public JobController(CreateJobService createJobService, GetJobService getJobService) {
        this.createJobService = createJobService;
        this.getJobService = getJobService;
    }

    /**
     * Yeni iş oluşturur.
     * 201 Created döner — "kaynak yaratıldı" demenin doğru HTTP yolu (200 değil).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Yeni iş oluştur", description = "İşi PENDING durumunda oluşturur ve jobId döner")
    public JobResponse create(@Valid @RequestBody CreateJobRequest request) {
        Job job = createJobService.create(request.type());
        return JobResponse.from(job);
    }

    /**
     * İşin durumunu sorgular.
     * İş yoksa GetJobService JobNotFoundException fırlatır ->
     * GlobalExceptionHandler bunu 404'e çevirir.
     */
    @GetMapping("/{id}")
    @Operation(summary = "İş durumunu sorgula", description = "jobId ile işin güncel durumunu döner")
    public JobResponse getById(@PathVariable UUID id) {
        return JobResponse.from(getJobService.getById(id));
    }
}
