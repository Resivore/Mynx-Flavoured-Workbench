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

The requested target comes only from the profile's typed role:

- Slab: `effectiveSlabSource()`
- Stair: `effectiveStairSource()`
- Wall: `nativeWall()`
- Vertical Slab: provider-owned `DerivedGeometrySupport.Geometry.VERTICAL_SLAB`
- Step: provider-owned `DerivedGeometrySupport.Geometry.STEP`

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
and switch sound. An accessor supplies a six-entry typed icon list. A render
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

The mode ordinal is stored under `shulker_trowel.geometry` in the trowel
stack's standard `DataComponents.CUSTOM_DATA`. Invalid or absent stored values
read as Full.

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
