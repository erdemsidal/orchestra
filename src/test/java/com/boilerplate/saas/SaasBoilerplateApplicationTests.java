package com.boilerplate.saas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class SaasBoilerplateApplicationTests {

    @Test
    void contextLoads() {
        // Uygulama context'i başarıyla ayağa kalkıyor mu kontrol et.
        // Bu test Postgres + Redis bağlantısı gerektirir.
        // docker compose up -d çalıştırdıktan sonra mvn test yap.
    }
}
