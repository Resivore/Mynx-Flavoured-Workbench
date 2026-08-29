# Testing

## Retained runtime evidence

Canary 7, embedded version `0.3.3-acquisition-priority-fix-canary`, is the
current, accepted, and retained rollback release. The exact artifact is
`offhand-shift-click-qol-0.3.3-acquisition-priority-fix-canary.jar`, 9,718
bytes, with SHA-256
`d0c2ebe7cd2d1a6dcbe2654dbee42dd9f5dce4fd710ea3748f3d15dad6f2b04a`.

On 2026-08-20, those exact Offhand bytes paired with Carried Container
Auto-Routing `0.3.4-menu-sync-priority-fix-canary` at SHA-256
`2143cf5f90c181b363cfa7d8d20a5e565f2b26800065e52907b4b24f64d8ad7d`
passed the focused procedure below and were accepted unchanged. The reviewed
log contained no relevant crash, mixin, listener/snapshot, synchronization,
loss, duplication, rollback, or routing error. This is a focused paired pass,
not exhaustive or standalone Offhand validation.

This migration performed no deployment, Minecraft launch, or runtime check and
did not change either Test Slot, the accepted baseline, Test Instance Manager
state, or either gameplay profile. Its Java 25 / Gradle 9.5.1 build passed all
20 focused fixtures in three suites, including the three static composition
checks against the separately migrated Carried source tree. No Carried files
were changed or synthesized by this task.

## Current focused regression procedure

Run this procedure only under separate runtime ownership, using the exact
Offhand artifact above. When validating the paired carrier tier, also record
the exact Carried artifact and hash. Steps 3, 4, and 8 and the carrier tiers in
steps 5–7 are paired checks. Reconcile exact source, destination, carrier,
cursor, remainder, and total item counts after each mutation.

Steps 5 and 6 state the exact hierarchy accepted with Carried 0.3.4. For a
later Carried successor, use that project's current counterpart-owned full
hierarchy while retaining Offhand's single-delegation, exact-remainder, and
foreign-slot checks; do not attribute a later carrier/storage priority change
to Offhand C7.

1. Open the Survival inventory for 30 seconds, interact with ordinary slots,
   and confirm the screen remains responsive.
2. Open Creative inventory, switch between inventory and item tabs twice, and
   confirm it remains responsive.
3. Repeatedly add and remove bundle contents. Every click and sprite update
   must be immediate, and the next unrelated action must work on its first
   click.
4. Repeatedly add and remove filled-shulker contents in Survival and Creative;
   close and reopen, then reconnect, and confirm the exact carrier and contents
   persist.
5. With an empty hotbar slot and matching destinations present, ground-pick up
   a matching stack. Confirm matching selected main hand → matching unlocked
   carrier → matching occupied physical hotbar → matching occupied offhand →
   empty physical hotbar → remaining ordinary inventory.
6. Repeat the preceding acquisition-order check with chest QUICK_MOVE.
7. From the player inventory, QUICK_MOVE a matching stack and confirm matching
   occupied offhand → matching unlocked carrier → normal menu handling of the
   exact remainder, with the clicked source excluded.
8. Lock and unlock a hovered carrier. Confirm tooltip/action-bar feedback,
   locked skip, stable first-carrier order, component-identical matching, and
   exact remainder. With Inventory Extended and Trinkets present, confirm no
   routed item enters equipment, Trinkets, trash, or another foreign slot.
9. Save, disconnect, reconnect, and shut down normally; inspect the matching
   log for mixin, menu, synchronization, loss, duplication, rollback, or
   routing evidence.

For a separately authorized standalone check, omit the carrier-specific rows
and confirm Offhand alone routes matching selected main hand → matching
occupied physical hotbar → matching occupied offhand → empty physical hotbar →
remaining ordinary inventory. That is a future procedure, not preserved
standalone-pass evidence.

## Stop conditions

Stop and preserve the exact world, stack, and log evidence on any startup or
mixin failure, unresponsive inventory screen, delayed carrier mutation, item
loss or duplication, stale carrier contents, wrong routing tier, component
mismatch merge, source-slot reuse, incorrect remainder, foreign-slot routing,
or persistence failure. Do not replace or promote the accepted C7 artifact as
part of diagnosis, and do not broaden a result beyond the cases actually run.
