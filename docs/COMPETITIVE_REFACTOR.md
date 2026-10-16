# Competitive platform refactor

The new brief supersedes the six-game arcade MVP. Preserve the Gradle wrapper, application ID, consent/ads, lifecycle pause and atomic DataStore persistence. Remove mascot, crown, candy typography and the old three-door/card-pair mechanics from the product.

Target catalog: Tile Blast, Traffic Escape, Spot the Difference, Path Finder, Color Rush, Reaction, Quick Math, Memory Grid, Precision Tap, Sequence Master, Word Scramble, Number Merge, Pattern Master, Rapid Sudoku, Logic Grid.

Navigation: onboarding → Home / Leaderboard / Challenges / Profile → game briefing → game session → detailed result. Shop is accessed through Profile. No invented rank, player entries or cloud-connected state.

Domain: deterministic seeded reducers with shared timer/lifecycle and per-game puzzle data, independent difficulty tiers and score breakdown; immutable results. Data: local profile, per-game mastery, idempotent result ledger. Platform: real Firebase adapters only when configured. Backend is an external deployment dependency; client scores must never directly write public leaderboards.

Validation gates: compiling app at each implementation boundary; generator and economics unit tests; all catalog entries exercised on emulator; cloud restoration tested only against a configured Firebase project. Store readiness additionally requires real-device performance/accessibility, expanded difference-image content, signed release, production service configuration and server replay validation.

Initial audit: baseline `:app:assembleDebug :core:domain:test` passed. Existing score cap and six shallow mechanics do not meet the new brief. Existing DataStore file is retained and versioned migrations must preserve historical coins/XP while separating old records from the competitive ruleset.
