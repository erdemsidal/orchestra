package com.orchestra.job.infrastructure.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * POST /api/jobs için IP başına rate limiting (token bucket, Bucket4j).
 *
 * FILTER nedir? Controller'a ULAŞMADAN önce her isteği yakalayan bir kapı.
 * Rate limit için ideal: limiti aşan istek, iş mantığına hiç dokunmadan burada
 * 429 ile geri çevrilir (boşuna DB/execute maliyeti ödemeyiz).
 *
 * OncePerRequestFilter: Spring'in filter tabanı; her istek için tam bir kez çalışır.
 *
 * NOT (ADR 0005): Limit uygulama katmanında ve IP bazında. IP kusurlu (NAT
 * arkasındaki herkes tek sayılır) ama auth olmadığı için en pratik anahtar bu.
 * Bucket'lar in-memory (ConcurrentHashMap) — tek instance için doğru; birden
 * çok instance olursa her biri kendi limitini tutar, o zaman bucket4j-redis gerekir.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // IP -> o IP'nin jeton kovası. Farklı IP'ler birbirini etkilemez.
    private final Map<String, Bucket> bucketsByIp = new ConcurrentHashMap<>();

    private final long capacity;
    private final long refillTokens;
    private final Duration refillPeriod;

    public RateLimitFilter(
            @Value("${app.rate-limit.capacity}") long capacity,
            @Value("${app.rate-limit.refill-tokens}") long refillTokens,
            @Value("${app.rate-limit.refill-period-seconds}") long refillPeriodSeconds) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriod = Duration.ofSeconds(refillPeriodSeconds);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Sadece iş GÖNDERİMİNİ sınırlıyoruz (POST /api/jobs). GET ve diğerleri serbest.
        if (!isJobSubmission(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);
        // Bu IP'ye ait kovayı al; yoksa yeni bir kova oluştur.
        Bucket bucket = bucketsByIp.computeIfAbsent(ip, key -> newBucket());

        // 1 jeton tüketmeyi dene ve kalan durumu öğren.
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Jeton vardı: geç. Kalan jetonu header'da bildirmek nazik bir davranış.
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            // Jeton yok: 429 döndür ve ne zaman tekrar denenebileceğini söyle.
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            writeTooManyRequests(response, request.getRequestURI(), waitSeconds);
        }
    }

    /** Bu IP için token bucket kur: kapasite + sabit hızlı (greedy) dolum. */
    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, refillPeriod)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private boolean isJobSubmission(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/api/jobs".equals(request.getRequestURI());
    }

    /** Gerçek istemci IP'si. Proxy arkasındaysak X-Forwarded-For ilk değeri. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, String path, long retryAfterSeconds)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"status":429,"error":"Too Many Requests",\
                "message":"Rate limit aşıldı. %d saniye sonra tekrar deneyin.",\
                "path":"%s"}""".formatted(retryAfterSeconds, path));
    }
}
