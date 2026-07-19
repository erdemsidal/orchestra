# ADR 0005: Rate limiting — algoritma, konum ve anahtar

- **Durum:** Kabul edildi
- **Tarih:** 2026-07-20
- **Faz:** 2 — Ölçeklenebilirlik

## Bağlam

`POST /api/jobs` her çağrıda senkron iş çalıştırıyor (DB + işlem). Bir istemci
(kötü niyetli ya da hatalı) saniyede binlerce iş göndererek sistemi boğabilir.
Gönderim hızını sınırlamamız gerekiyor.

Üç alt karar: hangi algoritma, limit nerede uygulanır, kim (hangi anahtar)
sınırlanır.

## Kararlar

**1. Algoritma: token bucket (Bucket4j).**
- Fixed window pencere sınırında çift patlamaya izin verir; sliding window daha
  pürüzsüz ama her isteğin zaman damgasını tutmayı gerektirir (bellek/hesap).
- Token bucket O(1) bellek (istemci başına tek sayaç), burst'e doğal olarak
  müsamaha eder (kova kapasitesi kadar) ama sürekliliği dolum hızında tutar.
  Bizim ihtiyacımıza (kısa patlama tamam, sürekli sel hayır) tam oturuyor.
- Ayar: kapasite 20, saniyede 20 dolum → IP başına ~20 istek/saniye, 20'lik burst.

**2. Konum: uygulama katmanı (bir Spring filter'ı).**
- Filter, isteği controller'a ulaşmadan yakalar; limiti aşan istek iş mantığına
  hiç dokunmadan 429 alır (boşuna DB/execute yok).
- İdeali bir API gateway olurdu (limit uygulamaya hiç gelmeden kesilir), ama o
  ekstra altyapı demek. Vitrin ölçeğinde uygulama katmanı yeterli.

**3. Anahtar: istemci IP'si.**
- Auth olmadığı için kullanıcıyı IP ile ayırıyoruz.

## Sonuç / Feda ettiğim

- Kazandığım: sistem, tek bir istemcinin gönderim seline karşı korunuyor.
  Ölçtüm: 100 eşzamanlı POST → 61 geçti, 39'u 429 (kova + dolum davranışı).
- Feda ettiğim / kusurlar:
  - **IP kusurlu:** NAT/proxy arkasındaki farklı kullanıcılar tek IP olarak
    görünür, aynı limiti paylaşır. Auth gelince anahtarı kullanıcı kimliğine
    çevirmek gerekir.
  - **In-memory, tek instance:** Bucket'lar bir `ConcurrentHashMap`'te. Uygulama
    yatayda ölçeklenip birden çok instance olursa her instance kendi limitini
    tutar; gerçek global limit için `bucket4j-redis` (dağıtık bucket) gerekir.
- **Ne zaman değişir:** yatay ölçekleme (→ bucket4j-redis) veya bir gateway
  eklenince (→ limiti oraya taşı) ya da auth gelince (→ anahtar = kullanıcı).
