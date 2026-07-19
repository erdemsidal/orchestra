#!/bin/bash
# LocalStack hazır olduğunda bir kez çalışır (/etc/localstack/init/ready.d).
# İş kuyruğunu oluşturur. (DLQ ve retry politikasını Faz 3 Adım 5'te ekleyeceğiz.)
awslocal sqs create-queue --queue-name jobs-queue
echo "SQS kuyruğu oluşturuldu: jobs-queue"
