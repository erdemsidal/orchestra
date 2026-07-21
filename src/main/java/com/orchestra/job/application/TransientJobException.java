package com.orchestra.job.application;

/**
 * GEÇİCİ (transient) iş hatası — tekrar denemeye değer.
 *
 * Örnek: ağ kesintisi, downstream servis bir an cevap vermedi, DB kilidi.
 * Bu istisna ExecuteJobService tarafından YUKARI FIRLATILIR (yakalanmaz) ki
 * worker mesajı silmesin; SQS visibility timeout sonrası tekrar teslim eder
 * (retry). 3 denemeden sonra hâlâ başarısızsa mesaj DLQ'ya gider (redrive policy).
 *
 * Karşıtı: kalıcı (permanent) hata — sıradan RuntimeException. Onu yakalayıp işi
 * FAILED yapıyoruz, tekrar denemiyoruz (bkz. ADR 0008).
 */
public class TransientJobException extends RuntimeException {

    public TransientJobException(String message) {
        super(message);
    }
}
