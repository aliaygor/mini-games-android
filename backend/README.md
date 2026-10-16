# Verified-score service

This JVM service shares `core:domain` with Android. It issues a server seed and competition level, replays actual timer/input commands and computes the score itself. The Android client never writes public leaderboard scores. Private cloud saves are deliberately separate from verified competition state.

Endpoints require a Firebase ID token:

- `POST /v1/start`: `{ "game": "TRAFFIC", "language": "tr", "daily": false }` → server session ID, seed, level and kind.
- `POST /v1/finish`: `{ "sessionId": "...", "nickname": "...", "commands": [["tick",32],["answer",11],...] }` → recalculated verified score.
- `GET /health`: process health.

The server uses Firebase Admin Application Default Credentials. No service account private key belongs in the Android app or repository. Build with `gradlew :backend:installDist`, then build `backend/Dockerfile` with the repository root as Docker context. Deploy to your own Cloud Run project/service identity with only the necessary Firebase Auth read and Firestore data permissions. The service is not deployed by this task; deployment needs your cloud configuration and billing choice.

After deployment put the actual HTTPS service URL in your local Gradle properties as `COMPETITION_URL=...`. No URL is fabricated. Without this value, sessions are local practice and no ranking submission is claimed. Auth providers and Firestore rules must also be configured.

Ranking collection IDs are shared by `RankingBoards`: global and ISO-week per-game/all-game boards; daily challenge per-game/all-game boards. Aggregate scores sum each game's personal best in the period. The server transaction atomically records use of a session, leaderboard entries and competition mastery. Duplicate receipts do not award a second score.

Security constraints: owner-bound seeds, one active session per account, start rate limit, finished-state replay, bounded input log/body, simulated duration bounded by wall-clock duration, 15-minute expiry and reaction plausibility checks. Firestore rules deny all client writes to competition/public boards. These checks prove legal gameplay, not a human player's physical touch; Play Integrity/App Check, stronger per-input timing attestation, abuse load tests and Firebase Emulator rule tests remain release gates.

Cloud restore uses private immutable session and wallet documents rather than one ever-growing Firestore result blob. Pagination and a 60-second ranking cache bound reads. Simultaneous-device/offline/private-wallet migrations still require live integration QA; this is not represented as a verified production service before that QA.
