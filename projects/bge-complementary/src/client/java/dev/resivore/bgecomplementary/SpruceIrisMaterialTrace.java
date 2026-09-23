package dev.resivore.bgecomplementary;

import dev.aero.cnmterraincompat.BgeMaterialBindings;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * C98's bounded, authoritative account of Iris's completed spruce maps.
 *
 * <p>Targets are selected only through {@link BgeMaterialBindings}: the canonical native block
 * plus bindings whose canonical material is native spruce. This never derives an identity from a
 * BGE registry name and never changes either map.</p>
 */
final class SpruceIrisMaterialTrace {
    private static final Logger LOGGER = LoggerFactory.getLogger("BGE/SpruceIrisMaterialTrace");

    private SpruceIrisMaterialTrace() {}

    static MaterialSnapshot beforeMaterialInheritance(Object2IntMap<BlockState> materialIds) {
        return new MaterialSnapshot(targets(), materialIds);
    }

    static void afterMaterialInheritance(Object2IntMap<BlockState> materialIds,
            MaterialSnapshot before) {
        for (Target target : before.targets()) {
            TreeMap<String, Integer> outcomes = new TreeMap<>();
            TreeMap<String, Integer> canonicalValues = new TreeMap<>();
            TreeMap<String, Integer> beforeValues = new TreeMap<>();
            TreeMap<String, Integer> afterValues = new TreeMap<>();
            for (BlockState physical : target.block().getStateDefinition().getPossibleStates()) {
                Optional<BlockState> canonical = target.canonicalState(physical);
                SnapshotValue parent = canonical.map(before::material).orElse(SnapshotValue.missing());
                SnapshotValue physicalBefore = before.material(physical);
                SnapshotValue physicalAfter = value(materialIds, physical);
                count(canonicalValues, parent.describe());
                count(beforeValues, physicalBefore.describe());
                count(afterValues, physicalAfter.describe());
                String outcome = physicalBefore.present() ? "explicit_physical"
                        : !canonical.isPresent() ? "missing_projection"
                        : !parent.present() ? "missing_canonical"
                        : physicalAfter.present() && physicalAfter.intValue() == parent.intValue()
                                ? "inherited_canonical" : "unexpected";
                count(outcomes, outcome);
            }
            LOGGER.info("BGE_SPRUCE_IRIS_MATERIAL_TRACE|block={}|familyRole={}|canonicalParent={}"
                            + "|states={}|canonicalMaterialIds={}|physicalBefore={}|physicalAfter={}"
                            + "|outcomes={}",
                    id(target.block()), target.role(), id(target.canonicalMaterial()),
                    target.block().getStateDefinition().getPossibleStates().size(), canonicalValues,
                    beforeValues, afterValues, outcomes);
        }
    }

    static LayerSnapshot beforeLayerInheritance(Map<Block, ?> layerTypes) {
        return new LayerSnapshot(targets(), layerTypes);
    }

    static void afterLayerInheritance(Map<Block, ?> layerTypes, LayerSnapshot before) {
        for (Target target : before.targets()) {
            SnapshotValue canonical = before.layer(target.canonicalMaterial());
            SnapshotValue physicalBefore = before.layer(target.block());
            SnapshotValue physicalAfter = value(layerTypes, target.block());
            String outcome = physicalBefore.present() ? "explicit_physical"
                    : !canonical.present() ? "missing_canonical"
                    : physicalAfter.present() && physicalAfter.describe().equals(canonical.describe())
                            ? "inherited_canonical" : "unexpected";
            LOGGER.info("BGE_SPRUCE_IRIS_LAYER_TRACE|block={}|familyRole={}|canonicalParent={}"
                            + "|canonicalLayer={}|physicalBefore={}|physicalAfter={}|outcome={}",
                    id(target.block()), target.role(), id(target.canonicalMaterial()), canonical.describe(),
                    physicalBefore.describe(), physicalAfter.describe(), outcome);
        }
    }

    private static List<Target> targets() {
        List<Target> result = new ArrayList<>();
        result.add(new Target(Blocks.SPRUCE_LEAVES, Blocks.SPRUCE_LEAVES, Optional.empty()));
        for (Binding binding : BgeMaterialBindings.all()) {
            if (binding.canonicalMaterial() == Blocks.SPRUCE_LEAVES
                    && binding.physicalBlock() != Blocks.SPRUCE_LEAVES) {
                result.add(new Target(binding.physicalBlock(), binding.canonicalMaterial(),
                        Optional.of(binding)));
            }
        }
        return List.copyOf(result);
    }

    private static String id(Block block) {
        return String.valueOf(BuiltInRegistries.BLOCK.getKey(block));
    }

    private static void count(Map<String, Integer> counts, String value) {
        counts.merge(value, 1, Integer::sum);
    }

    private static SnapshotValue value(Object2IntMap<BlockState> map, BlockState state) {
        return map.containsKey(state) ? SnapshotValue.present(map.getInt(state)) : SnapshotValue.missing();
    }

    private static SnapshotValue value(Map<Block, ?> map, Block block) {
        return map.containsKey(block) ? SnapshotValue.present(map.get(block)) : SnapshotValue.missing();
    }

    record MaterialSnapshot(List<Target> targets, Object2IntMap<BlockState> materialIds) {
        SnapshotValue material(BlockState state) {
            return value(materialIds, state);
        }
    }

    record LayerSnapshot(List<Target> targets, Map<Block, ?> layerTypes) {
        SnapshotValue layer(Block block) {
            return value(layerTypes, block);
        }
    }

    record Target(Block block, Block canonicalMaterial, Optional<Binding> binding) {
        String role() {
            return block == Blocks.SPRUCE_LEAVES ? "canonical" : "derived";
        }

        Optional<BlockState> canonicalState(BlockState state) {
            return binding.flatMap(value -> value.canonicalState(state))
                    .or(() -> Optional.of(state));
        }
    }

    record SnapshotValue(boolean present, Object value) {
        static SnapshotValue present(Object value) {
            return new SnapshotValue(true, value);
        }

        static SnapshotValue missing() {
            return new SnapshotValue(false, null);
        }

        int intValue() {
            return (Integer) value;
        }

        String describe() {
            return present ? String.valueOf(value) : "missing";
        }
    }
}
