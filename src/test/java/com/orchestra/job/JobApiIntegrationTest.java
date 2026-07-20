package com.orchestra.job;

import com.orchestra.job.application.JobQueue;
import com.orchestra.job.infrastructure.web.CreateJobRequest;
import com.orchestra.job.infrastructure.web.JobResponse;
import com.orchestra.job.domain.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
// Bu test yalnızca Postgres'i (Testcontainers) gerçek kaldırıyor. Diğer dış
// bağımlılıkları devre dışı bırakıyoruz:
//  - spring.cache.type=none  -> @Cacheable Redis'e bağlanmaya çalışmasın.
//  - JobQueue @MockitoBean    -> POST artık SQS'e mesaj atıyor; sahte kuyruk ile
//    LocalStack'e ihtiyaç kalmıyor. Bu testin derdi "katmanlar bağlanmış mı",
//    gerçek SQS entegrasyonu değil.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cache.type=none")
@Testcontainers
@Tag("integration")   // Docker gerektirir; CI'da "integration" grubu şimdilik hariç tutuluyor
class JobApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    // Gerçek SQS yerine sahte kuyruk: enqueue çağrıları hiçbir şey yapmaz.
    @MockitoBean
    private JobQueue jobQueue;

    @Test
    @DisplayName("POST /api/jobs işi PENDING oluşturur, kuyruğa atar ve 202 döner")
    void postJobs_isiGonderir() {
        // WHEN — gerçek HTTP isteği (Tomcat çalışıyor)
        ResponseEntity<JobResponse> response = restTemplate.postForEntity(
                "/api/jobs",
                new CreateJobRequest("email-gonder"),
                JobResponse.class);

        // THEN — 202 Accepted ve iş PENDING (async: worker daha çalıştırmadı)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(JobStatus.PENDING);
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
