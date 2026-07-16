# ADR 0001: İş durum geçişlerini nerede yöneteceğiz?

- **Durum:** Kabul edildi
- **Tarih:** 2026-07-16
- **Faz:** 1 — Temiz Mimari

## Bağlam

Bir iş (`Job`) dört durumdan geçer:

```
PENDING ──► RUNNING ──► DONE
                 └────► FAILED
```

Her geçiş serbest değil. Geçersiz olanlar (örn. `PENDING → DONE`: çalışmadan
bitemez; `DONE → RUNNING`: biten iş yeniden başlamaz) **kodun kendisi
tarafından engellenmeli**. Aksi halde bir çağıran yanlışlıkla tutarsız bir
duruma sokabilir. Soru şu: bu geçiş kurallarını **nerede** tutalım?

## Seçenekler

**A — Kural, `Job` nesnesinin kendi metotlarında.**
`job.start()`, `job.markDone()`, `job.markFailed()` gibi metotlar geçişi yapar
ve geçersizse `IllegalStateException` fırlatır.
- (+) Basit, az dosya. Kural, ait olduğu nesnenin içinde ("tell, don't ask").
- (+) Nesne kendini tutarsız duruma sokulmaktan korur (encapsulation).
- (−) Durum sayısı çok artarsa `Job` şişebilir.

**B — Ayrı bir `JobStateMachine` sınıfı.**
Tüm izinli geçişler tek bir yerde (ör. bir `Map<JobStatus, Set<JobStatus>>`)
toplanır.
- (+) Geçiş "haritası" tek bakışta görülür.
- (+) Çok sayıda durum/karmaşık akışta daha düzenli.
- (−) 4 durumluk bir akış için fazla mühendislik (over-engineering).
- (−) `Job` ile makine ayrı düşer, iki sınıf birbirini konuşmak zorunda.

## Karar

**Seçenek A.** Geçiş kurallarını `Job` domain nesnesinin metotlarında tutuyoruz.

Gerekçe: Bugünkü ihtiyaç 4 durum ve neredeyse doğrusal bir akış. En basit çözüm
işi görüyor (YAGNI). Yanılırsak A'dan B'ye geçiş ucuz: kuralları çekip ayrı bir
sınıfa taşımak küçük bir refactor. Değiştirmesi ucuz olan yerde en basitten
başlamak doğru olan.

## Sonuç / Feda ettiğim

- Kazandığım: az kod, yüksek okunabilirlik, nesne kendini korur, DB'siz test
  edilebilir saf domain.
- Feda ettiğim: geçiş kurallarının tek bir "harita"da toplu görünürlüğü.
- **Ne zaman B'ye geçmeliyim:** durum sayısı belirgin şekilde artarsa
  (ör. `RETRYING`, `CANCELLED`, `SCHEDULED` gibi yeni durumlar eklenir ve
  geçişler bir grafiğe dönüşürse), ya da geçiş mantığı `Job` dışında birden
  fazla yerde tekrar etmeye başlarsa.
