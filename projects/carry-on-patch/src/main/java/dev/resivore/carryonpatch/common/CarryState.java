package dev.resivore.carryonpatch.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.*;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Per-player preservation only. The upstream synchronized STRING remains the wire format. */
public final class CarryState {
    public static final String DATA = "GrabAndGo_CarriedData";
    public static final String FLAG = "GrabAndGo_IsCarrying";
    public static final String LOCK = "CarryOnPatch_RetryBlocked";
    public static final String WARNED = "CarryOnPatch_Warned";
    private Tag original;
    private boolean originalFlag;
    private boolean malformed;
    private boolean uncertain;
    private boolean inFlight;
    private String warned;

    public CompoundTag load(ValueInput input) {
        original = input.read(DATA, Codec.PASSTHROUGH)
                .map(v -> v.convert(NbtOps.INSTANCE).getValue().copy()).orElse(null);
        originalFlag = input.getBooleanOr(FLAG, false);
        uncertain = input.getBooleanOr(LOCK, false);
        warned = input.getStringOr(WARNED, "");
        malformed = false;
        CompoundTag data = null;
        try {
            if (original instanceof CompoundTag compound) data = compound.copy();
            else if (original instanceof StringTag string) {
                String value = string.asString().orElseThrow();
                data = value.isEmpty() ? new CompoundTag() : TagParser.parseCompoundFully(value);
            } else if (original == null) data = new CompoundTag();
        } catch (Exception ignored) { /* The original tag is retained below, never replaced. */ }
        malformed = data == null || (data.isEmpty() && originalFlag)
                || (data != null && !data.isEmpty() && !validShape(data));
        return malformed ? new CompoundTag() : data;
    }

    public static boolean validShape(CompoundTag data) {
        return switch (data.getStringOr("Type", "")) {
            case "entity" -> data.getString("EntityTypeId").filter(s -> !s.isEmpty()).isPresent()
                    && data.get("EntityData") instanceof CompoundTag entity && !entity.isEmpty();
            case "block" -> data.getString("BlockId").filter(s -> !s.isEmpty()).isPresent()
                    && data.get("BlockState") instanceof CompoundTag
                    && data.get("BlockEntityData") instanceof CompoundTag;
            default -> false;
        };
    }

    public void save(ValueOutput output, CompoundTag live, boolean carrying) {
        if (malformed) {
            if (original != null) output.store(DATA, Codec.PASSTHROUGH,
                    new Dynamic<>(NbtOps.INSTANCE, original.copy()));
            else output.discard(DATA);
            output.putBoolean(FLAG, originalFlag);
        } else {
            // Always a COMPOUND, including the legitimately empty state; no selected-field copy.
            output.store(DATA, CompoundTag.CODEC, live.copy());
            output.putBoolean(FLAG, carrying || !live.isEmpty());
        }
        if (uncertain || inFlight) output.putBoolean(LOCK, true); else output.discard(LOCK);
        if (warned != null && !warned.isEmpty()) output.putString(WARNED, warned); else output.discard(WARNED);
    }

    public boolean protectedState() { return malformed || uncertain || inFlight; }
    public boolean needsWarning() { return malformed || uncertain; }
    public boolean markWarned(String reason) { if (reason.equals(warned)) return false; warned = reason; return true; }
    public boolean begin() { if (protectedState()) return false; inFlight = true; return true; }
    public void retryable() { inFlight = false; }
    public void uncertain() { uncertain = true; inFlight = false; }
    public void consumed() {
        original = null; originalFlag = false; malformed = false;
        uncertain = false; inFlight = false; warned = null;
    }
}
