package com.orchestra.job.infrastructure.messaging;

import com.orchestra.job.application.JobQueue;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ADAPTER — JobQueue port'unun SQS implementasyonu.
 *
 * "Priz"e takılan santral: application JobQueue der, bu sınıf onu SQS ile doldurur.
 * SqsTemplate spring-cloud-aws'in verdiği, mesaj göndermek için hazır bean'i.
 * Endpoint LocalStack'e (localhost:4566) ayarlı olduğu için mesaj oraya gider;
 * production'da aynı kod gerçek AWS'e gider (sadece endpoint config'i değişir).
 *
 * Mesajın gövdesi: sadece jobId (String). Worker bu id ile işi DB'den okuyacak.
 */
@Component
public class SqsJobQueue implements JobQueue {

    private static final Logger log = LoggerFactory.getLogger(SqsJobQueue.class);

    private final SqsTemplate sqsTemplate;
    private final String queueName;

    public SqsJobQueue(SqsTemplate sqsTemplate,
                       @Value("${app.sqs.queue-name}") String queueName) {
        this.sqsTemplate = sqsTemplate;
        this.queueName = queueName;
    }

    @Override
    public void enqueue(UUID jobId) {
        sqsTemplate.send(queueName, jobId.toString());
        log.info("İş kuyruğa bırakıldı: id={} queue={}", jobId, queueName);
    }
}
