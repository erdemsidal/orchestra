package com.orchestra.job.domain;

import java.util.UUID;

/**
 * Domain katmanının kalbi — bir "iş"i temsil eder.
 *
 * ÖNEMLİ: Bu sınıfta HİÇBİR Spring/JPA anotasyonu yok. Saf Java.
 * "Spring'i silsem bu sınıf ve kuralları ayakta kalır mı?" sorusunun
 * cevabı EVET olmalı — hexagonal mimarinin can alıcı noktası bu.
 *
 * Durum geçiş kuralları (ADR 0001, Seçenek A) bu sınıfın metotlarında yaşar:
 * bir işi yanlış duruma sokmaya çalışırsan metot IllegalStateException fırlatır.
 */
public class Job {

    private final UUID id;
    private final String type;   // işin türü (şimdilik basit bir etiket; simüle iş)
    private JobStatus status;

    /**
     * Yeni bir iş oluşturur. Yeni iş HER ZAMAN PENDING doğar —
     * bu bir kuraldır, dışarıdan başka bir durumla oluşturulamaz.
     */
    public Job(UUID id, String type) {
        this.id = id;
        this.type = type;
        this.status = JobStatus.PENDING;
    }

    /**
     * Var olan bir işi (ör. veritabanından okunan) yeniden kurmak için.
     * Bu durumda durum PENDING olmak zorunda değil.
     * (Bu "reconstitution" constructor'ını persistence katmanı kullanacak.)
     */
    public Job(UUID id, String type, JobStatus status) {
        this.id = id;
        this.type = type;
        this.status = status;
    }

    // ── Durum geçişleri (işin kuralları) ─────────────────────────────

    /**
     * PENDING → RUNNING. İşin çalışmaya başladığını işaretler.
     * Bu, geçiş metodunun ÖRNEĞİ — markDone/markFailed'ı buna bakarak yazacaksın.
     */
    public void start() {
        if (status != JobStatus.PENDING) {
            throw new IllegalStateException(
                    "Sadece PENDING durumundaki iş başlatılabilir. Mevcut durum: " + status);
        }
        this.status = JobStatus.RUNNING;
    }

    /**
     * RUNNING → DONE. İşin başarıyla tamamlandığını işaretler.
     * (start() ile aynı kalıp: kontrol et → ihlalse fırlat → değiştir.)
     */
    public void markDone() {
        if (status != JobStatus.RUNNING) {
            throw new IllegalStateException(
                    "Sadece RUNNING durumundaki iş DONE yapılabilir. Mevcut durum: " + status);
        }
        this.status = JobStatus.DONE;
    }

    /**
     * RUNNING → FAILED. İşin başarısız olduğunu işaretler.
     * (Ön koşul markDone ile aynı: başarı da başarısızlık da ancak çalışan bir işten gelir.)
     */
    public void markFailed() {
        if (status != JobStatus.RUNNING) {
            throw new IllegalStateException(
                    "Sadece RUNNING durumundaki iş FAILED yapılabilir. Mevcut durum: " + status);
        }
        this.status = JobStatus.FAILED;
    }



    // ── Okuyucular (getter) ──────────────────────────────────────────
    // Not: Domain'i Lombok'a bağlamamak için getter'ları elle yazıyoruz;
    // domain katmanının hiçbir dış kütüphaneye ihtiyacı olmasın istiyoruz.

 


    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public JobStatus getStatus() {
        return status;
    }
}
