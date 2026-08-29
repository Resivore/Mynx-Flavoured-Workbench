# Container Slot Reservations

Container Slot Reservations preserves a player's organization intent after a
physical stack leaves an ordinary storage slot. Canary 1 targets Minecraft
Java 26.2 Fabric and deliberately remains independent of inventory-routing
engines.

## Canary 1 scope

Supported placed containers are exact vanilla:

- chest and trapped chest;
- barrel;
- every vanilla shulker-box color;
- double chests, with each physical half retaining ownership of its own local
  slots `0..26`.

Copper chests, arbitrary modded containers, player inventory/equipment,
crafting/result slots, Trinkets, trash slots, and foreign menu slots are not
eligible. The resolver is an adapter boundary so later container support can
be added without changing reservation identity or routing policy.

## Identity and lifecycle

A reservation stores an immutable, count-one `ItemStackTemplate`. Admission is
exactly `ItemStack.isSameItemSameComponents`; registry item identity alone is
never sufficient. Count is intentionally ignored by that comparison, while
custom names, item models, Matcha logical identity, and all other meaningful
components remain significant. The template is metadata and is never exposed
as container contents or included in physical item counts.

With the remappable reservation key while a supported screen is open:

- an occupied unreserved slot is reserved for its live physical stack;
- pressing again on the same occupied identity clears the reservation;
- an occupied slot with a different reservation replaces it with the live
  physical identity, without moving or destroying the stack;
- an empty slot uses the live carried/cursor stack as its template;
- an empty slot plus an empty cursor clears its reservation.

Using the cursor as a template does not click the slot, split, shrink, move,
insert, or duplicate the carried stack. Removing a physical item never clears
its reservation.

## Persistence

Persistence format `1` is a sparse, versioned component with at most 27 unique
local slot entries. Placed reservations live in the owning block entity's
Minecraft 26.2 persistent component map under the project-owned component
`container_slot_reservations:reservations`. Mutations rebuild that map while
preserving every unrelated component and mark the block entity changed.

This provides native close/reopen, chunk unload/reload, world save/reload, and
server-restart persistence for chests, trapped chests, barrels, and placed
shulkers. Ordinary chest/barrel drops do not retain reservations in Canary 1.

For shulkers:

1. item placement uses vanilla `BlockItem` component application to restore
   the custom component into `BlockEntity.components()`;
2. survival drops retain the same vanilla drop stack and apply the shulker
   block entity's unconsumed component map through Fabric's loot-drop event;
3. a narrow Creative predicate lets a physically empty but reserved shulker
   use vanilla's existing component-bearing item-drop path;
4. placement then restores the reservation to the same local indices.

No carrier stack is rebuilt from a registry ID. Container contents, color,
custom name, lore, enchantments, and unrelated components remain on the same
vanilla shulker item wherever Minecraft's component model supports them.

## Insertion enforcement

Reservation checks are an admission constraint, not a sorting engine.

- Vanilla chest/barrel and shulker menu destination slots retain their normal
  behavior and add the reservation predicate to `mayPlace`.
- Vanilla `moveItemStackTo` therefore preserves QUICK_MOVE ordering while
  skipping only empty slots reserved for another identity.
- Supported randomizable block entities add the same predicate to
  `Container.canPlaceItem`, which is the vanilla hopper insertion seam.
- Extraction is untouched. Emptying a slot leaves its reservation intact.

An occupied compatible stack continues to merge and extract normally. The
server never permits the reservation action to create an occupied slot whose
template conflicts with its physical stack.

## Server authority and client display

The client sends only the live menu ID, live menu-list slot index, and one of
three intent modes: slot stack, carried stack, or clear empty. It never sends
an authoritative template. The server revalidates player/menu state, menu ID,
slot bounds and activity, exact supported ownership, current physical stack,
and current carried stack before deriving or changing a reservation.

The server synchronizes every eligible destination menu index plus an optional
count-one display template. Empty reserved slots render the real item model
without decorations/count, followed by a translucent wash and project-owned
outline so it reads as a ghost rather than a physical stack. Their tooltip is
`Reserved: <item>` plus `No physical item is present`. Occupied reserved slots
receive only a small marker. Rendering uses each live `Slot.x`/`Slot.y`, so it
follows Inventory Extended layout changes rather than moving logical slots.

The dedicated keybinding ships **unbound**. Known Workbench defaults already
include Carried on `R`, portable crafting on `V`, and StrippingToggle/QSN on
`B`; an unbound default avoids claiming an unverified free key. One initial
key press produces one request; key repeat and tick-consumption are not used.

## Public query API

`dev.resivore.slotreservations.api.ContainerSlotReservationsApi` exposes
defensive queries for supported placed `Container` instances and vanilla
shulker `ItemStack` values:

- `getReservation` / `isReserved`;
- `reservationMatches`;
- `mayInsert` as the reservation admission predicate;
- `classify`, returning `OCCUPIED_COMPATIBLE`, `RESERVED_MATCH`,
  `UNRESERVED_EMPTY`, `RESERVED_OTHER`, `NON_WRITABLE`, or `INELIGIBLE`.

The API distinguishes intent and eligibility but does not choose destinations.
A later explicit Carried, QSN, or other routing integration can apply its own
priority policy without duplicating reservation semantics.

## Explicitly deferred

Canary 1 does not include Nested Shulker Stacking, Stack to Nearby Chests,
Carried or QSN integration, automatic sorting, dump-all behavior, category/tag
matching, multiple identities per slot, restock quantities, cross-container
templates, search, labels, or filter-pipe behavior.

See `TESTING.md` for automated scope and the later focused runtime matrix.
