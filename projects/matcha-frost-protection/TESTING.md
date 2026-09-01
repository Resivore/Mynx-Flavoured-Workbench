# Testing

## Accepted identity and evidence

**CANARY 2 ACCEPTED — USER-REPORTED EXTERNAL AGGREGATE PASS**

The accepted Workbench Stack v10 identity is exact
`matcha-frost-protection-0.1.0-canary2.jar`, 18,373 bytes,
SHA-256 `88ae708cbf2a9b1f5fe3b322579c3e7b507c7e0420cadb40aca796dba5b4a742`,
from source checkpoint `0d3d6650355a38758ff23227b34bc9bda5826d68`.

The user reported an aggregate external runtime `PASS` for that exact current
canonical candidate. No individual matrix-row observations were supplied, so
none are inferred. The accepted-stack manager installed and physically
verified the artifact as accepted deployment
`e3c2471a-2419-422d-8dac-0faf5681a790`; that accepted-baseline action is not a
Test Slot deployment and creates no Test Slot history. Canary 1 remains a
historical artifact only and is not a rollback release.

The retained clean Temurin 25.0.4.1 / Gradle 9.5.1 build passed all 28 focused
tests against Minecraft 26.2 and pristine Matcha Flavoured 1.12. That remains
static evidence independent of the external aggregate runtime result.

## Future regression setup

1. Verify the exact accepted version, filename, SHA-256, source checkpoint,
   dependencies, and enabled stack before testing. Stop on any identity,
   readiness, dependency, or ownership mismatch.
2. Launch the exact Minecraft 26.2 Matcha stack, run `/reload`, and stop on any
   registry, Mixin, function, recipe, or resource-reload error.
3. Prepare single armor pieces for HEAD, CHEST, LEGS, and FEET with
   `main:freezing_protection` at levels I, II, and III. Off-slot pieces may be
   provisioned by controlled commands; do not change Matcha's chest-only
   supported-items/acquisition contract or maximum level III.
4. Start each cold-water case without lingering effects. Matcha's penalties
   last five seconds; Canary 2 prevents new application and deliberately does
   not clear effects already applied by an earlier unprotected case.

## Powdered-snow matrix

| Case | Required observation |
| --- | --- |
| No Frost Protection | Ordinary vanilla behavior returns: no solid walking surface, freeze ticks accumulate while exposed, the powder-snow overlay appears, and full freezing can damage. |
| Leather boots baseline | Solid walking surface without deliberate descent; freeze ticks do not accumulate, so no new overlay or freeze damage. Sneaking/deliberate descent remains possible. |
| Frost Protection I, II, and III on HEAD | Every level matches the leather-boots baseline for walking, underlying freeze state, overlay, damage, and deliberate descent. |
| Frost Protection I, II, and III on CHEST | Same leather-boots-equivalent result. |
| Frost Protection I, II, and III on LEGS | Same leather-boots-equivalent result. |
| Frost Protection I, II, and III on FEET | Same leather-boots-equivalent result. |
| Remove the only qualifying piece | The next exposure immediately returns to ordinary traversal and freeze-tick accumulation. |
| Equip while already partially frozen | Frozen ticks thaw through vanilla's leather-wearable path; there is no direct tick clear or HUD-only suppression. |

Where useful, inspect `TicksFrozen` while testing. The decisive result is that
the underlying value stays at zero for fresh protected exposure or thaws at
the vanilla rate after protection is equipped; the overlay must follow that
state rather than being independently hidden. Recheck representative cases
after save/reload.

## Matcha freezing/cold-water matrix

Use a non-Creative player with water at `~ ~1 ~` in a biome in
`#minecraft:is_frozen`. Matcha 1.12's complete active penalty set is Slowness V
for five seconds, Darkness I for five seconds, and one point of `freeze`
damage per qualifying tick. It applies Darkness, not Blindness.

| Case | Required observation |
| --- | --- |
| No Frost Protection | All three Matcha penalties apply normally. |
| Frost Protection I on each armor slot | Slowness and Darkness still apply; the penalty path still issues freeze damage, subject only to Matcha's existing level-scaled enchantment protection. |
| Frost Protection II on each armor slot | Same normal Matcha penalty path, with only the existing level-scaled damage protection. |
| Frost Protection III on HEAD | No new Slowness, Darkness, or freeze damage from this Matcha cold-water system. |
| Frost Protection III on CHEST | Complete immunity remains and preserves Matcha's original maximum-level intent. |
| Frost Protection III on LEGS | Complete immunity to all three penalties. |
| Frost Protection III on FEET | Complete immunity to all three penalties. |
| Multiple level-III pieces | Same boolean immunity; no stacking or additional behavior. |
| Remove the last level-III piece | The normal three-penalty path resumes on the next qualifying tick. |
| Co-located protected and unprotected players | The protected player remains immune while the unprotected player receives their own penalties; no nearest-player cross-targeting occurs. |
| Unrelated environmental effect or damage | Behavior is unchanged; only the exact Matcha freezing-water condition function is redirected. |

## Preserved contracts

- Ordinary Frost Walker and its water behavior remain unchanged.
- Blessing of Demeter keeps its exact recipe shape, ingredients, method, item
  identity, name, lore, model, and count. The crafted book's complete stored
  enchantment set is exactly `main:freezing_protection` level III; Frost Walker
  and every additional enchantment are absent.
- Recheck representative client and dedicated-server or multiplayer cases if
  acceptance requires both authority paths.

For a future row-scoped regression, record `PASS` only after every applicable
row is observed against this exact artifact and stack. Stop and record `FAIL`
or `INCONCLUSIVE` for any overlay/state disagreement, blocked deliberate
descent, missing or leaked cold-water penalty, player cross-targeting, recipe
drift, unrelated regression, or client/server disagreement. Record only
observations actually made; do not infer row results from the current aggregate
external report or from static tests.
