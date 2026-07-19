// ═══════════════════════════════════════════════════════════
// GET /api/jobs/{id} yük testi
//
// Amaç: tek bir işi yüksek eşzamanlılıkla sorgulayıp GET endpoint'inin
// yük altında ne kadar sürdüğünü ölçmek (p50/p95/p99).
//
// Bu dosya k6 ile çalışır (Java değil, JavaScript). k6 kendi JS motorunu
// kullanır — Node.js gerekmez.
// ═══════════════════════════════════════════════════════════

import http from 'k6/http';
import { check } from 'k6';

// ── Test ayarları ────────────────────────────────────────────
// vus (Virtual Users): aynı anda istek atan sanal kullanıcı sayısı.
// duration: testin ne kadar süreceği. 50 kişi 30 saniye boyunca durmadan
// GET atacak.
export const options = {
  vus: 50,
  duration: '30s',
  // Özet raporda hangi istatistikleri görmek istediğimiz. Sana öğrettiğim
  // p50/p95/p99 üçlüsünü buraya ekliyoruz (k6 varsayılanı p90/p95).
  summaryTrendStats: ['avg', 'min', 'med', 'p(95)', 'p(99)', 'max'],
};

const BASE = 'http://localhost:8080';

// ── setup(): test BAŞLAMADAN bir kez çalışır ─────────────────
// Sorgulayacağımız bir iş lazım; onu burada bir kez oluşturup id'sini
// bütün VU'lara dağıtıyoruz. (İşin durumu DONE/FAILED olabilir, önemi yok;
// biz sadece var olan bir kaydı GET'leyeceğiz.)
export function setup() {
  const res = http.post(
    `${BASE}/api/jobs`,
    JSON.stringify({ type: 'load-test' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  return { jobId: res.json('id') };
}

// ── default(): her VU bunu tekrar tekrar çalıştırır ──────────
// data, setup()'ın döndürdüğü nesne. Her VU bu işi GET'liyor.
export default function (data) {
  const res = http.get(`${BASE}/api/jobs/${data.jobId}`);

  // check: bir doğrulama. Başarısızsa test durmaz ama sonda "başarı oranı"
  // olarak raporlanır. 200 gelmiyorsa bir sorun var demektir.
  check(res, {
    'status 200': (r) => r.status === 200,
  });
}
