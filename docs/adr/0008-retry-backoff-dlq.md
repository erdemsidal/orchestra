# ADR 0008: Retry, backoff ve dead-letter queue

- **Durum:** Kabul edildi
- **Tarih:** 2026-07-21
- **Faz:** 3 — Dağıtık / Event-Driven

## Bağlam

İşler başarısız olabilir. İki tür hata var:
- **Geçici (transient):** ağ, geçici kilit, downstream servis hıçkırığı → tekrar
  denemeye değer.
- **Kalıcı (permanent):** geçersiz veri, mantık hatası → tekrar denemek boşuna.

Ayrıca sürekli patlayan bir "zehirli" mesaj sonsuza dek denenirse kuyruğu tıkar.
Retry, backoff ve bir kaçış valfi (DLQ) gerekiyor.

## Kararlar

**1. Geçici / kalıcı ayrımı kodda.**
`TransientJobException` → ExecuteJobService bunu YAKALAMAZ, yukarı fırlatır.
Diğer `RuntimeException` (kalıcı) → yakalanır, iş FAILED yapılır. Hangi hatanın
geçici olduğuna işi çalıştıran (runner) karar verir.

**2. Retry mekanizması = SQS'in kendisi.**
Worker geçici hatada istisna fırlatınca mesaj silinmez; SQS visibility timeout
sonrası tekrar teslim eder. Ekstra retry kodu YOK — at-least-once teslimatın
doğal sonucu. İş RUNNING kalır, sonraki teslimatta start() atlanıp doğrudan
yeniden çalıştırılır (bkz. ADR 0007 idempotency ile uyumlu).

**3. Backoff = visibility timeout (sabit, 5 sn).**
Başarısız mesaj 5 sn sonra tekrar görünür; retry aralığımız bu.

**4. DLQ = SQS redrive policy.**
`jobs-queue` üzerinde `maxReceiveCount=3`: bir mesaj 3 kez teslim edilip hâlâ
başarısızsa SQS onu `jobs-dlq`'ya taşır. Bir DLQ tüketicisi (@SqsListener)
işi kalıcı olarak FAILED işaretler (döngüyü kapatır).

## Sonuç / Feda ettiğim

- Kazandığım: geçici hatalar otomatik retry'lanıyor (çoğu ikinci denemede
  düzelir), kalıcı hatalar boşuna denenmiyor, zehirli mesajlar 3 denemede DLQ'ya
  taşınıp sistemi tıkamıyor. Neredeyse tamamı SQS'in built-in özellikleriyle,
  minimum kodla. Canlı doğrulandı (zehir: 3 deneme → DLQ → FAILED).
- **Dürüst sınır (feda ettiğim):** backoff **sabit** (5 sn), gerçek **exponential**
  backoff (1s, 2s, 4s...) değil. SQS standart kuyrukta bunun için her denemede
  `ChangeMessageVisibility` ile visibility'yi artırmak gerekir — şimdilik eklemedik.
- **maxReceiveCount neden 3?** Denge: çok az (1-2) → geçici hatalar kurtarılamadan
  DLQ'ya düşer; çok fazla → zehirli mesaj uzun süre sistemi meşgul eder. 3 makul
  bir başlangıç; ölçüp ayarlanır.
- **Ne zaman değişir:** gerçek exponential backoff gerekirse ChangeMessageVisibility;
  DLQ'daki işler için otomatik yeniden işleme (redrive) veya alarm eklenebilir.
