# Accepted BGE C58 runtime evidence

Accepted release:

- BGE C58: `cnm-nibaru-integration-4.2.2-bge.canary58.glass-corner-uv+26.2.jar`, 6,036,614 bytes, SHA-256 `1a4e4d1cd9c8709720ec84975e70caffb5552ac676537b9bbae42dca96567e87`, embedded version `4.2.2-bge.canary58.glass-corner-uv+26.2`.
- Runtime identity: deployment `128c78b7-a80d-4b3f-8605-6cdf31bbebf1`, artifact `7c990388-34b7-4225-8f14-a0b9b5bfae94`, release source `d1d753a86b21467141bd39a0bcc1270f7827a8d7`.

The user independently reported aggregate `PASS` for this exact BGE member of the verified revision-62 Slot A cohort. The report does not identify individual checklist observations, so no checklist row is inferred as tested or passed. It is not a blanket result for the companion Trowel member, other BGE versions, or future artifacts.

Manager revision 63 recorded the exact-member result, revision 65 removed the completed cohort, and revision 66 atomically promoted exact C58 with exact private Shulker Trowel C10 into Baseline Stack v14. C58 replaced accepted BGE C52 and absorbed historical accepted standalone Nibaru deployment `e5eb4fcb-6c49-4ab2-86f9-1605ccd192ab`; the unified accepted artifact now supplies both `cnm_terrain_slabs_compat` and stable alias `more_slabs_stairs_and_walls`. Exact C52 remains the BGE rollback.

## Current procedure

No active runtime procedure is required for accepted C58. Preserve its exact bytes, filename, hash, embedded version, release source, accepted identity, and aggregate evidence. Do not reinterpret the aggregate report as row-level evidence, rebuild or repackage the accepted JAR, or apply its result to a successor. Any future release needs its own identity, policy-conforming runtime dependency metadata, validation, deployment, and runtime result.
