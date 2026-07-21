# ADR 0007: Idempotency — duplicate mesajları nasıl ele alıyoruz

- **Durum:** Kabul edildi
- **Tarih:** 2026-07-21
- **Faz:** 3 — Dağıtık / Event-Driven

## Bağlam

SQS **at-least-once** teslimat yapar: aynı mesaj bazen iki kez gelir (silme onayı
kaybolursa ya da standart kuyruğun doğası gereği). "Exactly-once" pratikte
imkânsıza yakın — ağ, bir mesajın tam bir kez teslim edildiğini garanti edemez.
Endüstri çözümü: **at-least-once teslimat + idempotent işleme**. Yani worker'ı,
aynı mesaj iki kez gelse de iş bir kez çalışmış gibi olacak şekilde yazmak.

Soru: idempotency anahtarı ne olsun?

## Seçenekler

**A — İşin durumu (status) idempotency anahtarı.**
İş yalnızca bir kez PENDING->RUNNING geçebilir. Duplicate geldiğinde iş artık
PENDING değildir; execute() bunu görüp sessizce atlar (çalıştırmaz, istisna da
fırlatmaz ki mesaj silinsin). Ekstra tablo yok.

**B — "İşlenen mesajlar" tablosu (dedup table).**
İşlemeden önce mesaj ID'sini unique-constraint'li bir tabloya INSERT et. Insert
başarılıysa işle; duplicate-key hatası alırsan zaten işlenmiş, atla. Atomik
(DB unique constraint) ve eşzamanlı duplicate'leri de yakalar.

## Karar

**Seçenek A** — işin durumunu idempotency anahtarı olarak kullanıyoruz.

Gerekçe: İşimizin zaten doğal bir "tek geçiş" güvencesi var (`start()` yalnızca
PENDING'den çalışır, ADR 0001). Bunun üstüne ekstra bir tablo, mesaj ID'si
taşımak ve ayrı bir yazma daha eklemek, bu ölçekte gereksiz karmaşıklık.
execute() artık PENDING olmayan işi nazikçe atlıyor (log'layıp döner), böylece
duplicate mesaj sorunsuz silinir — eskiden istisna fırlayıp mesaj sonsuza dek
geri gelirdi.

## Sonuç / Feda ettiğim

- Kazandığım: duplicate mesajlar (SQS'in yeniden teslim ettiği, **sıralı** gelen
  tekrarlar — baskın durum) güvenle, tablosuz ele alınıyor. İş iki kez çalışmaz.
- **Dürüst sınır (feda ettiğim):** İki teslimat **tam eşzamanlı** iki thread'e
  düşerse (SQS listener eşzamanlı işliyor), ikisi de işi PENDING okuyup ikisi de
  start() çağırabilir — "read-then-write" yarışı, iş iki kez çalışabilir. Seçenek
  A bu köşe durumu kapsamaz.
- **Ne zaman değişir / robust hale getirme:** eşzamanlı çift-çalıştırma gözlenirse
  ya da gerçek ödeme gibi kritik bir bağlamda exactly-once şart olursa:
  (1) optimistic locking (`@Version`) ile ikinci save'i çarptır,
  (2) Seçenek B'deki dedup tablosu (atomik unique constraint), veya
  (3) SQS FIFO kuyruğu (dedup + eşzamanlı teslim yok).
  Şimdilik gereksiz; ölçtüğümüzde/gözlemlediğimizde eklenir.
