# Migration freeze

This is the immutable source record for reconstructing Mynx Flavoured Workbench without importing legacy Git history.

## Legacy authority

- Repository: `Resivore/Minecraft-26.2-Workbench`
- Branch: `codex/workbench-current`
- Commit: `f0101f38c446dddaa4226450386402c63597ba7c`

## Project-scoped overrides

| Project | Legacy directory | UUID | Branch | Exact commit |
|---|---|---|---|---|
| Nibaru 26.2 Port | `projects/nibaru-26.2` | `680476b9-5336-5422-a575-779f2efd1eff` | `codex/bge-layer-geometry` | `1b4c070d7ea5b0699f518f91d0bec35e9a0679a0` |
| Block Geometry Extensions | `projects/clutter-no-more-compat` | `4b2342fc-7bdf-5ba6-9f37-d551109d214c` | `codex/bge-layer-geometry` | `1b4c070d7ea5b0699f518f91d0bec35e9a0679a0` |
| Naturalist 26.2 Port | `projects/naturalist-26.2` | `c92ad4fe-c210-46c4-ba1d-59828d2bcbcd` | `codex/naturalist-26.2` | `63a8b46ac939d6bd48e21d4840c480361ad852ef` |
| Horse Immunities | `projects/sweet-berry-horse-immunity` | `2dc7b47a-f3b4-5fbe-a1fa-53e4f0aaa446` | `codex/horse-immunities-powder-snow` | `78bb33251b9fa8c2f9373be75c1bd23f7ca845b2` |

Override commits are import sources only for the named project paths. They are not whole-repository seeds.

The approved freeze audit concluded **READY TO INITIALIZE WORKBENCH V2** and found no meaningful remaining local or unpushed project work. The legacy repository remains the historical archive. Old commits, branches, tags, worktrees, merge history, deployment history, and snapshots are intentionally not imported.
