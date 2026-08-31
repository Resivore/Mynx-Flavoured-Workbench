# Testing

The retained current candidate is `matcha-vanilla-village-restoration-0.1.0-canary1.zip`, SHA-256 `2b0d29ee6389c457b801091aa7469e6c53468944db3ede83760dc7d8fb49b8e4`. It remains `ACTIVE`, `STATIC_PASS`, `DEPLOYED`, and `RUNTIME_PASS` after the user reported an aggregate pass for that exact candidate. No individual runtime cases were supplied, so none are inferred.

For a future regression:

1. Reverify the exact candidate hash and the pinned Matcha 1.12 hash before launch.
2. In a disposable fresh world or wholly untouched terrain, load the restoration with demonstrable priority above Matcha and generate new village starts.
3. Confirm new starts use the matching normal vanilla style-specific composition without data-reload, jigsaw, or worldgen errors. Do not use existing generated village blocks as evidence.
4. Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if the restoration does not win above Matcha, a new start uses the beta or wrong style, or relevant errors occur.

The retained Matcha 80/50 candidate lattice does not guarantee identical realized village starts at eligible-biome boundaries because the vanilla town-center anchor can change the jigsaw biome check.

Accepted-stack promotion remains pending a managed global data-pack authority that can prove both activation and precedence over Matcha. The current accepted-stack manager is MOD-only, so the reported pass does not establish a live accepted-stack deployment.
