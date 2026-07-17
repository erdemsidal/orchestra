package com.orchestra.job.application;

import com.orchestra.job.domain.Job;

/**
 * PORT — "işi gerçekten çalıştıran" şey.
 *
 * "İş yapmak" bir dış dünya eylemidir (gerçekte e-posta atmak, rapor üretmek,
 * bir API çağırmak...). O yüzden application bunu doğrudan yapmaz, bir port'un
 * arkasına koyar:
 *   - Çalışma anında: gerçek/simüle edilmiş runner (infrastructure'da).
 *   - Test anında: sahte runner (istediğimiz gibi başarılı ya da patlayan).
 *
 * Tek metotlu bir arayüz (functional interface) — bu yüzden testte lambda ile
 * bile yazabiliriz: `job -> { }` başarı, `job -> { throw ... }` başarısızlık.
 */
public interface JobTaskRunner {

    /**
     * İşi çalıştırır. Başarılıysa sessizce döner; başarısızsa bir RuntimeException
     * fırlatır. Fırlayan istisnayı ExecuteJobService yakalayıp işi FAILED yapar.
     */
    void run(Job job);
}
