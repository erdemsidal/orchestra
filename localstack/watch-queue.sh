#!/bin/bash
# jobs-queue'yu CANLI izler: her 2 saniyede mesaj sayılarını günceller.
# Kullanım: bash localstack/watch-queue.sh   (durdurmak için Ctrl+C)
#
# "bekliyor"  = kuyrukta alınmayı bekleyen mesaj (ApproximateNumberOfMessages)
# "işleniyor" = bir worker'ın aldığı ama henüz silmediği/işi bitmediği, gizli
#               mesaj (ApproximateNumberOfMessagesNotVisible — visibility timeout)
Q="http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/jobs-queue"

echo "jobs-queue izleniyor (Ctrl+C ile çık)..."
while true; do
  attrs=$(docker exec orchestra-localstack awslocal sqs get-queue-attributes \
            --queue-url "$Q" \
            --attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible 2>/dev/null)
  visible=$(echo "$attrs" | grep -o '"ApproximateNumberOfMessages": *"[0-9]*"' | grep -o '[0-9]*')
  hidden=$(echo "$attrs"  | grep -o '"ApproximateNumberOfMessagesNotVisible": *"[0-9]*"' | grep -o '[0-9]*')
  printf "\r[%s]  bekliyor: %-4s  işleniyor: %-4s" "$(date +%H:%M:%S)" "${visible:-?}" "${hidden:-?}"
  sleep 2
done
