package com.orchestra.job.application;

import java.util.UUID;

/**
 * PORT — "işi bir kuyruğa bırakmak" için application'ın ihtiyaç listesi.
 *
 * JobRepository ile aynı mantık: application bu arayüzü TANIMLAR, ama SQS'i,
 * AWS'i, mesaj formatını BİLMEZ. O detaylar infrastructure'daki adapter'da
 * (SqsJobQueue) yaşar. Yarın SQS'ten RabbitMQ'ya geçsek application değişmez.
 *
 * Kuyruğa işin KENDİSİNİ değil, sadece KİMLİĞİNİ (jobId) koyuyoruz. Worker
 * bu id ile işi veritabanından okuyacak. Neden? Mesajlar küçük kalsın ve tek
 * doğruluk kaynağı DB olsun (mesajın içindeki kopya veri bayatlayabilir).
 */
public interface JobQueue {

    /** Verilen işi işlenmek üzere kuyruğa bırakır. */
    void enqueue(UUID jobId);
}
