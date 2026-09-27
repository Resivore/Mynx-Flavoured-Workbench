# C5 local masonry-closure verification

Current candidate: `architectural-material-closure-0.1.0-canary5.jar`, 1,799,641 bytes, SHA-256 `85202102993D2FBA6FA9CD17576E9D8209CFEA8C71050D73DA55CD3A8E79C1D1`, finalized `2026-09-27T00:48:08.2473578Z` from `1811c86b2b2221836deb299ebd11e32427e86269`. It is `STATIC_PASS / RUNTIME_UNTESTED` and private/local only.

With Java 25, run `test check stageCanaryArtifact`. The verifier must preserve the 17-profile, 646-cell provider-first closure (409 provider-owned; 237 AMC-owned), follow blockstate/item-model/model-parent edges as models, and resolve concrete texture values and inherited `#` variables to actual AMC-generated or legitimate provider PNG sprites. A JSON model must never satisfy a texture reference.

In an owner-approved isolated Minecraft 26.2 environment, verify representative provider states and the four patterned forms for each material. Report missing sprites, provider replacements, invalid drops/recipes, state errors, crashes, or cross-material behavior. This checklist is lifecycle-neutral and does not authorize access to a Minecraft testing profile.
