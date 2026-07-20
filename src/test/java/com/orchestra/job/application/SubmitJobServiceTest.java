package com.orchestra.job.application;

import com.orchestra.job.domain.Job;
import com.orchestra.job.domain.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SubmitJobService'in BİRİM testi — DB yok, SQS yok.
 *
 * JobQueue tek metotlu bir arayüz olduğu için sahtesini LAMBDA ile veriyoruz:
 * atılan id'leri bir listeye kaydeden minik bir sahte kuyruk.
 */
class SubmitJobServiceTest {

    @Test
    @DisplayName("submit() işi PENDING oluşturur, kaydeder ve id'sini kuyruğa atar")
    void submit_isiOlusturupKuyrugaAtar() {
        // GIVEN — sahte repo, sahte kuyruk (atılan id'leri toplayan liste)
        FakeJobRepository repo = new FakeJobRepository();
        CreateJobService createJobService = new CreateJobService(repo);
        List<UUID> kuyrugaAtilanlar = new ArrayList<>();
        JobQueue fakeQueue = kuyrugaAtilanlar::add;   // enqueue(id) -> listeye ekle
        SubmitJobService service = new SubmitJobService(createJobService, fakeQueue);

        // WHEN
        Job job = service.submit("email-gonder");

        // THEN
        // 1) İş PENDING doğdu (çalıştırılmadı)
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        // 2) Repository'ye kaydedildi
        assertThat(repo.findById(job.getId())).isPresent();
        // 3) Kuyruğa TAM olarak bu işin id'si atıldı
        assertThat(kuyrugaAtilanlar).containsExactly(job.getId());
    }
}
