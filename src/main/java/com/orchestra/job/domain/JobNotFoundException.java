package com.orchestra.job.domain;

import java.util.UUID;

/**
 * Aranan iş bulunamadı.
 *
 * DİKKAT: Burada @ResponseStatus / HttpStatus YOK — bilinçli bir tercih.
 * "404" bir HTTP kavramıdır; domain ve application katmanları HTTP'yi bilmez.
 * Faz 3'te bu use case'leri HTTP'si olmayan bir worker da çağıracak; orada
 * "404" hiçbir anlam ifade etmez.
 *
 * Bu istisnayı HTTP 404'e çevirmek infrastructure'ın işi
 * (bkz. GlobalExceptionHandler).
 */
public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(UUID id) {
        super("İş bulunamadı: id = " + id);
    }
}
