package com.orchestra.job.infrastructure.web;

import com.orchestra.job.domain.Job;
import com.orchestra.job.domain.JobStatus;

import java.util.UUID;

/**
 * API'nin dışarıya döndüğü iş temsili.
 *
 * NEDEN JobEntity'yi veya domain Job'ı doğrudan dönmüyoruz? (ADR'lik karar)
 *   1. JobEntity'yi dönersek veritabanı şemamız API sözleşmemiz olur:
 *      tabloya kolon eklediğimiz an API'miz habersiz değişir, istemciler kırılır.
 *   2. Domain Job'ı dönersek iç modelimiz dışarı sızar; ileride Job'a
 *      dışarıya göstermek istemediğimiz bir alan eklersek otomatik sızar.
 *   3. DTO ile API sözleşmesi AÇIK ve BİLİNÇLİ olur: burada ne yazıyorsa o gider.
 *
 * Bu, "iç modelin dış dünyaya sızması" probleminin çözümü.
 */
public record JobResponse(
        UUID id,
        String type,
        JobStatus status
) {
    /** domain Job -> API cevabı. Çeviriyi tek yerde topluyoruz. */
    public static JobResponse from(Job job) {
        return new JobResponse(job.getId(), job.getType(), job.getStatus());
    }
}
