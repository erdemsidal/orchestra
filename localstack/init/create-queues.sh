#!/bin/bash
# LocalStack hazır olduğunda bir kez çalışır (/etc/localstack/init/ready.d).
# İş kuyruğunu + dead-letter kuyruğunu (DLQ) oluşturur.

# 1) Önce DLQ — başarısız mesajların son durağı.
awslocal sqs create-queue --queue-name jobs-dlq

# 2) Ana kuyruk. Redrive policy: bir mesaj 3 kez teslim edilip hâlâ işlenemezse
#    jobs-dlq'ya taşınır (sonsuza dek denenmesin). VisibilityTimeout=5: başarısız
#    bir mesaj 5 sn sonra tekrar görünür (retry aralığı = bizim backoff'umuz).
awslocal sqs create-queue --queue-name jobs-queue --attributes '{
  "VisibilityTimeout": "5",
  "RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:000000000000:jobs-dlq\",\"maxReceiveCount\":\"3\"}"
}'

echo "SQS kuyrukları oluşturuldu: jobs-queue (3 denemeden sonra -> jobs-dlq)"
