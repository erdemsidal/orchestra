# ADR 0004: Ne cache'lenir, ne asla cache'lenmez

- **Durum:** Kabul edildi
- **Tarih:** 2026-07-19
- **Faz:** 2 — Ölçeklenebilirlik

## Bağlam

`GET /jobs/{id}` yük altında (50 eşzamanlı kullanıcı) p95 ~19.5ms sürüyordu. Tek
satırlık bir primary-key okuması olmasına rağmen, Postgres bağlantı havuzu
(Hikari, 20 bağlantı) yük altında darboğaz oluyor. Cache ile bu okumayı DB'ye
gitmeden karşılamak istedik.

İki gerçek karar var: (1) cache kodu mimaride nereye girer, (2) neyi cache'lemek
güvenli.

## Seçenekler

**Nereye:**
- A — `GetJobService`'e `@Cacheable`. Ama application katmanını bilerek
  Spring'siz tuttuk (ADR 0003 ruhu); Spring anotasyonu buraya sızmamalı.
- B — Infrastructure'da, `JobController.getById` üzerinde. Cache'lenen şey API
  cevabı (`JobResponse`, bir record — Redis'e temiz serialize olur).

**Neyi:**
- Her işi mi, yoksa yalnızca değişmez olanları mı?

## Karar

**Cache infrastructure'da, `JobController.getById` üzerinde** (`@Cacheable("jobs",
key="#id")`). TTL 5 dakika. `JobResponse` bir record olduğu için Redis'e sorunsuz
serialize oluyor; domain nesnesi `Job`'ı cache'leseydik (setter'sız, çift
constructor'lı) serialization sorun çıkarırdı.

**Cache invalidation (`@CacheEvict`) EKLEMİYORUZ** — ve bu bilinçli. Faz 1'de POST
senkron çalışıyor: kullanıcı bir işi GET'lediğinde iş zaten terminal (DONE/FAILED).
Terminal iş asla değişmez, yani cache'lediğimiz veri **değişmez**. Değişmez veriyi
cache'lemek bayat-veri riski taşımaz. TTL sadece belleği sınırlayan emniyet kemeri.

## Sonuç / Feda ettiğim

- Kazandığım (ölçülü): p95 19.5ms → 6.6ms (≈3x), throughput 3.9K → 11.2K req/s
  (≈2.9x). Aynı test, tek fark `@Cacheable`.
- Feda ettiğim: cache'in controller'da olması "thin controller"ı bir miktar
  esnetir. Kabul edilebilir, çünkü cache'lenen şey HTTP cevabının ta kendisi.
- **Ne zaman değişir (kritik):** Faz 3'te iş async olacak; kullanıcı işi
  PENDING/RUNNING (yani DEĞİŞKEN) haldeyken GET'leyecek. O an "değişmez veri"
  varsayımı çöker. Bu ADR o noktada güncellenecek. ↓

## Güncelleme (Faz 3, 2026-07-20): öngörü gerçekleşti

İş async olunca beklenen bayatlama yaşandı: bir işi RUNNING'ken GET'leyince
RUNNING cache'lendi; worker DONE yaptıktan sonra API TTL boyunca hâlâ RUNNING
gösterdi (Redis'te bayat kayıt). Bunu canlı gözlemledik.

**Çözüm — @CacheEvict DEĞİL, "sadece terminali cache'le":** `@Cacheable`'a
`unless = "!#result.status.terminal"` ekledik. Artık yalnızca DONE/FAILED
(değişmez) cache'leniyor; PENDING/RUNNING her seferinde DB'den taze okunuyor.
Böylece bayatlama imkânsız ve hâlâ cache invalidation'a (evict/put) gerek yok —
çünkü cache'e giren her şey tanımı gereği değişmez. `JobStatus.isTerminal()`
eklendi. Ödenen bedel: değişken durumlar cache'lenmediği için sürekli PENDING
sorgulanan işlerde cache faydası yok — ama zaten değişen bir şeyi cache'lemek
yanlış olurdu.
