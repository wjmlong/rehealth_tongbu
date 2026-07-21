# Orchestrator prompt

Use this in the root Codex conversation:

```text
Read AGENTS.md, ENGINEERING.md, and CODEX_ORCHESTRATION.md.

I want to run the ReHealth Android MVP work as coordinated parallel workstreams.

First, do not edit code. Inspect the repository and produce:
1. current repo map;
2. build/test commands;
3. files likely touched by each workstream;
4. conflict risks between workstreams;
5. recommended branch order.

Then spawn read-only subagents, one per workstream:
A Android build health
B BLE/background collection
C feature extractor
D network/offline sync
E backend mobile API
F model-service
G QA/release

Each subagent must return:
- current state
- target files
- implementation plan
- risks
- validation commands
- estimated order

After all subagents return, consolidate into a single implementation queue.
Do not edit files until I approve the queue.
```

