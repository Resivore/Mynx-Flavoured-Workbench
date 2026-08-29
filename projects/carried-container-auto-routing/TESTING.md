# Testing

C11, historically named `0.3.6-external-quick-move-priority-fix-canary`, is the current and accepted release. Its exact retained artifact is `carried-container-auto-routing-0.3.6-external-quick-move-priority-fix-canary.jar`, 34,528 bytes, with SHA-256 `01913b0455d961d20174c601b0544c178ed47868113e6f1173e132290fe4ca02`.

The exact runtime-passed rollback is `0.3.5-storage-partial-priority-fix-canary`. Its retained artifact is `carried-container-auto-routing-0.3.5-storage-partial-priority-fix-canary.jar`, 34,489 bytes, with SHA-256 `610f6c65c36468c9b98ce8aa7892f8a57003c59a28cb989fe3c80742bec4eaa2`. It is not failed and is not the live accepted release.

This migration does not deploy either artifact, alter a Test Slot or accepted physical baseline, launch Minecraft, or add runtime evidence.

## Preserved historical runtime evidence

- The user reported that the exact deployed C11 / `0.3.6` binary passed the intended focused external `QUICK_MOVE` storage-partial-priority scope in aggregate. This is an aggregate focused runtime pass; it does not assert that each representative case below was separately reported. The same bytes were promoted without rebuild, and the legacy accepted-stack verifier returned exactly `READY_TO_TEST_VERIFIED` with 23 accepted artifacts and zero overlays.
- The user reported that exact rollback `0.3.5` passed its focused passive-pickup storage-partial-priority runtime scope in aggregate. It was promoted unchanged, and the legacy accepted-stack verifier returned exactly `READY_TO_TEST_VERIFIED`. It remains the known passing rollback and was not invalidated by C11.
- Exact `0.3.4-menu-sync-priority-fix-canary`, paired with exact Offhand Shift-Click QoL `0.3.3`, previously passed the focused Survival/Creative inventory, repeated bundle/shulker mutation, persistence, acquisition, player-origin routing, lock, ordering, component-identity, and exact-remainder scope with no observed crash or loss. That artifact is a superseded historical predecessor and is not migrated.
- Exact `0.3.3-inventory-open-safety-fix-canary` was unsafe: repeated shulker interaction crashed with carrier loss after reload, bundle interaction lagged one action, and empty hotbar slots incorrectly preceded matching carriers/offhand for ground and external acquisition. Its successor fixed the demonstrated snapshot synchronization and priority defects. The failed artifact is not migrated.

No standalone or exhaustive runtime validation is inferred from those historical observations. Dedicated-server behavior was not established by the preserved evidence.

## Representative future recheck

Use only the exact retained C11 artifact, with the established compatible Offhand Shift-Click QoL `0.3.3` identity when paired composition is under test. Reconcile source, destination, remainder, and total counts after every mutation.

1. In chest `QUICK_MOVE`, place a compatible partial stack at Inventory Extended backing indices `9`, `36`, and `62` in separate repetitions while an empty hotbar slot exists. Each occupied storage partial must fill before a new hotbar stack opens.
2. Give a storage partial room for `10`, transfer an incoming stack of `20`, and confirm the exact `10` remainder may then use the empty hotbar. Repeat with multiple compatible storage partials and confirm stable inventory order and exhaustion before hotbar placement.
3. Preserve occupied-destination priority: selected main hand, matching unlocked carried shulker/bundle, compatible physical hotbar stack, matching occupied offhand, compatible ordinary-storage partial, empty physical hotbar, then empty ordinary storage.
4. With no compatible occupied stack, confirm empty hotbar still precedes empty ordinary storage. Confirm a component-distinct stack does not merge or block that fallback.
5. Repeat representative external transfer from a barrel and furnace, preserving source cleanup, menu callbacks, and exact remainder.
6. Sanity-check passive ground pickup and player-origin `QUICK_MOVE`; player-origin transfer must offer matching occupied offhand and qualifying carried containers before returning the exact remainder to the menu's normal path.
7. Recheck stable first-carrier order, locked-carrier skip, lock/unlock feedback, component-identical matching, and exclusion of empty or unrelated carriers.
8. Repeatedly mutate bundle and filled-shulker contents in Survival and Creative inventory, then save/reload or reconnect. Confirm immediate synchronization, carrier persistence, no duplication or loss, and no related exception or Mixin conflict in `logs/latest.log`.

Stop and record a failure or inconclusive result if the artifact filename, embedded version, or SHA-256 differs; an inventory/menu screen crashes or desynchronizes; a carrier or item is duplicated or lost; any destination tier is selected out of order; exact remainder accounting fails; locked/empty/unrelated carriers claim items; or relevant listener, snapshot, routing, or Mixin errors appear. Do not broaden the runtime classification beyond the cases actually observed.
