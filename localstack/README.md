# LocalStack / SQS — nasıl izlerim?

Lokal SQS kuyruğunu (`jobs-queue`) görüntülemenin yolları. Hepsi ücretsiz, hesap
gerektirmez. LocalStack `docker compose up -d` ile ayakta olmalı.

Kuyruk URL'i (kısaltma):

```bash
Q="http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/jobs-queue"
```

## 1) Canlı izleyici (önerilen)

```bash
bash localstack/watch-queue.sh
```

Her 2 saniyede "bekliyor" (alınmayı bekleyen) ve "işleniyor" (bir worker'ın alıp
henüz silmediği, gizli) mesaj sayısını gösterir. `POST /jobs` attıkça sayının
arttığını, worker çalışınca düştüğünü canlı görürsün. Ctrl+C ile çık.

## 2) Tek seferlik komutlar

```bash
# Kuyruklar
docker exec orchestra-localstack awslocal sqs list-queues

# Kaç mesaj var (görünür + gizli)?
docker exec orchestra-localstack awslocal sqs get-queue-attributes \
  --queue-url "$Q" --attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible

# Bir mesajı oku (silmeden — visibility timeout süresince gizlenir)
docker exec orchestra-localstack awslocal sqs receive-message --queue-url "$Q"

# Elle mesaj at (test)
docker exec orchestra-localstack awslocal sqs send-message --queue-url "$Q" --message-body '{"jobId":"test"}'

# Kuyruğu boşalt
docker exec orchestra-localstack awslocal sqs purge-queue --queue-url "$Q"
```

## 3) Docker Desktop

Containers → `orchestra-localstack` → konteynerin durumu ve logları (init
script'in "kuyruk oluşturuldu" çıktısı burada görünür).

## Not: app.localstack.cloud web paneli

Community sürümde, auth token olmadan başlatılan instance genelde bu web
panelinde görünmez. Uğraşmaya değmez; yukarıdaki yollar her zaman çalışır.
