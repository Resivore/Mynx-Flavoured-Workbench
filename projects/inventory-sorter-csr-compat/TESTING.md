# Inventory Sorter CSR Compatibility — Canary 3

Use only the dedicated Matcha Flavoured 26.2 Workbench through a serialized Test Instance Manager transition. Never access the protected Matcha Flavoured 26.1.2 profile. Test with the exact retained Canary 3 artifact, Inventory Sorter 3.0.0, and a compatible CSR release that includes specific portable-container identities (controlled baseline: CSR Canary 19).

First establish the no-reservation control: sort the same ordinary inventory with the compat disabled and enabled. The final layout, stack merging, counts, and components must be indistinguishable from Inventory Sorter's normal result.

## Reservation fill and anchoring

- Empty match: reserve an empty slot for cobblestone, place cobblestone in an unreserved slot, and sort. The reserved slot must receive it before the remaining unreserved stacks sort.
- Nonmatch: repeat with only dirt or another nonmatching item. The reservation must remain empty; it must never be used as ordinary sorting workspace.
- Partial and excess: put 32 cobblestone in its reservation and 64 in an unreserved slot. After sorting, the reservation must contain 64 and the remaining 32 must participate in the normal unreserved layout.
- Insufficient and multiple donors: separately use a donor too small to fill the reservation and donors split across several physical slots. Every available matching item must transfer in physical donor-slot order without loss or duplication.
- Equivalent targets: reserve two empty physical slots for the same stack and supply only one full donor. The first physical reserved slot must fill first.
- Full match: a full matching reserved stack must remain exact and all surplus matching items must stay in the ordinary sortable pool.
- Wrong occupant: put a wrong item in a reserved slot. Its exact stack, count, and components must remain untouched; do not repair, evict, merge, or replace it.
- Reserved donor: put a matching item in one reserved slot while another equivalent reservation is empty. The occupied reserved slot must never supply the empty reservation.
- Component sensitivity: use same-item stacks with different names or other incompatible components. A partial reserved stack must accept only a stack that CSR admits and vanilla can legally merge; incompatible components must remain separate and exact.
- Conservation: before and after every case, count each item and compare the relevant component patches. Stop on any loss, duplication, fabricated stack, component change, or cursor remainder.

## Portable outer stacks

- Generic empty shulker and bundle reservations must accept matching unreserved empty outer stacks while preserving name, type/variant, and every component.
- Create specific CSR reservations for one filled shulker and one filled bundle. Put the exact identity-bearing outer stacks in unreserved donor slots, alongside same-type lookalikes with different CSR identities. Sorting must move only the exact identities into their reservations and preserve contents, names, types, identity UUIDs, and all components.
- Keep filled unreserved shulkers and bundles in an obviously unsorted mixed inventory. They must move as complete outer ItemStacks; their contents must not be inspected, extracted, reordered, reconstructed, or merged.
- Put loose compatible items beside one and several partially filled bundles and sort with Inventory Sorter's bundle option enabled. Loose items must remain outer inventory stacks; bundle contents must not increase or change.

## Mapping, mixed layout, and parity

- Use ordinary stacks, an empty/partial reservation, a movable filled shulker, a movable filled bundle, and surplus matching items together. Reservation fill must happen first; every reserved slot must then remain frozen while Inventory Sorter lays out only the unreserved remainder.
- Repeat with distributed reservations and a supported double container. Verify both reservation fill and subsequent masking use the correct combined physical order and each half's correct local slot.
- Exercise the same empty, partial, multi-donor, wrong-occupant, portable-identity, and mixed cases through a configuration that reaches Inventory Sorter's client fallback. Its final slots/counts/components must match the authoritative server path, and no fallback click after the fill prefix may address a reserved slot.
- In the player main inventory, sort with shulkers only and then with one and several bundles. Preserve Canary 2 behavior: shulkers use the normal path; bundles and shulkers remain movable outer stacks; no loose item may enter bundle contents. CSR does not gain player-slot ownership in this canary.

Stop and record `FAIL` if a matching eligible reservation does not fill, a nonmatching item enters one, a reserved slot becomes general workspace or a donor, a wrong/full occupant changes, server and fallback results diverge, portable contents/components change, or item conservation fails. Compilation, JUnit, bytecode contracts, GameTests, and launch checks are controlled evidence only; record desktop runtime evidence only when actually observed in the dedicated instance.
