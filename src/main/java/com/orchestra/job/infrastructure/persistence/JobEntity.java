package com.orchestra.job.infrastructure.persistence;

import com.orchestra.common.audit.Auditable;
import com.orchestra.job.domain.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * jobs tablosunun JPA karşılığı.
 *
 * NEDEN domain Job'dan AYRI bir sınıf?
 *   Çünkü JPA'nın kendi şartları var ve bu şartlar domain Job'un korumalarını delerdi:
 *     - Argümansız (no-arg) bir constructor ZORUNLU — JPA nesneyi refleksiyonla kurar.
 *       Ama domain Job'da "yeni iş PENDING doğar" kuralını constructor'a gömmüştük;
 *       boş constructor eklesek o kural delinirdi.
 *     - Setter'lar gerekir — domain Job'da bilerek setter YOK, durum sadece
 *       start()/markDone()/markFailed() ile kurallara uyarak değişir.
 *     - final alan kullanılamaz — domain Job'da id ve type final.
 *
 *   Yani bu iki sınıfı birleştirseydik, JPA'yı memnun etmek için domain'in
 *   bütün korumalarını sökmek zorunda kalırdık. Ayırdık: JobEntity JPA'nın
 *   kurallarına uyar, Job işin kurallarına uyar. Aralarını Adapter çevirir.
 *
 * Auditable'ı extend ediyor -> created_at / updated_at otomatik dolar.
 */
@Entity
@Table(name = "jobs")
public class JobEntity extends Auditable {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String type;

    // EnumType.STRING -> veritabanına "PENDING" yazılır, 0/1/2 değil.
    // ORDINAL (sayı) kullansaydık enum sırası değişince eski kayıtlar bozulurdu.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    /** JPA için zorunlu no-arg constructor. Kod içinden kullanılmaz. */
    protected JobEntity() {
    }

    public JobEntity(UUID id, String type, JobStatus status) {
        this.id = id;
        this.type = type;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }
}
