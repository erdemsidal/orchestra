# ADR 0006: Kuyruk teknolojisi ve çalışma ortamı

- **Durum:** Kabul edildi
- **Tarih:** 2026-07-20
- **Faz:** 3 — Dağıtık / Event-Driven

## Bağlam

Faz 1-2'de `POST /jobs` işi senkron çalıştırıyordu: HTTP isteği iş bitene kadar
(simülasyonda 200-800ms, gerçekte belki dakikalar) bloke oluyordu. Sistemi
ölçeklenebilir yapmak için işi arka plana almalıyız: API işi bir **kuyruğa**
atsın ve anında dönsün, ayrı bir **worker** kuyruktan alıp çalıştırsın.

İki karar: hangi kuyruk teknolojisi, nerede çalıştıralım.

## Seçenekler

**Teknoloji:**
- **AWS SQS** — yönetilen kuyruk, kurulum yok, at-least-once teslimat. Basit.
- **Kafka** — çok yüksek throughput, event replay (log tabanlı). Operasyonel
  olarak ağır.
- **RabbitMQ** — esnek yönlendirme, kendi broker'ını işletmen gerekir.

**Ortam:**
- Gerçek AWS (hesap + kredi kartı) vs LocalStack (AWS'i lokalde taklit eden
  Docker konteyneri, aynı SDK).

## Karar

**AWS SQS**, **LocalStack** üzerinde.

Gerekçe: Ordering ve event replay'e ihtiyacım yok; işler bağımsız, sırası önemsiz.
SQS'in operasyonel yükü sıfır (yönetilen) ve at-least-once teslimatı bizim için
yeterli — idempotency ile tekrarları kendimiz halledeceğiz. Kafka'nın verdiği
yüksek throughput ve replay bu ölçekte gereksiz karmaşıklık.

LocalStack: ücretsiz, kredi kartısız, gerçek AWS SDK'sı ile çalışıyor. Kod
LocalStack'te de gerçek AWS'te de aynı; sadece endpoint URL'i değişir. Öğrenmek
ve göstermek için ideal.

## Sonuç / Feda ettiğim

- Kazandığım: basit, ucuz, AWS ekosistemiyle uyumlu, öğrenmesi kolay bir kuyruk.
- Feda ettiğim: **event replay** (bir mesajı sonradan geri sarıp yeniden
  işleyememek) ve **çok yüksek throughput** — Kafka bunları verirdi.
- **Ne zaman değişir:** event sourcing / replay ihtiyacı doğarsa ya da throughput
  SQS'in sınırlarını zorlarsa Kafka'ya geçiş düşünülür. Producer/consumer'ı bir
  port arkasına koyarsam (bkz. hexagonal) bu geçiş domain'i etkilemez.
