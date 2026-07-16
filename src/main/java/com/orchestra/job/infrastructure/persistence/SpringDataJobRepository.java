package com.orchestra.job.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA arayüzü — SQL'i bizim yerimize Spring üretir.
 *
 * DİKKAT: Bu, application katmanındaki JobRepository PORT'u DEĞİL.
 *   - Bu arayüz Spring'e aittir, JobEntity ile konuşur, JPA'nın bütün
 *     dünyasını (flush, lazy loading, detached entity...) beraberinde getirir.
 *   - Port ise BİZE aittir, domain Job ile konuşur, sadece 2 metodu vardır.
 *
 * İkisini JpaJobRepositoryAdapter birbirine bağlar. Bu sınıfı application
 * katmanı ASLA görmez — bu yüzden burada, infrastructure'da duruyor.
 *
 * Gövdesi neden boş? save/findById gibi metotları JpaRepository'den miras alıyor;
 * Spring çalışma anında bu arayüzün implementasyonunu kendisi üretiyor.
 */
interface SpringDataJobRepository extends JpaRepository<JobEntity, UUID> {
}
