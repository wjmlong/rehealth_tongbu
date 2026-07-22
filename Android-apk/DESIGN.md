# ReHealth Android Attribution Design System

This contract is scoped to the attribution tab. It codifies the existing ReHealth Android visual language and the historical Demo attribution reference at `D:/rehealth_demo/Android-apk@7008f2f`; it does not authorize an app-wide redesign.

## 1. Atmosphere & Identity

The attribution tab is a calm clinical dashboard: light mint surfaces, restrained borders, dense but legible evidence rows, and explicit provenance. Its signature is the ranked Core16 factor list, which keeps every factor visible while distinguishing returned model evidence from unavailable data.

## 2. Color

All attribution colors are defined in `ui/theme/AttributionTokens.kt`.

| Role | Token | Value | Usage |
|---|---|---:|---|
| Page | `AttributionPalette.Page` | `#F8FCFB` | Screen background |
| Surface | `AttributionPalette.Surface` | `#FFFFFF` | Cards and selected text |
| Subtle surface | `AttributionPalette.SurfaceSubtle` | `#F7FBFA` | Empty and expanded evidence states |
| Primary text | `AttributionPalette.TextPrimary` | `#092A3E` | Titles and values |
| Secondary text | `AttributionPalette.TextSecondary` | `#718087` | Supporting and unavailable copy |
| Accent | `AttributionPalette.Accent` | `#19A96B` | Selection, protective direction, actions |
| Accent soft | `AttributionPalette.AccentSoft` | `#E8F8F1` | Rank badges and status backgrounds |
| Risk direction | `AttributionPalette.ContributionRisk` | `#E39A22` | Positive risk contribution |
| Worsening | `AttributionPalette.ImprovementWorsening` | `#E36B61` | Negative improvement |
| Forecast comparison | `AttributionPalette.ForecastNoAction` | `#F28B82` | No-action forecast |
| Forecast reduction | `AttributionPalette.ForecastReduction` | `#4E7BFF` | Projected reduction metric |

No color implies diagnosis. Missing data uses secondary text and a zero-length neutral track, never a favorable or adverse color.

## 3. Typography

The system font remains Android's platform sans-serif for native CJK metrics. `AttributionTypography` defines the complete tab scale: 25sp page title; 14sp card title; 12sp selector/subtitle; 11sp body; 10sp detail; 9sp evidence; 8sp metric label; 27sp improvement score; 20sp current risk; and 13sp factor/forecast scores. Body and safety copy use explicit 17sp line height; detail copy uses 15sp.

## 4. Spacing & Layout

`AttributionDimensions` owns all attribution geometry. The screen uses an 18dp gutter and 12dp vertical rhythm. Cards use 16dp content padding, 18dp radius, and a 1dp border. Historical component-specific values such as the 28dp factor rank and 38dp evidence indent are named tokens because they define anatomy rather than arbitrary spacing.

The tab has one vertical scroll owner (`LazyColumn`). Primary content must remain readable on the supported phone viewport without horizontal scrolling. Safety copy has a deliberate line break so `医学诊断` remains intact.

## 5. Components

### Attribution Card

- Structure: bordered surface with a title row and typed content state.
- States: ready, loading, refreshing, empty, accumulating, and error/retry.
- Accessibility: visible text describes provenance and unavailable data; retry uses a native button.

### Period Selector

- Structure: three equal-width 7/30/90-day tabs.
- States: selected and unselected.
- Accessibility: selectable group with tab role and selected state.

### Ranked Factor Row

- Structure: stable 1–16 rank badge, factor/value column, signed returned contribution, absolute-contribution share, indented progress track, and indented disclosure.
- States: returned positive, returned negative, missing, expanded, and collapsed.
- Data rule: share is `abs(factor contribution) / sum(abs(returned contributions))`; missing factors show `--`. No score, share, or clinical value is synthesized.
- Accessibility: the row is a button with an expanded/collapsed state description and an action label.

### Metrics And Charts

- Forecast and activity metrics reuse the same tonal metric primitive.
- Chart colors map to named semantic series tokens; fixed insets, strokes, and dots map to dimension tokens.
- Empty charts retain their card anatomy and state why real data is unavailable.

## 6. Motion & Interaction

Only the contribution progress value animates, for 650ms, to communicate a newly rendered magnitude. Period selection and disclosures change state directly; no decorative motion is added. Android system animation settings remain authoritative.

## 7. Depth & Surface

Depth uses borders plus tonal shifts, not shadows. Cards are white with the shared line color. Nested content uses subtle or metric surfaces only when it communicates empty, evidence, activity, or forecast state.

## 8. Accessibility Constraints & Accepted Debt

- Target: readable CJK, intact medical phrases, native touch targets, explicit selected/disclosure state, and no color-only meaning.
- Real-data constraint: visual QA must not seed or fabricate health records. When the emulator has no confirmed records, activity, or server intervention, the honest empty state is the required rendered state; populated variants remain source- and mapper-test-verifiable until real data exists.
- Accepted debt: none introduced by this attribution contract.
