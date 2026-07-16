package com.orchestra.job.application;

import com.orchestra.job.domain.Job;
import com.orchestra.job.domain.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CreateJobService'in BİRİM testleri.
 *
 * İşte port'un asıl ödülü: bu use case bir şeye "kaydetmek" zorunda,
 * ama testte gerçek Postgres yerine FakeJobRepository (HashMap) veriyoruz.
 * Docker yok, Spring context yok, milisaniyede koşuyor.
 */
class CreateJobServiceTest {

    @Test
    @DisplayName("create() yeni işi PENDING durumunda oluşturur")
    void create_isiPendingOlarakOlusturur() {
        // GIVEN — sahte repository ile kurulmuş servis
        CreateJobService service = new CreateJobService(new FakeJobRepository());

        // WHEN — yeni iş oluştur
        Job job = service.create("email-gonder");

        // THEN — iş PENDING doğmuş olmalı ve türü verdiğimiz gibi olmalı
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getType()).isEqualTo("email-gonder");
        assertThat(job.getId()).isNotNull();
    }

    @Test
    @DisplayName("create() işi repository'ye kaydeder")
    void create_isiRepositoryyeKaydeder() {
        // GIVEN — sahte repository'yi elimizde tutuyoruz ki sonradan sorgulayabilelim
        FakeJobRepository repo = new FakeJobRepository();
        CreateJobService service = new CreateJobService(repo);

        // WHEN
        Job job = service.create("email-gonder");

        // THEN — gerçekten kaydedilmiş mi? Sahte DB'den geri okuyup doğruluyoruz.
        assertThat(repo.kayitSayisi()).isEqualTo(1);
        assertThat(repo.findById(job.getId())).isPresent();
    }
}
