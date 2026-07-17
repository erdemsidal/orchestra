# ADR 0003: Paket yapısı — feature bazlı + düz katmanlar

- **Durum:** Kabul edildi
- **Tarih:** 2026-07-17
- **Faz:** 1 — Temiz Mimari

## Bağlam

Kodu nasıl paketleyeceğime karar vermem gerekti. İki eksen var:

1. **Neye göre bölünecek?** Teknik katmana göre mi (`controller/`, `service/`,
   `repository/`), yoksa iş özelliğine göre mi (`job/`)?
2. **Ne kadar alt klasör?** Her rol kendi klasöründe mi (`port/`, `dto/`,
   `config/`), yoksa dosyalar katmanın içinde düz mü dursun?

## Seçenekler

**A — Package-by-layer.** `controller/`, `service/`, `entity/`, `repository/`.
Her Spring tutorial'ının öğrettiği düzen.

**B — Package-by-feature + her rol kendi alt klasöründe.** `job/` altında
`domain/`, `application/port/`, `infrastructure/web/dto/` gibi derin klasörler.

**C — Package-by-feature + düz katmanlar.** `job/` altında sadece `domain/`,
`application/`, `infrastructure/` (+ infrastructure içinde `persistence/`, `web/`).
Rol klasörleri (`port/`, `dto/`, `config/`) yok; dosyalar katmanın içinde düz durur.

## Karar

**Seçenek C.**

Package-by-layer'ı (A) elememin sebebi: bir feature'a alan eklemek beş ayrı
klasörde dolaşmayı gerektirir, ve en önemlisi `entity/Job` hem JPA hem iş kuralı
taşıyarak domain'i Spring'e yapıştırır — o zaman domain'i DB'siz test edemem.
Bu projenin bütün amacı olan katman ayrımı çöker.

B'yi (derin alt klasörler) elememin sebebi: 10 dosya için 7 klasör oldu ve
tutarsızdı (`port/` klasördü ama servisler açıktaydı). Alt klasör eklemek
karmaşıklığı azaltmıyor, sadece taşıyor; tek dosyalık bir `exception/` klasörü de
bir tür gürültü.

C ile üç katman ayrımı korunuyor (domain / application / infrastructure), ama her
katmanda dosyalar düz duruyor. Her katmanda 3-4 dosya var, bir bakışta görünüyor.

Bir sınıfın "port" ya da "dto" olması bir **klasör** meselesi değil, bir **rol**
meselesi: `JobRepository` bir arayüz olduğu için hâlâ port, ayrı bir klasörde
durmasına gerek yok.

## Sonuç / Feda ettiğim

- Kazandığım: az klasör, düşük gürültü, tutarlı yapı; feature'ın tamamı tek yerde
  (`job/`); domain izole kaldığı için DB'siz test edilebiliyor.
- Feda ettiğim: rollerin (port/adapter/dto) klasör isminden okunabilirliği. Artık
  bunu dosya adından ve konumdan anlıyorum (`JobRepository` = port, `JobEntity` =
  persistence detayı).
- **Ne zaman değişir:** `job/` içindeki bir katman çok büyürse (ör. `web/` altında
  onlarca DTO birikirse) o katmana özel bir alt klasör geri gelebilir. Karar dosya
  sayısına göre, baştan simetri kaygısıyla değil.
