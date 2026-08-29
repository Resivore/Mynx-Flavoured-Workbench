# Testing

C3 is the current candidate and remains `NOT_DEPLOYED` and `RUNTIME_UNTESTED`. Run this matrix only during a controlled handoff of the exact retained C3 JAR; the migration itself performed no Minecraft runtime checks.

## C3 runtime handoff

1. Move an unmounted ordinary Horse through a mature sweet berry bush; confirm the Horse is neither slowed nor damaged.
2. Ride an ordinary Horse through a mature sweet berry bush; confirm neither the direct Player rider nor the Horse is slowed or damaged.
3. Walk and ride an ordinary Horse onto powdered snow and let it stand there; confirm the Horse enters normally and receives the intended solid-from-above surface collision.
4. Cross and remain on powdered snow; confirm the Horse stays on top and does not sink.
5. Keep the unmounted Horse on powdered snow for more than 140 ticks; confirm it does not freeze, accumulate frost as a hazard, or take freezing damage.
6. Repeat while mounted for more than 140 ticks; confirm the current direct Player rider remains carried normally and does not freeze or take freezing damage.
7. Check an unmounted ordinary Player in powdered snow; confirm vanilla sinking, freezing, and eventual damage remain unchanged.
8. Check an unrelated entity in powdered snow; confirm its vanilla behavior remains unchanged.
9. Dismount into the sweet berry bush and powdered snow cases; confirm Player protection ends appropriately after dismount while the ordinary Horse remains protected.
10. Traverse normal snow mounted and unmounted; confirm no regression to normal-snow behavior.

Stop and record the result as failed or inconclusive if the game crashes, the exact Horse sinks or freezes, protection persists for a dismounted Player, an unrelated entity gains immunity, or normal-snow behavior changes. Do not promote C3 without a controlled pass of the applicable matrix.
