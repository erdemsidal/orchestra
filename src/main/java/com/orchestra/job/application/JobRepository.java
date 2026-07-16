package com.orchestra.job.application;

import com.orchestra.job.domain.Job;

import java.util.Optional;
import java.util.UUID;

/**
 * PORT — application katmanının dış dünyaya "ihtiyaç listesi".
 *
 * Bu arayüzü application katmanı TANIMLAR, infrastructure katmanı DOLDURUR.
 * Bağımlılık oku içeri bakar: infrastructure → application. Tersi asla.
 *
 * Dikkat et:
 *  - Spring/JPA anotasyonu YOK.
 *  - Domain nesnesi (Job) ile konuşuyor, JobEntity ile değil.
 *  - Sadece BİZİM ihtiyacımız olan 2 metot var. JpaRepository'yi extend etseydik
 *    flush(), saveAll(), lazy loading gibi JPA'nın bütün dünyası buraya sızardı.
 *
 * "Priz" bu. Gerçek santrali (JPA) infrastructure takacak, testte ise
 * jeneratörü (HashMap'li sahte bir sınıf) takacağız.
 */
public interface JobRepository {

    /** İşi kalıcı hale getirir ve kaydedilmiş halini döner. */
    Job save(Job job);

    /** İşi id ile arar. Bulunamazsa Optional.empty() döner (null değil). */
    Optional<Job> findById(UUID id);
}
