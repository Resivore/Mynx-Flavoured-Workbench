# Testing

## Canary 23 — pending dedicated-slot economy and Naturalist one-shot test

Canary 23 changes cumulative profession thresholds to `0/5/15/30/50` XP, while every ordinary Tier 1–4 offer still awards `1/2/3/4` XP and maximum-tier offers award zero. Loading a current-schema saved Ribbit may add only tiers newly eligible under the lower schedule; it preserves existing offer state and never lowers saved XP. Sorcerer rank 2 still requires its first Benzene success (and has a 5-XP floor afterward); Fisherman rank 5 still requires its first Opal success (and has a 50-XP floor afterward).

Naturalist fauna remains an optional, two-offer selection. Each selected fauna offer is separately one-use and never ordinarily restocks. The guard is server-side: an exhausted offer rejects a stale payment completion before it consumes extra Glowcaps. Normal serialized `MerchantOffer` decoding reattaches that guard only to the exact persisted Naturalist fauna provider range, so other Wandering offers retain their existing behavior.

Canary 23 is `ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`. When a serialized Test Instance Manager slot is deliberately available, use only the dedicated Matcha Flavoured 26.2 Workbench. Never access or modify the protected Matcha Flavoured 26.1.2 gameplay instance. Rehash retained `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary23.jar` before deployment and replace, never duplicate, the existing `ribbits` JAR.

1. Launch the normal managed 26.2 stack with Naturalist and Ribbits C23. For an existing C22-or-earlier saved Ribbit, record its XP/rank and current offer uses before loading C23; after opening its menu, verify it is eligible immediately under the new schedule, its saved XP is not reduced, and pre-existing offer uses/menu selections remain intact.
2. For an ordinary five-tier profession, purchase lower-tier offers and verify the same `1/2/3/4` XP awards unlock ranks at 5, 15, 30, and 50 cumulative XP. Confirm a Sorcerer cannot reach rank 2 before its Benzene trade, then reaches it with the 5-XP floor; confirm a Fisherman cannot reach rank 5 before its Opal trade, then reaches it with the 50-XP floor.
3. Spawn or find a Wandering Ribbit with the Naturalist fauna provider. Confirm exactly two independently selected fauna offers are present where entries resolve, with correct localized baby names and unchanged bucket-fauna names.
4. Buy fauna offer A once. Confirm A becomes exhausted and cannot consume another Glowcap or yield a second result; confirm fauna offer B remains purchasable. Perform a normal restock/day transition and confirm neither exhausted fauna offer refreshes.
5. Save, restart, and reopen the same merchant. Confirm the exhausted offer is still exhausted, B's independent state is retained, and ordinary provider offers retain their normal restock behavior.

Record only observed behavior. Stop with `FAIL` on reduced saved XP, lost existing offer state, an early gate bypass, changed per-offer XP, fewer/more than two resolved fauna offers, a second fauna purchase, a fauna restock, lost exhaustion after reload, raw translation key, incorrect item/entity result, or unrelated regression. Do not record runtime `PASS` from build, static tests, resource validation, archive inspection, or GameTests.
