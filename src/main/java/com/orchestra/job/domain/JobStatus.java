package com.orchestra.job.domain;

/**
 * Bir işin yaşam döngüsündeki durumlar.
 *
 * İzinli geçişler:
 *   PENDING → RUNNING → DONE
 *                    └→ FAILED
 *
 * DONE ve FAILED "terminal" durumlardır: bir iş bunlara ulaşınca artık
 * durumu değişmez.
 *
 * Not: Bu enum saf Java'dır — Spring/JPA anotasyonu içermez. Geçiş
 * KURALLARINI burada değil, Job sınıfında tutuyoruz (bkz. ADR 0001).
 */
public enum JobStatus {
    PENDING,
    RUNNING,
    DONE,
    FAILED
}
