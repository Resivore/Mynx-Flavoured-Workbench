# QSN C18 runtime procedure — not currently deployed

**CONTROLLED_VALIDATION_PASS — NOT_DEPLOYED — RUNTIME_UNTESTED**

Exact candidate: `quick-stack-nearby-compat-0.1.0-canary18.jar`, 72,102 bytes, SHA-256 `cb9144de133ac2dd9db20925b1275200213fcf57b36f9b5d530a6b02ac905554`, source `009e455b91df0cdea1bd1489097d8a0ce2359ade`. C18 must be assigned only through a verified serialized Test Instance Manager transition in the dedicated Matcha Flavoured 26.2 Workbench. The paired specific-identity provider is CSR C17: `container-slot-reservations-0.1.0-canary17.jar`, 190,491 bytes, SHA-256 `5ae4d65b70b8f8d2ed07d9b33724ae9a1b7048518bb5a4ba6ef72fadbf0f4b82`, source `34f0104c561ff93c12999fc7b14a57d3d8643114`.

1. Put a non-empty shulker in a supported nearby chest, reserve its physical slot with CSR C17, remove it, and press QSN. The whole shulker returns to that exact empty reservation; all 27 contents, name, color, CSR identity, and unrelated components are unchanged.
2. Remove, add, or rearrange its internal contents; separately rename and legitimately recolor it. QSN still returns the same identified shulker if CSR reports its slot as a match.
3. Give its internal items ordinary nearby destinations, then press QSN. Confirm the whole matched shulker returns first and none of its contents drain during that press.
4. Carry a visually identical but different shulker against the specific reservation. It must not enter the slot. A populated shulker with no home, or a blocked matching home, must remain an outer item while C17/C16 carried-content behavior still applies.
5. Empty an identified shulker after reserving it, then press QSN. It must follow the existing normal empty-outer QSN/CSR path; do not treat it as C18's populated special case.
6. Apply a QSN user source lock or full keep rule to the populated outer shulker. A matching reservation must not override that rule. Check another populated shulker without a home, ordinary loose items, bundles, Inventory Extended storage, nested shulker targets, ShapeMap affinity, and result conservation in the same session.

Record only observed Minecraft behavior. Automated checks and a successful launch are not runtime PASS. Do not access the protected Matcha Flavoured 26.1.2 gameplay profile.
