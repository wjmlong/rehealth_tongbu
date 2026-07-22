# Final Emulator Binary QA — Clone Fidelity Review

## Recommendation

- **recommendation:** REQUEST_CHANGES
- **review type:** Evidence-blocked visual fidelity review
- **product verdict:** NOT DETERMINED
- **evidence verdict:** BLOCKED
- **goal:** Compare the final emulator binary's `top`, `middle`, `bottom`, and `second-page` captures with user target screenshots 4–6, verifying preserved Demo UI structure, CJK integrity, factor-row anatomy, and card order.

The target packet is valid and inspectable. The required final-binary actual capture directory, `.omo/evidence/final-emulator-binary-qa/`, did not exist on the initial check and still did not exist after the required wait-and-recheck cycle. Approval is therefore impossible. This is an evidence blocker, not evidence that the product UI is defective.

## Findings

### CRITICAL

None established. No final-binary rendered frame was available from which to prove a critical product defect.

### HIGH

1. **[evidence] The complete final-binary comparison set is absent.**
   - Missing directory: `.omo/evidence/final-emulator-binary-qa/`.
   - Consequently there are no inspectable `top`, `middle`, `bottom`, or `second-page` PNGs from the final binary, no PNG signatures or dimensions to validate, and no same-build capture set to compare with screenshots 4–6.
   - This blocks every requested rendered-surface assertion: preserved Demo hierarchy, card order, factor-row anatomy, expanded detail layout, CJK wrapping/clipping, crash/blank-screen absence, and second-page continuity.
   - Older captures under `.omo/evidence/final-emulator-ui/` and `.omo/qa/restore-demo-attribution-live-data/` are not substitutes for the specifically requested final-binary evidence and were not used to infer a pass.

### MEDIUM

None established. Product-level visual findings require the missing final-binary frames.

### LOW

None established.

## Target Anatomy To Verify

The three supplied target PNGs are valid PNG files and directly establish the expected structure:

1. **Screenshot 4 / top:** `健康归因` header; 7/30/90-day segmented selector; health-improvement card; current risk and trend; personal-risk forecast card with two series, confidence legend, explanatory copy, and forecast metrics.
2. **Screenshot 5 / middle:** continuation of the risk forecast; behavior card in the same Demo position; then the contribution-factor card and grouped factor section start.
3. **Screenshot 6 / bottom:** ordered grouped factors with numbered circular rank badges, label/value hierarchy, signed score, separate contribution percentage, indented dual-tone progress bars, row disclosure state, and an expanded detail callout; attribution remains the selected bottom-navigation owner.

The final capture set must demonstrate this order and anatomy without CJK truncation, tofu, clipped baselines, semantic phrase breakage, blank/crashed content, or reordering on the second page.

## Product Blockers

None can be established from the available artifacts. The review deliberately does not convert missing evidence into a product defect.

## Evidence Blockers

1. Provide fresh, valid PNG captures from the exact final installed binary at `.omo/evidence/final-emulator-binary-qa/` covering `top`, `middle`, `bottom`, and `second-page`.
2. Include enough provenance to bind the captures to that binary/build, plus PNG signature/dimension validation. All four frames must be fully composited and cover the intended scroll/navigation states.
3. Re-run this comparison after the evidence exists; approval requires direct inspection of all four actual frames against all three target screenshots.

## Evidence Inspected

### Reference targets

- `C:/Users/wjmlong/AppData/Local/Temp/codex-clipboard-5225098f-c281-4643-a359-18534546938d.png` — valid PNG, 402×794, SHA-256 `EC0DC22DFD9AF8E5B6E40E976D7D388667EA099243AC78E36604A066DA324177`.
- `C:/Users/wjmlong/AppData/Local/Temp/codex-clipboard-fd52493f-fa6c-4bd6-a38c-711b8548048f.png` — valid PNG, 417×918, SHA-256 `3E44842BB9AC5B4ECF0D71C42DC1A17C3C5542492915BDE21FCF4CDA08C5D817`.
- `C:/Users/wjmlong/AppData/Local/Temp/codex-clipboard-59460fe8-0d16-4a6f-9e07-71294dab2db6.png` — valid PNG, 403×925, SHA-256 `DACD49E11D52C495B6E27F3338BE43BB15517B5807F2595E08D4B3E454D737DF`.
- `.omo/qa/restore-demo-attribution-live-data/task-1/target-and-baseline.md`.
- `.omo/plans/restore-demo-attribution-live-data.md`.

### Review context

- `.omo/evidence/restore-demo-attribution-live-data-clone-fidelity.md` — prior review only; not treated as final-binary proof.
- `.omo/evidence/final-emulator-ui/` — enumerated as older evidence only; not treated as the requested final-binary capture set.
- Git revision at review time: `bda9dde3cab64b4621f61513c2c7988f9d7b4098`.

### Missing required evidence

- `.omo/evidence/final-emulator-binary-qa/` — absent on initial inspection and absent after one wait-and-recheck cycle.

## Final Decision

**REQUEST_CHANGES** due solely to the HIGH-severity evidence blocker. Product fidelity remains unjudged until the required final-binary captures are present and directly inspected.
