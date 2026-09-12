# Testing

## Canary 24 — pending focused Naturalist fauna transaction test

Canary 23 has external `RUNTIME_FAIL` evidence: after purchasing one selected Wandering Ribbit Naturalist baby/bucket fauna offer once, the same offer could still be purchased again. The intended contract is exactly one successful result and one Glowcap payment per each of the two independently selected fauna offers.

Canary 24 guards the server's actual merchant result slot before output transfer. The guard applies only to the exact persisted `ribbits:optional_naturalist_fauna` provider range. It must reject normal clicks, shift-clicks, and rapid or stale displayed-result attempts once that exact offer has been used, without granting an item or consuming payment. It must preserve the two selections, species/entity components, prices, uses, demand, special price, ordering, persistence, and ordinary provider behavior; an already-used fauna offer must remain exhausted after close/reopen and world save/reload and must never ordinarily restock.

Canary 24 is `ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`. Use only a deliberately available serialized Test Instance Manager slot in the dedicated Matcha Flavoured 26.2 Workbench. Never access or modify the protected Matcha Flavoured 26.1.2 gameplay instance. Rehash retained `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary24.jar` before deployment and replace, never duplicate, the existing `ribbits` JAR.

1. Launch the normal managed 26.2 stack with Naturalist and Ribbits C24. Spawn or locate a Wandering Ribbit whose persisted provider snapshot contains `ribbits:optional_naturalist_fauna`; record the exact two selected fauna offers, their item/entity components, Glowcap prices, order, uses, demand, and special prices.
2. Buy fauna offer A once with an ordinary click. Verify exactly one result is received, exactly its listed Glowcap price is consumed, A becomes exhausted immediately, and fauna offer B remains available and unchanged.
3. Try A again with an ordinary click, shift-click, and rapid repeated clicks, including clicking any stale displayed result before the client refresh completes. Verify every attempt yields no second result, consumes no Glowcaps, and leaves A's uses/demand/special price unchanged.
4. Buy fauna offer B once using shift-click. Verify exactly one result and one listed-price payment, then repeat the ordinary, shift-click, and rapid/stale attempts against B and confirm they also yield and consume nothing.
5. Close and reopen the merchant menu. Confirm both exact fauna selections remain exhausted with all recorded fields intact and no selection, component, price, or ordering drift. Save and fully reload the world, reopen the same merchant, and confirm the same state again.
6. Perform the normal time/restock transition. Confirm neither fauna offer restocks, while an eligible ordinary Wandering offer retains its pre-existing purchase and restock behavior.

Record only observed behavior. Stop with `FAIL` on any second fauna result, incorrect or duplicate payment, one offer exhausting the other prematurely, successful stale-result extraction, fauna restock, exhaustion lost after reopen/reload, any persisted field or selection drift, altered ordinary-offer behavior, or unrelated regression. Do not record runtime `PASS` from build, static tests, resource validation, archive inspection, or GameTests.
