# ADR 0002: API'yi CRUD değil, use-case bazlı tasarlamak

- **Durum:** Kabul edildi
- **Tarih:** 2026-07-17
- **Faz:** 1 — Temiz Mimari

## Bağlam

Elimde bir `jobs` tablosu var. Alışılmış refleks, tabloya dört CRUD işlemi
açmaktır: create, read, update, delete. Yani `POST`, `GET`, `PUT`, `DELETE`.

Ama asıl soru "tabloda hangi işlemler var" değil, **"kullanıcı bu sistemde ne
yapabilir"**. Kullanıcı iki şey yapıyor: bir iş gönderiyor ve durumunu soruyor.
Başka bir şey yapmıyor — özellikle işin durumunu **kendisi belirleyemiyor**.

## Seçenekler

**A — Use-case bazlı API.** Sadece kullanıcının gerçekten yapabildiği eylemler
endpoint olur: `POST /jobs` (oluştur + çalıştır), `GET /jobs/{id}` (durumu sor).

**B — Tam CRUD.** `POST` / `GET` / `PUT /jobs/{id}` / `DELETE /jobs/{id}`.

## Karar

**Seçenek A.** İki endpoint: oluştur ve sorgula. `PUT` ve `DELETE` bilerek yok.

`PUT /jobs/{id}` **tehlikeli olurdu**, sadece gereksiz değil. Bir istemci şunu
yapabilirdi:

```
PUT /api/jobs/abc-123   { "status": "DONE" }
```

Yani PENDING bir işi, hiç çalışmadan DONE yapmak. Bu, domain'e yazdığım bütün
durum geçiş kurallarını (ADR 0001) ve onları koruyan testleri **tek istekte
by-pass ederdi**. İşin durumu dışarıdan set edilen bir alan değil; bir olayın
(işin çalışmasının) **sonucudur**. Durumu yalnızca sistem, domain kurallarıyla
ilerletir (`start` / `markDone` / `markFailed`).

`DELETE` de yok: bir iş bir **kayıttır** ("bu iş geçen salı çalıştı ve başarısız
oldu"). Tarihi silmiyoruz. (İleride gerekirse arşivleme ayrı bir konu olur.)

## Sonuç / Feda ettiğim

- Kazandığım: domain kuralları hiçbir yoldan delinemiyor; API yüzeyi küçük ve
  niyeti net; her endpoint gerçek bir kullanıcı ihtiyacına karşılık geliyor.
- Feda ettiğim: "standart CRUD" tanıdıklığı. Bir işi elle düzeltmek/silmek için
  API yok; şimdilik bu bir eksik değil, bir tercih.
- **Ne zaman değişir:** operasyonel bir ihtiyaç doğarsa (ör. yönetici bir işi
  iptal etmek isterse), bunu `PUT status` olarak değil, yeni bir use-case olarak
  eklerim: `POST /jobs/{id}/cancel` → domain'de kuralı olan bir `cancel()` metodu.
  Yani yine durumu set etmem, yeni bir kurallı geçiş tanımlarım.
