# Test Instance Manager v2 foundation

`runtime-state.json` is a gated, empty contract fixture—not a live deployment ledger. It deliberately imports no accepted stack, experimental overlay, snapshot, or runtime result.

The state is one atomic document: accepted baseline plus fixed slots A and B. Each deployment unit owns exact artifacts and ownership keys; each occupied slot stores its own deployment state and runtime result. `tools/runtime_slots.py` validates zero, one, or two left-packed candidates, resolves the effective profile with collision checks, and plans compare-and-swap transitions. Promoting one passing slot updates the baseline and preserves/normalizes the other slot in the same transition.

Live apply is refused while `activation` is `GATED`. A future runtime adapter must acquire the one physical instance lock, verify the expected state revision, apply the exact artifact delta atomically, roll back on failure, and only then commit the ledger transition. It may target only the dedicated Minecraft 26.2 Workbench; the protected gameplay instance is permanently outside scope.
