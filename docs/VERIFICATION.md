# Doğrulama — 5 Ekim 2026

## Arcade arayüz yenilemesi

- Taçlı logo, özgün oyun illüstrasyonları, parlak kartlar ve oyun HUD tasarımı uygulandı. Ana menü, hazırlık, altı oyun, günlük ödül, görevler, mağaza, profil ve sonuç ekranları yenilendi.
- JDK 21 ile JVM 17 hedeflenerek son `:app:assembleDebug` ve `:app:lintDebug` çalıştırmaları geçti; lint hata vermedi. Bağımlılık güncelleme, kullanılmayan kaynak ve launcher monochrome uyarıları bulunuyor.
- 3 Compose instrumentation testi mevcut emülatörde geçti: dil/menü akışı, recreation/duraklatma/devam/sonuç kaydı ve altı oyunun HUD/duraklatma ekranı. Sonrasında rozetlerin genişliği ve atlas kenar kırpımı görsel kontrolde düzeltildi; son APK yeniden derlenip yüklendi.
- Türkçe gerçek emülatör görüntüleri `artwork/arcade/preview-home.png` ve `preview-color.png`; ImageGen istemleri ve kaynak görseller aynı klasörde bulunuyor.
- Aşağıdaki release doğrulaması arayüz yenilemesinden önceki sürüme aittir. Yeni UI için tablet/API 24, büyük font, erişilebilirlik ve performans ölçümleri henüz yapılmadı.

## Önceki altyapı doğrulaması

- JDK 17, Gradle 8.13, AGP 8.13.2, compile/target SDK 36.
- Debug APK derlendi ve mevcut Pixel 9 Android 17 (API 37) emülatöründe açıldı.
- 11 JVM testi: altı oyunda tur sonu, matematik seçenekleri, erken refleks, kapı önizlemesi, hafıza eşleşmesi/hatası, blok kesişimi, skor sınırları, zorluk/cevap süresi, coin/ödül tekrarları, günlük seri ve görev/mağaza işlemleri. Hepsi geçti.
- 2 Android Compose testi: dil/menü akışı ve Activity recreation; duraklatılmış tur/recreation/devam/sonuç kaydı. Hepsi geçti. Sürekli güncellenen gerçek zamanlı ekranın test idling beklemesini önlemek için testte başlangıç/duraklatma ViewModel üzerinden atomik uygulanır; dialog, yeniden yaratma, devam ve sonuç UI üzerinde doğrulanır.
- Release APK R8 kod küçültme ve kaynak küçültmeyle derlendi; lintVitalRelease geçti. Bu APK imzasızdır ve test reklamları içerir, yayın paketi değildir.
- Debug lint: hata yok. 5 uyarı yalnız daha yeni bağımlılık sürümlerinin mevcut olmasıyla ilgili. Dil paketi bölünmesi ve backup politikası uyarıları düzeltildi.

Raporlar: `core/domain/build/reports/tests/test/`, `app/build/reports/androidTests/connected/debug/`, `app/build/reports/lint-results-debug.html`.

Bu doğrulama reklam doluluğu veya gerçek Firebase hesabını test etmez. Rewarded/interstitial için üretim hesabı ile UMP/ödül callback/no-fill testleri, en düşük desteklenen API 24 ve tablet/fiziksel cihaz testleri, erişilebilirlik ve frame-time ölçümleri yayın öncesinde gerekir. Aktif turun process death sırasında iptal edilmesi bilinçli V1 davranışıdır. Tamamlanan sonuç disk üzerinde kalır.
