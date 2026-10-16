# Bubble Pop and Traffic Escape rework — 6 October 2026

The user's feedback identified two concrete failures: vehicles looked like rectangles, and the first game lacked the feel of tapping and popping connected bubbles. This revision replaces those two presentations and updates the home layout and shared game HUD.

## Implemented

- Bubble Pop: glossy spherical bubbles, connected color groups, burst rings and particles, animated gravity/refill, distinct pop audio, invalid single-bubble shake without consuming a move. Groups of five create a cross blast; groups of eight create a color blast. Specials chain when struck. Stones remain anchors, and refills preserve a legal move. Rules remain seeded and replayable.
- Traffic Escape: three original transparent rendered vehicle sprites (sedan, SUV and long van), textured parking surface, bay markings, curbs, landscaping, visible exit lane, selection highlight, animated legal movement, drag controls and accessible arrow controls. Movement still uses collision-checked puzzle logic.
- Home: actual new game artwork, larger game covers, concise descriptions, lighter header, category controls and compact progression. All games share the adjusted typography and card shape.

## Verification

- `:app:assembleDebug` passes.
- `:core:domain:test` passes 21 tests, including two new tests covering special chains, stone preservation, fall origins and legal refills over five difficulty tiers.
- `:app:lintDebug` passes (existing warnings remain).
- Actual emulator checks: tapping the opening blue group increased the score from 0 to 1,876, reduced remaining moves from 23 to 22 and registered one correct move. Dragging vehicle 4 down one cell changed its bounds and the move counter from 0 to 1.
- Actual gameplay captures: `artwork/competitive/new-bubbles.png`, `new-traffic.png`, `new-home.png`. They are app screenshots, not generated UI mockups. Vehicle generation prompts and source provenance are in `artwork/competitive/PROMPTS.md`.

## Scope and remaining work

This is a focused quality revision of two games and the shared presentation. It does not mean all fifteen games have been rebuilt or that the product is ready for Google Play. The other games' content and mechanics, production backend deployment, device/tablet performance and release checks still need their own review. Client and competition server must use the same domain revision because special-chain rules changed.
