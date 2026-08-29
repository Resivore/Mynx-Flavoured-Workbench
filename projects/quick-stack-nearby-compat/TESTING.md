# Testing

C6 (`0.1.0-canary6`) is the current and accepted release. Its exact retained
artifact is `quick-stack-nearby-compat-0.1.0-canary6.jar`, 21,805 bytes, with
SHA-256
`c2f4ae3b02a5517ad184998c784c356d90132aeaee91546c91d03a878be6ce98`.

The distinct passing rollback is C4 (`0.1.0-canary4`), retained as
`quick-stack-nearby-compat-0.1.0-canary4.jar`, 17,181 bytes, with SHA-256
`93581de741daa22426a22e1c816b630b9e0ffb90224a2cd27260563530afb441`.
C4 is superseded, not failed. C5 is a failed dual-button-visibility predecessor
and is not a rollback release.

This migration does not deploy either artifact, alter a Test Slot or accepted
physical baseline, launch Minecraft, or add runtime evidence.

## Preserved evidence

At the legacy C6 source checkpoint, Java 25 `clean test build --no-daemon`
passed 36 focused JUnit tests and all 8 required Fabric GameTests. The retained
artifact verifier also passed. Those are static/build results, not gameplay
validation.

Migration verification repeated the same offline Java 25, Gradle 9.5.1, and
Loom 1.17.19 `clean test build --no-daemon` contract against the exact C6
reference inputs: 36/36 JUnit tests and 8/8 required GameTests passed. The
fresh 21,803-byte JAR at SHA-256
`c089e419ccad5d23ce365546f1dc301b05899d1ed8b4dca3f6e41ca623535754`
was not substituted for the retained release. Every class, Fabric metadata,
manifest, and common Mixin resource matched byte-for-byte; the only entry
difference was CRLF rather than LF line endings in the otherwise identical
client Mixin JSON.

The exact C6 deployment used official
`quick-stack-nearby-0.4.0.jar` (SHA-256
`43f1130527f782a291231c682791b4fd3766a20916c691cbdb98f91fdcc47e53`),
Inventory Search 3.4.0, its embedded Inventory Sort Core 3.4.0, patched
Inventory Extended 1.1.2 Canary 4, Clutter No More 2.0.7+26.2, Nibaru C42,
and CNM integration 1.36. The user reported an aggregate **FOCUSED RUNTIME
PASS** for the exact deployed C6 and approved promotion. No individual
checklist row was separately reported, so none is inferred.

The manager had returned `READY_TO_TEST_VERIFIED` for the exact deployment,
then promoted the same live C6 bytes and returned the cumulative Workbench to
23 accepted artifacts with zero overlays. This is deployment and promotion
evidence, not a second gameplay test. Exact C4 had its own earlier aggregate
focused runtime pass on Nibaru C41 and CNM integration 1.36 and remains the
passing rollback.

By the frozen legacy checkpoint, the cumulative stack had later advanced to
Nibaru C43 and CNM integration 1.39. Their accepted-stack presence is context,
not proof that a specific QSN regression row was rerun against them.

## Present regression procedure

Use the exact accepted C6 artifact and official QSN 0.4.0. Record the complete
enabled stack, world, log, filenames, versions, and SHA-256 values before
testing. When reproducing the original C6 environment, use Inventory Search
3.4.0, embedded Inventory Sort Core 3.4.0, patched Inventory Extended 1.1.2
Canary 4, Clutter No More 2.0.7+26.2, Nibaru C42, and CNM integration 1.36.

1. Open the normal Survival player inventory. Confirm Inventory Search owns
   the base right-edge button and QSN appears directly below it, with no shared
   visible or clickable pixels.
2. Click Inventory Search and confirm its native search UI opens. Left-click
   QSN and confirm nearby quick stack runs; right-click it and confirm QSN's
   own rules screen opens.
3. Close and reopen the inventory, toggle the recipe book, and repeat both
   controls to cover initialization order and recurrent positioning.
4. Put a matching item in Inventory Extended row 4, 5, or 6 and a matching
   destination nearby. Confirm QSN uses the lower-row source while hotbar,
   equipment, offhand, crafting/result, Trinkets, trash, and foreign menu slots
   remain outside this project's source-window expansion.
5. Exercise the lock/keep-count UI on one lower-row item and confirm QSN keeps
   its own rules, packet ceiling, persistence, movement, and remainder behavior.
6. Merge one CNM ShapeMap-equivalent geometry into a partial nearby
   destination and repeat in reverse. Confirm the existing destination geometry
   wins, exact source variants remain distinct, and QSN retains target order,
   capacity, and remainder ownership.
7. Put a matching destination in a vanilla shelf and another in a chest or
   barrel. Confirm the shelf is ignored and the ordinary container remains
   eligible without changing QSN's access, validity, or ordering behavior.
8. Open a representative real container and Creative inventory. Confirm their
   Inventory Search classification remains upstream-owned and that QSN does
   not add its player-inventory action button to Creative.
9. Repeat a vanilla player-inventory case without Inventory Extended, an
   Inventory Search case without Inventory Extended, and a QSN case without
   Clutter No More. Confirm each optional integration stays soft and ordinary
   QSN behavior remains native.
10. Reconcile source, destination, and total counts throughout. Confirm no
    duplication, loss, ghost stack, item conversion, component collapse,
    unexpected nearby-routing behavior, relevant error, or crash.

Stop and preserve the exact world and log evidence if any identity cannot be
established, either button is missing or overlapping, a foreign slot enters
the source window, a shelf receives items, a ShapeMap merge changes the wrong
geometry or variant, counts diverge, or a relevant Mixin/client-server error
appears. Do not broaden a future result beyond the cases actually observed.
