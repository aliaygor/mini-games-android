# Mini Games: 30 Seconds

Genç ve yetişkin oyuncular için 15 oyunlu Kotlin/Jetpack Compose beceri ve bulmaca platformu. Koyu lacivert tasarım, neon vurgular, oyun başına ustalık seviyesi, detaylı skorlar, günlük mücadele ve çevrimdışı profil.

## Çalıştırma

Android Studio'da repository kökünü açın; SDK 36 ve JDK 17/21 kullanın. Gradle sync sonrası `app` debug varyantını standart Android App run configuration ile çalıştırın. Bytecode hedefi JVM 17'dir. Firebase olmadan misafir ve yerel profil ile oyunlar açılır.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio1\jbr'
.\gradlew.bat :app:assembleDebug :core:domain:test :core:data:testDebugUnitTest :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Oyunlar

Tile Blast, Traffic Escape, Spot the Difference, Path Finder, Color Rush, Reaction, Quick Math, Memory Grid, Precision Tap, Sequence Master, Word Scramble, Number Merge, Pattern Master, Rapid Sudoku, Logic Grid.

Her katalog girdisi gerçek input, skor, süre, duraklatma/devam, sonuç ve kalıcı oyun seviyesine bağlıdır. Eski kapı/civciv, eşli hafıza kartı ve blok dizme mekanikleri yeni katalogda yoktur. İlk oyundaki kampanya kapsamı ile üretim kalitesi aynı şey değildir: örneğin Fark Bul şu anda tek gerçek görsel çiftini içerir; tam içerik kütüphanesi tamamlanmamıştır.

## Mimari

- `core/domain`: Android bağımsız seeded oyun kuralları, puzzle çözücüleri, Score/Difficulty, progression, wallet ledger, cloud merge, günlük challenge ve replay doğrulaması.
- `core/data`: DataStore profil/sonuç kaydı, v1→v2 migration ve JSON round-trip testleri.
- `core/platform`: Credential Manager + Firebase Auth, Firestore cloud restore/sync ve paginated leaderboard adaptörü, AdMob/UMP, telemetri, özgün kısa sesler.
- `app`: Hilt, MVVM, Compose UI ve lifecycle/timer/input kontrolü.
- `backend`: ortak domain motorunu kullanan Firebase Admin JVM skor doğrulama servisi.

[Mimari ve kapsam](docs/PRODUCT_ARCHITECTURE.md) · [Doğrulama](docs/VERIFICATION.md) · [Firebase adımları ve SHA değerleri](docs/FIREBASE_SETUP.md) · [Sunucu kurulum sözleşmesi](backend/README.md).

## Gerçek servis yapılandırması

Google sağlayıcısını, Anonymous Auth'u ve Firestore'u kendi Firebase projenizde etkinleştirin. Güncel `app/google-services.json` Web OAuth client bilgisi içermelidir. Google butonu bu bilgi yokken kapalıdır. Sunucuyu kendi Cloud Run ortamınıza kurup gerçek HTTPS adresini Gradle properties içinde `COMPETITION_URL` olarak verin. Uygulama bu değer olmadan uydurma rank/oyuncu göstermez; turları yerel kaydeder.

Debug yalnız Google test AdMob ID'lerini kullanır. Üretim `ADMOB_APP_ID`, `ADMOB_REWARDED_ID`, `ADMOB_INTERSTITIAL_ID` değerlerini kullanıcı Gradle properties içine ekleyin. Rewarded hint yalnız SDK callback sonrası verilir; interstitial her tur sonunda gösterilmez. Gameplay içinde banner yoktur.

## Yayın durumu

Bu refactor tamamlanmış Play ürünü olarak sunulmaz. Canlı Google giriş/iki cihaz cloud restore testi, sunucu/rules deployment, anti-abuse/App Check, içerik çeşitliliği ve denge, tablet/API 24/büyük font/erişilebilirlik, fiziksel cihaz frame-time ölçümü, release signing ve mağaza gereklilikleri hâlâ yayın kapılarıdır. Firebase hesapları ve production ID'leri uydurulmamıştır. Aktif tur process death sonrası iptal edilir; kaydedilen sonuçlar korunur. Ranked turu duraklatmak yerel antrenmana çevirir; bu, zaman baskısını durdurup dünya skoruna avantaj taşımayı önler.
