# Attribution Restoration Visual Gate Review

## Recommendation

**APPROVE**

## Original Intent

Restore the historical Android attribution-tab surface represented by the three supplied reference screenshots, while binding it to real Jeecg/PIAS/MR11/Room/server-intervention sources and using honest same-layout empty states instead of demo values.

## Desired Outcome

The canonical Android app should present, in order, the attribution header, 7/30/90 selector, improvement/current-risk summary, 30-day PIAS trend, Room-backed activity, grouped expandable Core16 factors, server-only intervention plan, and medical disclaimer. The rendered surface should retain the reference's spacing, rounded-card anatomy, mint/ink palette, typography hierarchy, and CJK legibility without inventing health data.

## User Outcome Review

The fresh API31 emulator captures satisfy the user-visible outcome. Region-by-region comparison shows the same information hierarchy and visual language despite different viewport dimensions and density. Empty summary and PIAS states stay compact inside their target cards; Room activity replaces meal semantics; all 16 factor rows remain represented rather than being dropped; the plan is explicitly server-only; and the final disclaimer remains visible. No blank ready card, CJK clipping, overlapping text, pasted-image implementation, opaque compositor defect, or broken scroll region is visible.

The factor interaction evidence confirms that the rows are real expandable Compose components. The actual accent is somewhat greener than the teal reference, and factor rows omit the reference's numbered rank bubbles, but these are visual notes rather than failures of the stated structure/spacing/radii/color/type-feel criterion: the shared app theme remains coherent, group hierarchy remains clear, and the allowed honest-empty state necessarily suppresses unavailable contribution values.

## Criterion Review

| Criterion | Result | Evidence |
| --- | --- | --- |
| Target regions and order render | PASS | `absolute-final/top.png`, `middle.png`, `bottom.png` |
| Header and period selector fidelity | PASS | Reference 1 vs `absolute-final/top.png` |
| Summary and PIAS card anatomy | PASS | Reference 1/2 vs `absolute-final/top.png` |
| Room activity substitutes meal semantics honestly | PASS | `absolute-final/top.png`, `middle.png` |
| Grouped contribution-factor surface | PASS | Reference 2/3 vs `absolute-final/middle.png`, `bottom.png`; `factor-transition-*.png` |
| Server-only plan and disclaimer | PASS | `absolute-final/bottom.png` |
| Spacing, radii, palette, type feel | PASS | All six supplied images, region-normalized for viewport/density |
| No blank surface or CJK clipping | PASS | All actual PNGs and matching UI XML bounds |
| Fresh installed-build evidence | PASS | source timestamp 2026-07-22 20:01; captures 21:03-21:04; `gradle-absolute-final.log`; `absolute-final/install.txt`; `absolute-final/crash-check.txt` |

## Blockers

None.

## Non-Blocking Notes

1. The live theme uses a greener mint than the reference's more cyan teal.
2. Empty factor rows do not show the reference's numbered circular rank markers. The grouping, order, row hierarchy, progress track, detail affordance, and expandable evidence panel remain intact.
3. The three requested actual captures are different scroll positions rather than same-size reference pairs, so a raw whole-image similarity percentage would be misleading; the review used corresponding visual regions and XML bounds.

## Direct Slop and Programming Pass

- No screenshot/raster is used as the production UI; the surface is a real Compose tree with reusable `AttributionCard`, compact-message, metric, legend, chart, factor-row, and feedback-button primitives.
- No visual-only/deletion-only or tautological test claim was used for approval. Approval is based on fresh emulator artifacts, UI hierarchy bounds, motion/expanded-state captures, and successful Gradle output.
- No invented normalization or parsing was added merely to mimic the screenshots. Honest missing values remain missing.
- `AttributionScreen.kt` is large and contains many composables; this is a maintenance note under the programming perspective, but it does not violate the stated visual or live-data success criteria and therefore is not a blocker.
- The available unrelated dirty-file code-review report explicitly records `remove-ai-slops` and `programming` coverage, but it does not cover this attribution UI. This direct gate pass supplies the missing feature-specific coverage, as permitted when the report artifact is absent.

## Checked Artifacts

### References

- `C:/Users/wjmlong/AppData/Local/Temp/codex-clipboard-5225098f-c281-4643-a359-18534546938d.png`
- `C:/Users/wjmlong/AppData/Local/Temp/codex-clipboard-fd52493f-fa6c-4bd6-a38c-711b8548048f.png`
- `C:/Users/wjmlong/AppData/Local/Temp/codex-clipboard-59460fe8-0d16-4a6f-9e07-71294dab2db6.png`

### Fresh Actual Surface

- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/top.png`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/middle.png`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/bottom.png`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/top.xml`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/middle.xml`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/bottom.xml`
- `.omo/qa/restore-demo-attribution-live-data/task-4/factor-transition-start.png`
- `.omo/qa/restore-demo-attribution-live-data/task-4/factor-transition-mid.png`
- `.omo/qa/restore-demo-attribution-live-data/task-4/factor-transition-end.png`
- `.omo/qa/restore-demo-attribution-live-data/task-4/gradle-absolute-final.log`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/install.txt`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/launch.txt`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/crash-check.txt`

### Source and Review Context

- `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt`
- `.omo/plans/restore-demo-attribution-live-data.md`
- `.omo/qa/restore-demo-attribution-live-data/task-3/manual-qa-plan.md`
- `.omo/evidence/audit-existing-dirty-code-review.md`

## Exact Evidence Gaps

- No same-viewport, same-density reference/actual pairs exist, so no raw pixel-diff score is asserted.
- The requested final captures show the honest empty/live state, not populated PIAS, populated contribution values, or a populated server intervention plan. Those content differences are explicitly allowed and do not prevent review of the card anatomy.
- `omo ulw-loop status --json` was unavailable because `omo` is not installed/on `PATH`; this report therefore uses the required fallback path.
- No feature-specific code-review report was found. The direct source/slop/programming review above fills that gap; the unrelated dirty-file report was not treated as evidence for attribution correctness.
