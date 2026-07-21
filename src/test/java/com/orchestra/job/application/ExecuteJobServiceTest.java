package com.orchestra.job.application;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.orchestra.job.domain.Job;
import com.orchestra.job.domain.JobNotFoundException;
import com.orchestra.job.domain.JobStatus;

/**
 * ExecuteJobService'in BİRİM testleri — yine DB'siz, gerçek runner olmadan.
 *
 * JobTaskRunner tek metotlu bir arayüz olduğu için sahtesini LAMBDA ile
 * yazabiliyoruz:
 *   job -> { }                    -> hiçbir şey yapmaz = başarı
 *   job -> { throw new ... }      -> patlar = başarısızlık
 * Böylece "iş başarılı olursa" ve "iş patlarsa" senaryolarını, gerçekten
 * bir şey çalıştırmadan, Thread.sleep beklemeden, kesin kontrol altında test ederiz.
 */
class ExecuteJobServiceTest {

    @Test
    @DisplayName("execute() başarılı işi DONE yapar")
    void execute_basariliIs_doneYapar() {
        // GIVEN — repoda PENDING bir iş ve HİÇ patlamayan bir runner
        FakeJobRepository repo = new FakeJobRepository();
        Job job = new Job(UUID.randomUUID(), "email-gonder");
        repo.save(job);
        JobTaskRunner basariliRunner = j -> { /* başarı: sessizce döner */ };
        ExecuteJobService service = new ExecuteJobService(repo, basariliRunner);

        // WHEN
        Job sonuc = service.execute(job.getId());

        // THEN — iş DONE olmalı ve bu durum repoya da yazılmış olmalı
        assertThat(sonuc.getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(repo.findById(job.getId()).orElseThrow().getStatus())
                .isEqualTo(JobStatus.DONE);
    }

    @Test
    @DisplayName("execute() olmayan iş için JobNotFoundException fırlatır")
    void execute_olmayanIs_istisnaFirlatir() {
        // GIVEN — boş repo, runner'ın önemi yok
        ExecuteJobService service = new ExecuteJobService(new FakeJobRepository(), j -> { });

        // WHEN + THEN
        assertThatThrownBy(() -> service.execute(UUID.randomUUID()))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    @DisplayName("execute() başarısız işi FAILED yapar (istisna fırlatmadan)")
    void execute_basarisizIs_failedYapar() {
        // GIVEN — repoda PENDING bir iş ve HER ZAMAN patlayan bir runner
        FakeJobRepository repo = new FakeJobRepository();
        Job job = new Job(UUID.randomUUID(), "email-gonder");
        repo.save(job);
        JobTaskRunner patlayanRunner = j -> { throw new RuntimeException("patladı"); };
        ExecuteJobService service = new ExecuteJobService(repo, patlayanRunner);

        // WHEN — executor istisnayı yakalar, yukarı fırlatmaz
        Job sonuc = service.execute(job.getId());

        // THEN — iş FAILED olmalı, hem dönen sonuçta hem repoda
        assertThat(sonuc.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(repo.findById(job.getId()).orElseThrow().getStatus())
                .isEqualTo(JobStatus.FAILED);
    }

    @Test
    @DisplayName("execute() geçici hatada FAILED yapmaz, istisnayı fırlatır (retry için)")
    void execute_geciciHata_yenidenFirlatir() {
        // GIVEN — PENDING iş ve GEÇİCİ hata fırlatan runner
        FakeJobRepository repo = new FakeJobRepository();
        Job job = new Job(UUID.randomUUID(), "email-gonder");
        repo.save(job);
        JobTaskRunner geciciHataRunner = j -> { throw new TransientJobException("geçici"); };
        ExecuteJobService service = new ExecuteJobService(repo, geciciHataRunner);

        // WHEN + THEN — istisna YUKARI fırlar (worker'a "tekrar dene" sinyali)
        assertThatThrownBy(() -> service.execute(job.getId()))
                .isInstanceOf(TransientJobException.class);
        // İş FAILED OLMADI, RUNNING kaldı — bir sonraki teslimatta retry edilecek
        assertThat(repo.findById(job.getId()).orElseThrow().getStatus())
                .isEqualTo(JobStatus.RUNNING);
    }

    @Test
    @DisplayName("execute() duplicate'i atlar: terminal işte runner'ı ÇAĞIRMAZ (idempotency)")
    void execute_zatenTerminalIs_runnerCagirmaz() {
        // GIVEN — repoda zaten DONE olan bir iş (bir önceki teslimatta işlenmiş)
        FakeJobRepository repo = new FakeJobRepository();
        Job job = new Job(UUID.randomUUID(), "email-gonder");
        job.start();
        job.markDone();          // artık DONE (terminal)
        repo.save(job);
        // Runner çağrılırsa testi patlatan bir "tuzak" runner: çağrılMAmalı.
        boolean[] runnerCagrildi = {false};
        JobTaskRunner tuzakRunner = j -> runnerCagrildi[0] = true;
        ExecuteJobService service = new ExecuteJobService(repo, tuzakRunner);

        // WHEN — aynı iş ikinci kez execute'a gelirse (duplicate mesaj)
        Job sonuc = service.execute(job.getId());

        // THEN — runner HİÇ çağrılmadı (iş ikinci kez çalışmadı), durum DONE kaldı
        assertThat(runnerCagrildi[0]).isFalse();
        assertThat(sonuc.getStatus()).isEqualTo(JobStatus.DONE);
    }
}
