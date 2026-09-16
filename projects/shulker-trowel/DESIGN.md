# Design and ownership boundary

## Placement authority

`ShulkerTrowelItem.useOn` returns early on the client. The logical server reads
the live offhand shulker's Minecraft 26.2 `DataComponents.CONTAINER`, filters
actual `BlockItem` stacks through the selected geometry resolver, performs the
existing quantity-weighted draw only over eligible entries, and invokes the
resolved target's normal `BlockItem.place(BlockPlaceContext)`.

Each candidate retains both identities:

- the original shulker slot and source `BlockItem`, used for the post-success
  recheck and one-item Survival consumption;
- the resolved placement `BlockItem`, used only for the synthetic one-count
  placement stack and vanilla placement call.

The server rewrites the authoritative offhand component only after a successful
action. Creative does not consume. Failed or empty resolution returns before
the placement-sound scope.

## Exact material and geometry ownership

Full mode is the compatibility fast path: it returns the exact source
`BlockItem`, preserving passing Private Canary 2 even for materials outside the
Nibaru catalog.

Shaped modes resolve the exact source `Block` through:

1. `NibaruProviderAdapter.profile(sourceBlock)`; or
2. the profile carried by an exact accepted runtime binding for an already
   derived CNM geometry.

The requested target comes only from an owner-authored typed role:

- Slab: `effectiveSlabSource()`
- Stair: `effectiveStairSource()`
- Wall: `nativeWall()`
- BGE-derived modes: the corresponding stable `BgeGeometryCatalog.Descriptor`

`TargetGeometry` owns only Full, Slab, Stair, and Wall. It projects every other
mode from `BgeGeometryCatalog.ordered()`, including stable key, persistence ID,
selector order, display name, availability, and exact resolved block/item. The
Trowel therefore has no parallel list of BGE geometry names. A later BGE
descriptor becomes a selector mode without a source change here.

The current BGE catalog contributes Vertical Slab, Step, Corner, Quarter Column,
and Layer. Missing material support fails closed for that exact mode; it never
falls back to Full or a related geometry.

The adapter and provider use exact block identity. No `ShapeMap` lookup,
component offset, canonical-name parser, registry suffix, material string, or
compatibility-owned family database participates in placement. This is the
critical boundary that excludes IBF's non-geometry door, window, shutter, and
fence/gate equivalence families. A missing profile or role fails closed before
weighting.

Native-sparse profiles remain valid when the provider exposes an exact
effective vanilla slab or stair. Sparseness means absence of a typed target,
not absence of a provider-authored native block.

## CNM input and presentation seam

The exact accepted CNM 2.0.7 binary exposes:

- static `ClutterNoMoreClient.onKeyInput(int,int)` and `shapeKey()`;
- public non-final `ShapeSwitcherOverlay` with overridable `changeSlot(int)` and
  `shouldStayOpenThisTick()`;
- the overlay's final `shapes` list and two render-time
  `ShapeMap.transferStack(ItemStack,int)` calls.

A client-only HEAD injection asks the trowel branch to handle the existing shape
key. It cancels only when the main-hand item is this trowel and the trowel
payload channel is available. Otherwise CNM's original method executes
unchanged.

`TrowelShapeSwitcherOverlay` subclasses CNM's overlay to retain its exact
layout, motion, HOLD/TOGGLE/PRESS behavior, scrolling, look-to-switch behavior,
and switch sound. An accessor supplies the dynamic native-plus-BGE icon list.
Each icon is an actual item resolved through the selected geometry descriptor;
if Oak Planks does not support a future role, the first exact supported catalog
material supplies the representative icon rather than collapsing the role. A render
redirect substitutes icons only when the overlay instance is the trowel
subclass; ordinary CNM overlays delegate to the original
`ShapeMap.transferStack` call. The subclass never calls CNM's
`ChangeStackPayload` and never mutates the held item.

A short-lived client UI cache remembers the last requested mode for the same
stack object and selected slot. It prevents rapid PRESS/reopen input from
restarting at an as-yet-unsynchronized authoritative value. The cache clears on
server acknowledgement, stack/slot change, or timeout and never writes item
components; the server remains authoritative.

## Persisted state and networking

The stable mode persistence ID is stored under `shulker_trowel.geometry` in the
trowel stack's standard `DataComponents.CUSTOM_DATA`. Invalid or absent stored
values read as Full. IDs 0 through 6 retain their previous identities; Layer
remains ID 6 even though Corner (ID 7) and Quarter Column (ID 8) precede it in
the selector. Selector order never acts as saved or network identity.

The narrow `change_geometry` client-to-server payload contains only the
requested mode ID. The server rejects invalid IDs, dead players, and players
without the exact trowel in the main hand. A valid request updates that live
stack, marks the inventory changed, and broadcasts the normal menu changes.
The client never speculatively writes the component.

## Preserved reference conclusions

Shulker Palette established quantity weighting and live container-component
access as useful behavior references, but supplies no dependency or runtime
state. Jake's Build Tools supplies only one authorized private sprite through
the verified local assembly process; no JBT code or item identity is copied.
The prior prototype's reflective CNM coupling, registry-name guessing, and
parallel geometry tables remain rejected.

## Private runnable asset boundary

The tracked clean C5 JAR intentionally omits the protected sprite. The accepted
private artifact was assembled from exact Jake's Build Tools input
`jakes-build-tools-v5.0.6.jar` at SHA-256
`7D9C309ADC2955C4C79A4CA43EB958CE41A283B4A629386B3BD55125782302DA`
by adding only `assets/jbt/textures/item/iron_trowel.png` as
`assets/shulker_trowel/textures/item/trowel.png`. That 346-byte, 16×16 sprite
has SHA-256
`D754DBAB87A0FE923016268F0CDDC4A785B022A1184837F9C3CA2BE6732F7789`.

The sprite and exact private C5/C2 runnable JARs remain private,
non-redistributable external identities. They are not tracked here, and a
freshly assembled or rebuilt equivalent must never replace their recorded
tested identities.

## C11 offhand palette reservation

`OffhandShulkerPlacementMixin` injects at the HEAD of Minecraft 26.2's
canonical `BlockItem.place(BlockPlaceContext)`. It returns `FAIL` only when
the placement context is `InteractionHand.OFF_HAND` and the actual block is a
`ShulkerBoxBlock`. This covers ordinary vanilla block-item placement before
the placement path can consume a stack, update components, create a block or
block entity, emit sound/particles, or award successful-placement effects.
The same mixin applies on client and server: the client does not predict a
one-tick placement and the server is final authority.

The rule deliberately examines the Minecraft block type rather than a color
list or registry names. Main-hand shulkers are outside the predicate, as are
all unrelated offhand `BlockItem`s. A Trowel placement remains a main-hand
`BlockPlaceContext`; its source palette remains the live offhand
`DataComponents.CONTAINER` and is not itself a placement stack.

## Optional Quick Right-Click 1.9 seam

The immutable reference is `originals/mods/quickrightclick-26.2.0-1.9.jar`,
125,653 bytes, SHA-256
`87e77365919532bd38a004235c58e1220bbcbae680d8a3d27ed2ce2ddbd7831b`.
Its `fabric.mod.json` identifies mod ID `quickrightclick`, version `1.9`.
The exact Fabric implementation has these relevant binary seams:

- `com.natamus.quickrightclick_common_fabric.config.ConfigHandler` exposes the
  native `enableQuickShulkerBoxes` toggle, defaulting to enabled.
- `QuickEvent.onItemClick(Player, Level, InteractionHand)` recognizes a held
  `BlockItem`, branches on `ShulkerBoxBlock`, and calls
  `features.ShulkerBoxFeature.init`.
- `ShulkerBoxFeature.init` begins with the native toggle, then implements the
  portable temporary-shulker behavior. Its companion shulker block-entity
  mixin restores the held stack after that interface closes.

There is no repository-tracked pack configuration path that can reproduce the
native toggle when an instance is recreated, so C11 does not depend on a
per-instance user config. Instead, `ShulkerTrowelMixinPlugin` enables its
`@Pseudo` `QuickRightClickShulkerCompatibilityMixin` only when Fabric Loader
reports exactly mod ID `quickrightclick` and version `1.9`. At the audited
`QuickEvent.onItemClick` HEAD it returns normal `InteractionResult.PASS` only
for held `ShulkerBoxBlock`s. QRC then has no special shulker behavior while
Minecraft retains ordinary interaction processing. Bed, cartography table,
crafting/smithing table, ender chest, grindstone, and stonecutter branches are
never intercepted. If the mod is absent or its metadata changes, the optional
mixin is not applied; C11 has no QRC class linkage or hard dependency.

The artifact carries no Quick Right-Click classes, resources, or nested JARs.
The focused binary contract test records the exact JAR identity and entry
points without copying upstream implementation.

## C11 private artifact boundary

The local-only C11 artifact starts from the tracked clean
`shulker-trowel-0.1.0-canary10.jar` and adds only the separately authorized
`assets/jbt/textures/item/iron_trowel.png` as
`assets/shulker_trowel/textures/item/trowel.png`. The source and packaged
sprite are exactly 346 bytes with SHA-256
`D754DBAB87A0FE923016268F0CDDC4A785B022A1184837F9C3CA2BE6732F7789`.
No `assets/jbt/` entry, JBT class, Quick Right-Click class, or nested JAR is
present. The private C11 JAR is ignored, locally retained only, and is never
tracked or redistributed.
