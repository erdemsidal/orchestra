package com.orchestra.job.infrastructure;

import com.orchestra.job.application.CreateJobService;
import com.orchestra.job.application.ExecuteJobService;
import com.orchestra.job.application.GetJobService;
import com.orchestra.job.application.JobRepository;
import com.orchestra.job.application.JobTaskRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Use case'leri Spring'e bean olarak tanıtır.
 *
 * NEDEN böyle, neden application'a @Service koymadık?
 *   Çünkü application katmanının Spring'i bilmesini istemiyoruz. @Service koysaydık
 *   o sınıflar org.springframework.* import etmek zorunda kalırdı.
 *
 * Bedeli: bu dosya (fazladan bir sınıf).
 * Ödülü:  - application %100 framework'süz kalır, saf Java olarak test edilir.
 *         - "Hangi parça neye bağlanıyor?" sorusunun cevabı TEK ve AÇIK bir yerde.
 *           @Service/@Autowired dağınıklığında bu bilgi kodun içine yayılır;
 *           burada tek bakışta görüyorsun.
 *
 * Metot parametresindeki JobRepository'yi Spring nereden buluyor?
 *   JpaJobRepositoryAdapter @Repository ile işaretli ve JobRepository'yi
 *   implement ediyor -> Spring onu bulup buraya enjekte ediyor.
 *   İşte "priz"e santralin takıldığı an tam olarak burası.
 */
@Configuration
public class JobBeanConfig {

    @Bean
    public CreateJobService createJobService(JobRepository jobRepository) {
        return new CreateJobService(jobRepository);
    }

    @Bean
    public GetJobService getJobService(JobRepository jobRepository) {
        return new GetJobService(jobRepository);
    }

    // JobTaskRunner'ı Spring, SimulatedJobTaskRunner'dan (@Component) buluyor —
    // tıpkı JobRepository'yi JpaJobRepositoryAdapter'dan bulduğu gibi.
    @Bean
    public ExecuteJobService executeJobService(JobRepository jobRepository,
                                               JobTaskRunner jobTaskRunner) {
        return new ExecuteJobService(jobRepository, jobTaskRunner);
    }
}
