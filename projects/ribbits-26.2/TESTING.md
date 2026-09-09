# Testing

## Canary 22 — pending dedicated-slot Naturalist fauna trade test

Canary 22 changes only the private English-language migration path. The source-safe and assembled private `assets/ribbits/lang/en_us.json` both contain `trade.ribbits.naturalist_fauna.baby = Baby %s`; the existing provider still supplies Naturalist's `entity.naturalist.<species>` component as its argument. No fauna catalogue, bucket variant, Glowcap price, offer selection, baby entity data, scheduler, Chute, leash, or other trade behavior changes.

Canary 22 is `ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`. When a serialized Test Instance Manager slot is deliberately available, use only the dedicated Matcha Flavoured 26.2 Workbench. Never access or modify the protected Matcha Flavoured 26.1.2 gameplay instance. Rehash retained `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary22.jar` before deployment and replace, never duplicate, the existing `ribbits` JAR.

1. Launch the normal managed 26.2 stack with Naturalist and Ribbits C22.
2. Spawn or find a Wandering Ribbit with Naturalist fauna trades and inspect multiple baby-fauna offers. Confirm names render as `Baby Deer`, `Baby Zebra`, or equivalent Naturalist species names—not `trade.ribbits.naturalist_fauna.baby`.
3. Confirm the species portion uses Naturalist's own localized entity name.
4. Inspect at least one Naturalist bucket-fauna offer and confirm its normal item name is unchanged.
5. Purchase and use one baby-fauna trade; confirm it produces the intended baby entity.

Record only observed behavior. Stop with `FAIL` on a raw translation key, incorrect species name, changed bucket item name, incorrect baby result, or unrelated regression. Do not record runtime `PASS` from build, static tests, resource validation, archive inspection, or GameTests.
