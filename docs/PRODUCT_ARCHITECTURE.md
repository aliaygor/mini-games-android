# Rekabetçi platform mimarisi

## Ürün akışı

Onboarding (Google / Guest / Local nickname) → Ana sayfa / Sıralama / Mücadeleler / Profil → oyun açıklaması → aktif tur → detaylı sonuç → sonraki seviye veya tekrar. Görünüm mağazası Profil içinden açılır. Ana sayfa nickname, player level/XP, coin, gerçek rank varsa rank, toplam skor, seri, ortak UTC challenge ve kategorili oyun kataloğunu gösterir.

## Sınırlar

`app` rendering ve MVVM; `core:domain` saf kurallar; `core:data` kalıcılık; `core:platform` Android/Firebase/reklam/ses adaptörleri; `backend` güvenilen public-score yazıcısı. Hilt uygulama bağımlılıklarını sağlar. Modül sınırları Android bağımsız kuralların sunucuda da aynı sürümle çalışmasını sağlar. Oyun kuralları Compose state ve Android saatine bağımlı değildir.

GameState immutable'dır. `start / tick / act / pointer / finish` ortak reducer akışını kullanır. Aktif süre monotonic clock üzerinden ilerler. Duraklatma ve arka plan timer'ı durdurur. Oyun hazırlanması/generators Dispatchers.Default üzerinde; disk ve ağ işi arka planda çalışır. Precision Canvas 16ms, diğer turlar 32ms ticker kullanır; 60 FPS ölçüldüğü iddia edilmez.

## Gerçek mevcut mekanikler

| Oyun | Mevcut kurallar | Henüz tam olmayan içerik/mekanik |
|---|---|---|
| Tile Blast | Bağlı grup temizleme, gravity/refill, 5'li line/8'li color special, taşlar, skor hedefi/hamle limiti | Bomba, buz, renk bazlı görev kampanyası ve denge |
| Traffic Escape | Araç seç/sürükle, tek eksende collision, 6×6 grid, artan araç/truck sayısı, optimal hamle BFS, yasal ters karıştırma | One-way/özel engel kampanyası, seviye çeşitliliği QA |
| Spot the Difference | İki özgün kafe fotoğrafı, beş gerçek fark, touch regions, yanlış input süre cezası, rewarded hint | 3/7/10 farklı geniş içerik kütüphanesi |
| Path Finder | Seeded maze, anahtar/kilit, kısa yol metriği | Teleport/switch/tek yön/gizlenen harita |
| Color Rush | 4→6 renk, grid büyümesi, TEXT/INK Stroop, azalan cevap süresi | Hareketli bloklar ve distractor balansı |
| Reaction | Random WAIT/GO, erken ceza, 5→10 tur, çoklu hedef, gerçek average/best ms | Fake signal/hareketli hedef |
| Quick Math | Çarpma, parantezli çıkarma, true/false, time pressure | Daha geniş expression generator |
| Memory Grid | 3×3→6×6, kısa preview, artan pattern, ordered Hard input | Sequence sunum polish |
| Precision Tap | Hareketli hedef, daralan hit radius, merkez/kenar puanı | Çoklu/sahte hedef |
| Sequence Master | Artan/azalan sıra, negatifler, artan sayı adedi | Decimal/hareket/gizlenme |
| Word Scramble | TR/EN authored dictionary, tüm harfler, geçerli anagramlar | Geniş dil içerik paketi/bonus harf |
| Number Merge | Eşit komşuları birleştirme, boş hücreye taşıma, chain, spawn, dead-board | Endless ayrı mod ve denge |
| Pattern Master | Gizlenen yön/sembol dizisi, ters-yön kuralı, artan uzunluk | Sunum/sequential animasyon polish |
| Rapid Sudoku | 4×4→6×6, satır/sütun/region, unique-solution generation, hata | Ek authored challenge paketleri |
| Logic Grid | 3×3 shape/rotation/color/count matrix, unique seçenekler | Daha geniş symmetry/position kural aileleri |

## Skor ve ilerleme

Skor: base + speed + accuracy + combo + perfect, ardından difficulty multiplier. Eski 1.000 puan tavanı kaldırıldı; taşma güvenliği için session score üst sınırı 2 milyon. Oyun tamamlanması veya yeterli isabet/accuracy oyun ustalığını ilerletir. Player level ayrı quadratic XP eğrisine bağlıdır. Achievement'lar gerçek sonuç/level eşiklerinden açılır. Top-100/1000 başarımları yalnız server rank doğrulaması ile eklenmelidir; yerelde uydurulmaz.

DataStore sürümü 2'dir. Eski coin/XP/history korunur, eski oyunun rekoru competitive rekor olarak kullanılmaz. Result UUID ledger aynı sonucun tekrar kaydını önler. Wallet event IDs coin grant/claim/purchase/double işlemlerini birleşim sırasında tekilleştirir. Live simultaneous-device restore ve büyük history performansı ayrıca test edilmelidir; büyüyen local history için Room ledger migration yayın ölçeğinde değerlendirilmelidir.

## Çevrimdışı ve rekabet

Guest random nickname + UUID veya kullanıcı local nickname oluşturur. Offline oyun ve local progression kalıcıdır. Google hesabı Credential Manager/Firebase Auth ile bağlanır; anonymous hesap mümkünse link edilir, account collision durumunda yerel event history mevcut Google profiline merge edilir. Firestore private progress ile public verified rank ayrıdır. Cloud history/wallet ayrı immutable belgeler; header transaction; ağ hatalarında local outbox korunur.

Ranked session sunucudan seed/level/sessionId alır. Input/tick trace server domain motorunda replay edilir. Sonuçlar global/ISO-week/game/daily board'larına yalnız Admin SDK transaction ile gider. Günlük challenge UTC tarihinden deterministiktir; tüm oyuncular aynı seed/level kullanır. Weekly skor oyun başına en iyi sonuçların toplamıdır. Ranking 25'lik pagination, 60s cache ve own-rank count sorgusu kullanır. Gerçek backend yokken boş/configuration state gösterilir.

## Yayın kapıları

Canlı Firebase auth/link/restore/rules QA; replay servisi deploy ve abuse/App Check; oyun denge ve geniş içerik; frame-time/bellek/cold-start ölçümleri; API 24/tablet/large-font/TalkBack; release R8/AAB/signing; gerçek reklam/UMP no-fill/reward callback; gizlilik/veri silme ve Play metadata. Aktif tur process death sırasında kaybolur; completed results kalıcıdır. Yayın kalitesi veya tamamlanmış multi-week campaign iddiası yapılmaz.
