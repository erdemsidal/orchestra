package com.orchestra.job.application;

import com.orchestra.job.domain.Job;
import com.orchestra.job.domain.JobNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GetJobService'in BİRİM testleri — yine DB'siz, sahte repository ile.
 */
class GetJobServiceTest {

    @Test
    @DisplayName("getById() var olan işi döner")
    void getById_isVarsa_donerIsi() {
        // GIVEN — sahte repoya önceden bir iş koy
        FakeJobRepository repo = new FakeJobRepository();
        Job kayitli = new Job(UUID.randomUUID(), "email-gonder");
        repo.save(kayitli);
        GetJobService service = new GetJobService(repo);

        // WHEN — o işi id ile sorgula
        Job bulunan = service.getById(kayitli.getId());

        // THEN — aynı iş dönmeli
        assertThat(bulunan.getId()).isEqualTo(kayitli.getId());
        assertThat(bulunan.getType()).isEqualTo("email-gonder");
    }

    @Test
    @DisplayName("getById() iş yoksa JobNotFoundException fırlatır")
    void getById_isYoksa_istisnaFirlatir() {
        // GIVEN — boş repo
        GetJobService service = new GetJobService(new FakeJobRepository());

        // WHEN + THEN — olmayan bir id sorgulanırsa istisna fırlamalı
        assertThatThrownBy(() -> service.getById(UUID.randomUUID()))
                .isInstanceOf(JobNotFoundException.class);
    }
}
