package com.orchestra.job;

import com.orchestra.job.infrastructure.web.CreateJobRequest;
import com.orchestra.job.infrastructure.web.JobResponse;
import com.orchestra.job.domain.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ENTEGRASYON TESTİ — bütün katmanlar birlikte, GERÇEK Postgres ile.
 *
 * Birim testlerinden farkı:
 *   - JobTest / CreateJobServiceTest: tek sınıf, sahte bağımlılık, ~10 ms.
 *     "Kuralım doğru mu?" sorusunu cevaplar.
 *   - Bu test: Tomcat + Spring + Hibernate + Flyway + gerçek Postgres, ~5-10 sn.
 *     "Parçalar birbirine DOĞRU bağlanmış mı?" sorusunu cevaplar.
 *
 * İkisi farklı iş yapar; birbirinin yerine geçmez. Test piramidi:
 * altta bol hızlı birim testi, tepede az sayıda böyle pahalı test.
 *
 * TESTCONTAINERS NASIL ÇALIŞIYOR?
 *   @Container    -> Test başlamadan Docker'da gerçek bir Postgres konteyneri
 *                    ayağa kaldırır, testler bitince siler. Elle "docker compose up"
 *                    gerekmez, CI'da da aynı şekilde çalışır.
 *   @ServiceConnection -> Spring'e "veritabanı URL/kullanıcı/şifresini bu konteynerden
 *                    al" der. application.yml'deki ayarları otomatik ezer.
 *                    (Eskiden bunu elle @DynamicPropertySource ile yazmak gerekirdi.)
 *
 * Not: H2 gibi sahte bir DB yerine GERÇEK Postgres kullanıyoruz. Neden?
 * H2, Postgres'in UUID tipini, indekslerini, SQL lehçesini birebir taklit etmez;
 * "testte geçti, prod'da patladı" klasiği oradan çıkar.
 */
// Simüle runner'ın rastgeleliğini testte KAPATIYORUZ: hata oranı 0, bekleme 0.
// Neden? Entegrasyon testi deterministik olmalı. Gerçek runner %30 ihtimalle
// patlıyor ve rastgele süre bekliyor; bunu açık bırakırsak test bazen geçer
// bazen kalır (flaky test) ve hızı da düşer. Bu ayarlarla iş her zaman anında
// başarılı olur, biz de create+execute+kaydet+oku zincirini kararlı test ederiz.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.job.simulated.failure-rate=0",
                "app.job.simulated.min-ms=0",
                "app.job.simulated.max-ms=0"
        })
@Testcontainers
class JobApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("POST /api/jobs işi oluşturur, senkron çalıştırır ve DONE döner")
    void postJobs_isiOlusturupCalistirir() {
        // WHEN — gerçek HTTP isteği (Tomcat çalışıyor)
        ResponseEntity<JobResponse> response = restTemplate.postForEntity(
                "/api/jobs",
                new CreateJobRequest("email-gonder"),
                JobResponse.class);

        // THEN — 201 Created ve iş DONE (senkron çalıştı; hata oranı 0'a sabitli)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(JobStatus.DONE);
        assertThat(response.getBody().type()).isEqualTo("email-gonder");
        assertThat(response.getBody().id()).isNotNull();
    }

    @Test
    @DisplayName("POST sonrası GET /api/jobs/{id} aynı işi döner (DB'den okuyarak)")
    void getJobs_kaydedilenIsiDoner() {
        // GIVEN — önce bir iş oluştur (gerçekten Postgres'e yazılır)
        JobResponse olusturulan = restTemplate.postForEntity(
                "/api/jobs",
                new CreateJobRequest("rapor-uret"),
                JobResponse.class).getBody();
        assertThat(olusturulan).isNotNull();

        // WHEN — id ile sorgula (gerçekten Postgres'ten okunur)
        ResponseEntity<JobResponse> response = restTemplate.getForEntity(
                "/api/jobs/" + olusturulan.id(),
                JobResponse.class);

        // THEN — aynı iş dönmeli
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(olusturulan.id());
        assertThat(response.getBody().type()).isEqualTo("rapor-uret");
    }

    @Test
    @DisplayName("GET /api/jobs/{id} olmayan iş için 404 döner")
    void getJobs_olmayanIs_404Doner() {
        // WHEN — var olmayan bir id sorgula
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/jobs/" + UUID.randomUUID(),
                String.class);

        // THEN — JobNotFoundException -> GlobalExceptionHandler -> 404
        // Katmanların doğru bağlandığının kanıtı: application HTTP bilmiyordu,
        // istisnayı infrastructure 404'e çevirdi.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("POST /api/jobs boş type için 400 döner")
    void postJobs_bosType_400Doner() {
        // WHEN — geçersiz gövde
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/jobs",
                new CreateJobRequest(""),
                String.class);

        // THEN — @Valid devreye girer, 400
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
