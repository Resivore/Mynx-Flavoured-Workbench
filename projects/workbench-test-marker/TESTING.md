# Testing

## Marker 0.2.0 focused gate

Test only exact `workbench-test-marker-0.2.0.jar`, SHA-256
`9147664302721976461DBBC1064260FC4309575182A8B050458D0446EAFEC6A6`.

1. Before launching Minecraft, require Test Instance Manager physical
   verification to report the title projection synchronized with canonical
   runtime state.
2. On the title screen, confirm the overlay shows exactly the manager-produced
   `Baseline: Stack vN`, `Slot A: ...`, and `Slot B: ...` lines for the current
   state.
3. Confirm an empty slot reads `Empty`, no normal line contains `UNKNOWN`, and
   retired V1 metadata cannot replace the current projection.

Build/static checks and manager verification are not Minecraft runtime results.
