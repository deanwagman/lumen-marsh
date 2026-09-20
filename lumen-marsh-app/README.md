# Lumen Marsh

Guest companion for **Lumen Marsh**, a fictional eco-futurist wetlands destination. The app reads live attraction conditions, guest advisories, and published park-flow guidance from [VenueOps API](../venueops-api). Guests never see operator identities, work orders, or unpublished recommendations.

## Run

Start the API, then the Flutter guest app:

```bash
cd ../venueops-api && ./gradlew bootRun

cd ../lumen-marsh-app
flutter run -d chrome --dart-define=VENUEOPS_API_BASE_URL=http://localhost:8080
```

Android emulator: `VENUEOPS_API_BASE_URL=http://10.0.2.2:8080`.

Production-style web container (Flutter **3.47.1** pinned in the Dockerfile):

```bash
docker build --build-arg VENUEOPS_API_BASE_URL=http://localhost:8080 -t lumen-marsh-app:local .
docker run --rm -p 3000:8080 lumen-marsh-app:local
# health: GET /health
```

Debug-only design system gallery:

```bash
flutter run -d chrome --dart-define=VENUEOPS_API_BASE_URL=http://localhost:8080
```

Then open `/#/design-system`. The gallery is not registered in release builds.

## Brand principles

- Calm operational UI, slightly more expressive in discovery moments.
- Status is never color alone: pair tone with a label, icon, and sentence.
- Prefer semantic roles (`statusPositive`) over palette names (`lumenGlow`).
- Motion is organic and interruptible. Safety and error messages use the settle spring (no bounce).

## Token naming

| Kind | Example | Use |
| --- | --- | --- |
| Palette | `LumenPalette.lumenGlow` | Theme factories only |
| Semantic color | `LumenColors.statusPositive` | Components via `context.lumenColors` |
| Tone | `LumenTone.positive` | Badges, stripes, banners |
| Space / radius | `LumenSpacing.md`, `LumenRadius.lg` | Layout |
| Breakpoint | `LumenBreakpoints.rail` | Phone vs rail vs expanded |

## Component inventory

Design-system (`lib/design_system/`): layout (`LumenPage`, `LumenContentConstraint`, `LumenSection`), status (`LumenStatusBadge`, `LumenStatusStripe`, `LumenFact`, `LumenNoticeBanner`), feedback, `LumenPressable`, `LumenEntrance`, `LumenIconAction`.

Feature compositions (`lib/features/attractions/presentation/widgets/`): `AttractionCard`, `AttractionHero`, `AttractionFactGrid`, `AttractionStatusBanner`, `WaitTimeDisplay`.

## Motion

Presets live in `LumenSprings`: responsive (press, nav), standard (status change), expressive (hero discovery), settle (entrances and errors). Implementation uses `SpringSimulation` and `AnimationController.animateWith` — no animation package.

`MediaQuery.disableAnimationsOf(context)` removes spatial scale/translate. Opacity may still fade briefly.

Do not feed overshooting spring values into opacity; clamp first.

## Accessibility

- Minimum touch target 48px.
- Visible focus on `LumenPressable`.
- Badges and banners expose text labels, not only color.
- Support 200% text scale and reduced motion (gallery toggles both).

## Adding an attraction status

1. Add the value to the VenueOps domain and guest JSON.
2. Parse it in `AttractionStatus`.
3. Map it in `toneForAttractionStatus` (and icon / recommended action).
4. Do not add attraction-specific colors inside `lib/design_system/`.

## When to create a global component

Promote a widget when a second feature needs the same behavior (badge, banner, page frame, press spring). Keep widgets in the feature when they know domain types, guest copy, or attraction artwork rules.

## Tests

```bash
flutter analyze
flutter test
```
