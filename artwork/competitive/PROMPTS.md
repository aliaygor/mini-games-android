# Competitive visual assets

Mode: built-in ImageGen, original photorealistic cafe photograph plus a precise-object-edit variant. Both source PNGs are in this directory; resized WebP copies are bundled in `drawable-nodpi/difference_cafe_a.webp` and `difference_cafe_b.webp`.

Generate prompt: Original 3:2 architectural editorial photograph of a premium contemporary cafe with navy and walnut materials and natural window daylight. Straight-on eye-level composition; brass wall clock, shelf plant, foreground red cup, blue book and central geometric framed art. No people, children, mascot, text, brand or watermark.

Edit prompt: Preserve camera, furniture, lighting and every other object. Make exactly five localized edits: remove clock hands; gray shelf plant pot becomes cobalt blue; red cup/saucer becomes emerald; blue book cover becomes red; burnt-orange lower-right geometric artwork becomes turquoise. Same resolution and composition.

Verified visible target regions are normalized in `GameScreen.kt`: clock, shelf pot, cup, book, artwork. Compare both images when modifying hit regions. Minor generative texture drift is not a scored target.

Content currently includes one real five-difference cafe pair. A publishable campaign still needs an authored, reviewed library of city/office/architecture/nature/technology pairs, with 3/5/7/10 distinct target regions, difficulty metadata, asset licensing provenance, touch-region QA and no duplicate levels. Do not describe this single pair as a complete multi-week Spot-the-Difference campaign.

## Real vehicle sprites — 6 October 2026

Mode: built-in ImageGen, new transparent assets. Originals: `vehicle-red-source.png`, `vehicle-blue-source.png`, `vehicle-van-source.png`. Alpha-preserving crop and resize produce the bundled `vehicle_red.webp`, `vehicle_blue.webp`, `vehicle_van.webp`. Both the playable parking scene and menu thumbnail consume those assets.

Red prompt: Production Android parking puzzle game sprite. One realistic premium 3D rendered red sports sedan, strict orthographic overhead view, front up. Entire car and tires visible; detailed hood, windshield, roof, rear windshield, mirrors, headlights and rear lights; metallic paint. Adult racing game quality, crisp at small size. No lettering, logo, ground, road, border or additional objects. Centered, width about 45 percent of height, truly transparent background, tiny contact shadow.

Blue prompt: One metallic blue SUV, strict orthographic overhead view, front up, four tires and mirrors. Recognizable detailed automotive shape, headlights, hood, windshield, roof sunroof, rear windshield, tail lights. Premium realistic 3D rendering, width roughly half height, transparent background, tiny contact shadow. No text, logos, road or other objects.

Van prompt: One mustard yellow long delivery van, strict orthographic overhead view, front up. Entire vehicle, black tires, mirrors, hood, windshield, roof panels, rear doors and lamps. Premium realistic 3D rendering, width roughly one third height. Transparent background, tiny contact shadow. No logo, lettering, road or additional objects.

Bubble spheres, burst particles, asphalt, bay markings, curbs and landscaping are rendered with Compose Canvas. Prior arcade/mascot assets are retained only as source history.
