# Testing

**ACTIVE — CONTROLLED VALIDATION PASS — NOT DEPLOYED — RUNTIME UNTESTED**

Current CCAR Canary 17 is `carried-container-auto-routing-0.3.12-routed-pickup-audio-canary1.jar`, SHA-256 `43a72f37161c5de8d8ae308d552aefe4a7a7c0f5acb3cdc8b59889cc7807af09`, source `c9ed10647c2944a2b0a86146a99ddf2299b8e4e8`. C16 remains its predecessor and accepted C12 remains independent accepted provenance. Use only the dedicated Matcha Flavoured 26.2 Workbench; never open or alter the protected 26.1.2 profile.

With a qualifying unlocked carried shulker or bundle, pick up a matching ground stack and verify exactly one normal-volume vanilla item-pickup sound at clearly lower pitch. Repeated rapid routed pickups must retain vanilla-style pitch variation rather than sounding fixed. Verify a multi-carrier split produces one pop, and a partially carried-routed stack whose remainder enters ordinary inventory also produces one lower-pitched pop with no doubled normal pop.

Pick up equivalent stacks with no carried acceptance, a locked carrier, a full carrier, and a nonqualifying carrier; each must retain ordinary vanilla pickup audio. Verify ordinary pickups, lock toggles, UI actions, QSN, and QUICK_MOVE produce no CCAR pickup cue. Recheck counts, remainders, pickup delay/ownership, pickup animation, statistics/advancements, main-hand priority, carrier order, Inventory Extended, Offhand Shift-Click QoL, CSR reservations, exact components, effective capacities, bundle/shulker behavior, and lock persistence. Stop on startup failure, a missing or doubled cue, altered volume, mechanically fixed pitch, wrong normal-audio fallback, or any routing/count/lock regression. Record only observed runtime evidence.
