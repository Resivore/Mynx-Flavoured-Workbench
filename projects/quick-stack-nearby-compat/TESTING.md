# Testing

## Accepted baseline state

**ACCEPTED — AGGREGATE RUNTIME PASS — NO TEST SLOT**

Exact C8 / `0.1.0-canary8` is both current and accepted: `artifacts/quick-stack-nearby-compat-0.1.0-canary8.jar`, 34,941 bytes, SHA-256 `E6AAF43D881F31202931C40B6F40762F2E2DAB1F2AE8D0CE5FD7F213EC0CBBA5`, source `1f545721658cce803200fa277737052cead79b70`.

The user's combined CSR/QSN report was recorded only as an aggregate `PASS` for exact C8 at manager revision 79; no individual checklist-row observation was supplied or inferred. Revision 81 promoted its exact accepted unit into Stack v17:

- Deployment `30a3d125-ecb6-4a7a-862b-ca2e2c1bbcfe`.
- Upstream artifact `cb31d144-b6c4-41fb-ba44-35d896b228f6`: `quick-stack-nearby-0.4.0.jar`, 159,918 bytes, SHA-256 `43F1130527F782A291231C682791B4FD3766A20916C691CBDB98F91FDCC47E53`.
- Compatibility artifact `62f897e7-dc2e-4754-82f0-43240d4655be`: exact C8 above.

C8 no longer occupies Slot B. Exact prior accepted C6 is the declared rollback; exact C4 remains distinct historical passing provenance; exact C7 remains independently failed. None of those results is rewritten or inherited.

## Next launch

At verified manager revision 82 and state digest `19C42A70EF995A671B9A6D063AD61A927F0BD8A22617722FEF103F862F971577`, QSN C8 remains enabled from the accepted baseline while Slot A tests CSR C5 and Slot B tests the Stacks Are Stacks patch. QSN itself is not an experimental candidate and requires no new standalone matrix in that launch.

Any optional QSN observation made during the diagnostic CSR/Stacks Are Stacks probe must be recorded exactly as incidental evidence and must not be inferred as a new C8 result. Do not disable, redeploy, rebuild, or modify QSN, and never access the protected 26.1.2 gameplay profile.
