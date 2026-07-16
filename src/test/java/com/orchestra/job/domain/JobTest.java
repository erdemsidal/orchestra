package com.orchestra.job.domain;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Job domain nesnesinin BİRİM testleri.
 *
 * DİKKAT: Burada @SpringBootTest YOK, veritabanı YOK, hiçbir dış bağımlılık YOK.
 * Sadece "new Job(...)" deyip metotlarını çağırıyoruz. Bu testler milisaniyede
 * koşar. İşte "domain'i Spring'den ayırmanın" somut ödülü bu.
 */
class JobTest {

    /** Testlerde tekrar eden "yeni iş" kurulumu — okunurluk için küçük bir yardımcı. */
    private Job yeniIs() {
        return new Job(UUID.randomUUID(), "email-gonder");
    }

    // ═══════════════════════════════════════════════════════════════
    // GEÇERLİ GEÇİŞLER — kuralın izin verdiği yollar
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Yeni oluşturulan iş PENDING durumundadır")
    void yeniIs_pendingDurumundaDogar() {
        // GIVEN + WHEN — yeni bir iş oluştur
        Job job = yeniIs();

        // THEN — durumu PENDING olmalı
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    @DisplayName("start Pending ise RUNNING yapar")
    void start_pendingIse_runningYapar() {
        // GIVEN — yeni iş (PENDING)
        Job job = yeniIs();

        // WHEN — işi başlat
        job.start();

        // THEN — durumu RUNNING olmalı
        assertThat(job.getStatus()).isEqualTo(JobStatus.RUNNING);
    }

    @Test
    @DisplayName("markDone RUNNING ise DONE yapar")
    void markDone_runningIse_doneYapar() {
        // GIVEN — başlatılmış iş (artık RUNNING)
        Job job = yeniIs();
        job.start();

        // WHEN — işi tamamlandı olarak işaretle
        job.markDone();

        // THEN — durumu DONE olmalı
        assertThat(job.getStatus()).isEqualTo(JobStatus.DONE);
    }

    @Test
    @DisplayName("markFailed RUNNING ise FAILED yapar")
    void markFailed_runningIse_failedYapar() {
        // GIVEN — başlatılmış iş (artık RUNNING)
        Job job = yeniIs();
        job.start();

        // WHEN — işi başarısız olarak işaretle
        job.markFailed();

        // THEN — durumu FAILED olmalı
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
    }

    // ═══════════════════════════════════════════════════════════════
    // KURAL İHLALLERİ — geçersiz geçişte nesne kendini korumalı
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PENDING işe markDone() çağrılırsa IllegalStateException fırlar")
    void markDone_pendingIse_istisnaFirlatir() {
        // GIVEN — yeni iş, henüz PENDING (start() çağırmadık, RUNNING değil)
        Job job = yeniIs();

        // WHEN + THEN — markDone() çağrısı istisna fırlatmalı.
        // assertThatThrownBy içindeki lambda ( () -> ... ) çalıştırılır ve
        // bir istisna beklenir; fırlamazsa test BAŞARISIZ olur.
        assertThatThrownBy(() -> job.markDone())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Zaten RUNNING olan işe start() çağrılırsa istisna fırlar")
    void start_zatenRunningIse_istisnaFirlatir() {
        // GIVEN — başlatılmış iş (RUNNING)
        Job job = yeniIs();
        job.start();

        // WHEN + THEN — ikinci kez başlatmak kural ihlali
        assertThatThrownBy(() -> job.start())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("DONE (terminal) işe start() çağrılırsa istisna fırlar")
    void start_doneIse_istisnaFirlatir() {
        // GIVEN — tamamlanmış iş (DONE — terminal durum)
        Job job = yeniIs();
        job.start();
        job.markDone();

        // WHEN + THEN — biten iş yeniden başlatılamaz
        assertThatThrownBy(() -> job.start())
                .isInstanceOf(IllegalStateException.class);
    }
}
