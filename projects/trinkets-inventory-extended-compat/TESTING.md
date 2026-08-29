# Testing

## Retained accepted runtime evidence

The accepted Canary 5 release is the exact three-file set below. The
Inventory Extended Canary 4 filename is intentional because that unchanged
patch remained the accepted Inventory Extended member of Canary 5.

| File | Embedded identity | SHA-256 |
|---|---|---|
| `inventoryextended-1.1.2-mc26.2-trinkets-compat-canary4.jar` | `inventoryextended` `1.1.2` | `a0ced554cb687f7119aa19ac3466c0de3ffb0a888e994514223152335b043636` |
| `trinkets-4.1.0-beta.3+26.2-inventory-compat-canary5.jar` | `trinkets_updated` `4.1.0-beta.3+26.2` | `4c1fa6ac36c0457483fd0d395b99bbd94c9334aad6defece7633bbf0552d1724` |
| `trinkets-inventory-extended-compat-0.5.0-canary5.jar` | `trinkets_inventory_extended_compat` `0.5.0-canary5` | `76c8c735219ceebfc7ec9a7b103e4cd4f027e43b2489cbc71f8b025b103efa6c` |

On 2026-08-14, these exact bytes passed the intended client runtime scope:
startup after removal of the competing required mixin; Creative-to-Survival
wrapper mapping; single correctly positioned Totem and Elytra rendering; the
normal charm icon without `missingno`; ordinary equip and unequip; all six
Inventory Extended storage rows; physical-hotbar mapping; storage-before-hotbar
quick move; the full-storage offhand boundary; save/reload; leave/rejoin; and
relevant client-log inspection. The configured Matcha `keepInventory=true`
death/respawn path also passed with equipped Trinkets items and Inventory
Extended contents preserved.

The disposable localhost server reached `Done` and stopped cleanly, but the
later client attempt was not representative and was abandoned. It is neither
hosted-server validation nor a Canary 5 failure. Hosted-server join, relog, and
dimension coverage and `keepInventory=false` drops remain untested.

This migration performed no deployment, Minecraft launch, or runtime check.
Run the procedure below only under separately assigned runtime ownership.

## Current representative runtime procedure

1. Record the exact three filenames and SHA-256 values above, Minecraft 26.2,
   Fabric Loader and Fabric API versions, complete mod/resource stack, world,
   and matching client and server logs. Do not enable pristine or obsolete
   copies of Inventory Extended or Trinkets, another companion canary, or
   `trinkets-ie-visual-diagnostic-0.1.0.jar` alongside C5.
2. Reach the title screen and open the test world. Confirm the three embedded
   mod identities load and there is no required-mixin, creative-slot,
   menu-index, networking, or protocol failure.
3. In Survival Inventory, inspect all six Inventory Extended rows, the physical
   hotbar, armor, offhand, and Trinkets slots. Contents and logical slot
   ownership must remain stable.
4. In Creative-to-Survival Inventory with a Totem and Elytra equipped, confirm
   each renders exactly once and centered in its intended Trinkets frame. With
   the charm slot empty, confirm its normal icon appears with no magenta/black
   fallback square or detached duplicate.
5. Equip and unequip the Totem and Elytra normally. Confirm there is no ghost,
   duplicate, lost, or shifted stack and that ordinary Inventory Extended
   contents do not move unexpectedly.
6. Fill all six Inventory Extended rows and the physical hotbar, leave offhand
   empty, and shift-click an equipped Totem and then an Elytra. Each must remain
   equipped and offhand must remain unchanged when no ordinary destination is
   available.
7. Free exactly one physical-hotbar slot and confirm an equipped Trinket can
   move there. Refill the hotbar, free exactly one slot in Inventory Extended
   row 6, and confirm unequip uses that row. With both destinations available,
   confirm storage remains ahead of the physical hotbar.
8. Shift-click compatible items separately from Inventory Extended row 6 and
   the physical hotbar toward Trinkets. Both ordinary-player source ranges must
   still participate without involving offhand.
9. Save/reload and leave/rejoin. Recheck every equipped item, all six extended
   rows, the hotbar, and offhand. If the configured environment uses
   `keepInventory=true`, repeat the recorded death/respawn check without
   generalizing it to the disabled path.
10. For the deferred hosted-server pass, join the actual host, repeat the core
    inventory/equip checks, disconnect/reconnect, change dimension once, and
    verify persistence and client/server agreement. Inspect both logs before
    changing the hosted-server classification.
11. Close Minecraft normally and inspect the matching log for Mixin application
    or injection failures, invalid-slot warnings, menu/index exceptions,
    Trinkets, Inventory Extended or companion errors, and `[VISDIAG]` output.
    None should be present.

Treat whole-screen backgrounds, buttons, static Matcha artwork, or unrelated
inventory-layer behavior as evidence for their separately owned projects. Do
not move Trinkets logical slots or change this compatibility layer to fit a
visual composition issue.

## Stop conditions

Stop and preserve the exact artifact identities, world, and logs on any startup
transformation failure, crash, invalid slot, duplication or loss, ghost stack,
shifted mapping, persistence failure, offhand mutation in the full-storage
boundary, or client/server disagreement. Do not rebuild or replace the accepted
C5 files during diagnosis.
