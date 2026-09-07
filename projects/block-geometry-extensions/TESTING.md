# BGE C59 runtime procedure

Current candidate: `cnm-nibaru-integration-4.2.3-bge.canary59.external-materials+26.2.jar`, 6,041,722 bytes, SHA-256 `7aa074c4d04503a8118dbcaca5e4a2e4c0ded9ec8b9d51dd8ef4cb154a98bd52`, embedded version `4.2.3-bge.canary59.external-materials+26.2`, source checkpoint `4e59f44c924af0dbb5d233e6de6b9c5ca2c7735d`.

C59 has passed controlled validation but is not deployed and has no Minecraft runtime result. C58 remains the accepted rollback: `cnm-nibaru-integration-4.2.2-bge.canary58.glass-corner-uv+26.2.jar`, SHA-256 `1a4e4d1cd9c8709720ec84975e70caffb5552ac676537b9bbae42dca96567e87`.

When the serialized Test Instance Manager has assigned C59 to a dedicated 26.2 Workbench slot, use a profile containing the corresponding optional providers and verify only observed behavior:

- Ribbits: Mossy Oak Planks BGE geometry is present.
- Mynx Trees: Wisteria Log and Silver Birch Log retain side/end texture distinction under BGE geometry; Wisteria Wood and Silver Birch Wood use bark on every face.
- Macaw's Paths: exactly the 13 each Running Bond, Windmill Weave, Flagstone, and Crystal path materials plus the five soil paths are admitted; no other Macaw's Paths source is admitted.
- Existing C58 BGE families, registry identities, geometry, and accepted Glass Corner behavior remain unchanged.

Do not mark any row passed without direct observation. GameTests, static validation, artifact inspection, or deployment/readiness verification do not constitute Minecraft runtime PASS. Do not use or modify the protected Matcha Flavoured 26.1.2 profile.
