package com.orchestra.job.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/jobs isteğinin gövdesi.
 *
 * record: Java 16+ ile gelen, "sadece veri taşıyan" sınıflar için kısa yazım.
 * Otomatik olarak constructor, getter (type()), equals/hashCode/toString üretir.
 * DTO'lar tam da bunun için ideal — davranışı yok, sadece veri.
 *
 * @NotBlank / @Size: Controller'da @Valid ile birlikte çalışır. Kural ihlalinde
 * GlobalExceptionHandler devreye girip 400 + alan bazlı hata mesajı döner.
 * Bu "girdi doğrulama"dır (dış dünyadan gelen çöpe karşı savunma) —
 * domain'deki "iş kuralı"ndan farklıdır, karıştırma.
 */
public record CreateJobRequest(

        @NotBlank(message = "type alanı boş olamaz")
        @Size(max = 100, message = "type en fazla 100 karakter olabilir")
        String type
) {
}
