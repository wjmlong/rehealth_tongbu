# Debug journal

Artifacts to retain as task evidence:
- .omo/evidence/complete-rehealth-backend-model-service/wave-2-5-fix-b788ed8/**

Temporary artifacts to revert:
- none

Hypotheses:
- H1 T2 failure gate passes without runtime state transition because it only echoes JSON.
- H2 T3 legacy fixture is falsely rejected because key-name matching treats a boolean exclusion marker as raw payload.
- H3 T4 reactor command omits -am and -Dcases is not consumed.
- H4 T5 lacks its dispatcher and model-service URL/provider-secret validation.
