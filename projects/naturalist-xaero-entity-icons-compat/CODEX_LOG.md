# Codex Log

## 2026-09-07T18:00:00Z — Implement Naturalist × Xaero Entity Icon Compatibility Canary 1

- Revision: 1
- Source checkpoint: `ab199db39ece792fc6b38fa5edf1fe7536e89ba1`
- Changes: Created the distinct companion with a closed registry covering 37 exact Naturalist entity IDs and renderer-selected exact model classes. The post-empty Xaero prerender bridge uses explicit head/neck paths for ordinary animals and explicit compact-body paths only for the documented fish/invertebrate/scorpion families; it copies current ancestor transforms without mutating Naturalist models. The ten known-good Naturalist controls remain unowned. Unknown class/path/trace/draw states fail closed; targeted reload eviction covers only owned Naturalist results.
- Build/static: Two independent offline clean Java 25 / Loom 1.17.19 `check stageCanaryArtifact` runs passed. JUnit coverage verifies the exact 37-target roster, ten native controls, and duck egg/dirt trail exclusions; binary checks verify Xaero 26.4.2/nested XaeroLib and Naturalist C8 seams; archive inspection found only companion classes/resources. Both archives were byte-identical.
- Runtime: No Minecraft launch or runtime observation. Canonical manager revision 114 had Slot A occupied by BGE C59 and Slot B occupied by IBF C4, so no deployment, profile mutation, cache test, or protected-instance access occurred.
- Artifact: `naturalist-xaero-entity-icons-compat-0.1.0-canary1.jar`; version `0.1.0-canary1`; 16,901 bytes; SHA-256 `df5a2322b4b6c745fd1fe4b846075629869242b9c6ebb61c3480857f3f3c5de2`; source `ab199db39ece792fc6b38fa5edf1fe7536e89ba1`; runtime dependency policy `CAPABILITY_OR_PROVIDER` with no exceptions.
- Result: ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED. Naturalist C8 and accepted Xaero × EMF and Ribbits × Xaero artifacts remain unchanged.
- Next state: Retain Canary 1 in the primary checkout, publish revision 1 from authoritative main, and use the complete TESTING.md matrix only after a free-slot manager transition is authorized.
