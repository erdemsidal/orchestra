-- ═══════════════════════════════════════════════════════════
-- V1 — jobs tablosu
--
-- Flyway bu dosyayı uygulama açılışında bir kez çalıştırır ve
-- flyway_schema_history tablosuna "V1 uygulandı" diye yazar.
-- Bir daha asla çalıştırmaz. Şema değişikliği gerekirse V2__... yazılır;
-- BU DOSYA bir daha DEĞİŞTİRİLMEZ (checksum tutmazsa Flyway hata verir).
-- ═══════════════════════════════════════════════════════════

CREATE TABLE jobs (
    -- Kimliği uygulama üretiyor (UUID.randomUUID()), veritabanı değil.
    -- Neden? Domain'de Job'ı kaydetmeden ÖNCE id'si olsun istiyoruz;
    -- böylece application katmanı DB'ye "id ver" diye sormak zorunda kalmaz.
    id          UUID         PRIMARY KEY,

    type        VARCHAR(100) NOT NULL,

    -- Enum'u metin olarak saklıyoruz (ordinal/sayı değil).
    -- Neden? Sayı saklarsak enum sırası değişince eski kayıtlar bozulur:
    -- "2" bugün DONE, yarın sıraya yeni bir değer eklenirse başka şey olur.
    -- Metin okunabilir ve kırılgan değil.
    status      VARCHAR(20)  NOT NULL,

    -- Auditable'dan gelen zaman damgaları
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

-- Durum bazlı sorgular için indeks.
-- Faz 3'te worker "PENDING işleri getir" diye soracak; o sorgu bu indeksi kullanır.
CREATE INDEX idx_jobs_status ON jobs (status);
