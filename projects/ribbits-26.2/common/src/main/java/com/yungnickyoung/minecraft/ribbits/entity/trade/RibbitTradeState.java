package com.yungnickyoung.minecraft.ribbits.entity.trade;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Persistent per-Ribbit economy state. Missing fields use conservative new-entity defaults. */
public final class RibbitTradeState {
    public static final long UNSET_DAY = Long.MIN_VALUE;

    private int rank = 1;
    private int xp;
    private int gardenerPair = -1;
    private int farmerTier2Choice = -1;
    private int farmerTier3Choice = -1;
    private int fishermanAquaticChoice = -1;
    private int fishermanCoralFamily = -1;
    private int chefMasterSpecialty = -1;
    private int sorcererBlessingChoice = -1;
    private int prospectorBullionChoice = -1;
    private int guardBranch = -1;
    private boolean fishermanOpalGate;
    private boolean sorcererBenzeneGate;
    private long chefMenuDay = UNSET_DAY;
    private final int[] chefMenus = {-1, -1, -1, -1};
    private long restockDay = UNSET_DAY;
    private int restocksUsedToday;
    private long lastRestockGameTime = Long.MIN_VALUE;
    private int tradeSchema;

    public void read(ValueInput input, boolean musician) {
        this.rank = input.getIntOr("MynxTradeRank", musician ? 0 : 1);
        this.xp = input.getIntOr("MynxTradeXp", 0);
        this.gardenerPair = input.getIntOr("MynxGardenerPair", -1);
        this.farmerTier2Choice = input.getIntOr("MynxFarmerTier2Choice", -1);
        this.farmerTier3Choice = input.getIntOr("MynxFarmerTier3Choice", -1);
        this.fishermanAquaticChoice = input.getIntOr("MynxFishermanAquaticChoice", -1);
        this.fishermanCoralFamily = input.getIntOr("MynxFishermanCoralFamily", -1);
        this.chefMasterSpecialty = input.getIntOr("MynxChefMasterSpecialty", -1);
        this.sorcererBlessingChoice = input.getIntOr("MynxSorcererBlessingChoice", -1);
        this.prospectorBullionChoice = input.getIntOr("MynxProspectorBullionChoice", -1);
        this.guardBranch = input.getIntOr("MynxGuardBranch", -1);
        this.fishermanOpalGate = input.getBooleanOr("MynxFishermanOpalGate", false);
        this.sorcererBenzeneGate = input.getBooleanOr("MynxSorcererBenzeneGate", false);
        this.chefMenuDay = input.getLongOr("MynxChefMenuDay", UNSET_DAY);
        for (int tier = 1; tier <= 4; tier++) {
            this.chefMenus[tier - 1] = input.getIntOr("MynxChefTier" + tier + "Menu", -1);
        }
        this.restockDay = input.getLongOr("MynxRestockDay", UNSET_DAY);
        this.restocksUsedToday = Math.max(0,
                Math.min(2, input.getIntOr("MynxRestocksUsedToday", 0)));
        this.lastRestockGameTime = input.getLongOr("MynxLastRestockGameTime", Long.MIN_VALUE);
        this.tradeSchema = input.getIntOr("MynxTradeSchema", 0);
    }

    public void write(ValueOutput output) {
        output.putInt("MynxTradeRank", this.rank);
        output.putInt("MynxTradeXp", this.xp);
        output.putInt("MynxGardenerPair", this.gardenerPair);
        output.putInt("MynxFarmerTier2Choice", this.farmerTier2Choice);
        output.putInt("MynxFarmerTier3Choice", this.farmerTier3Choice);
        output.putInt("MynxFishermanAquaticChoice", this.fishermanAquaticChoice);
        output.putInt("MynxFishermanCoralFamily", this.fishermanCoralFamily);
        output.putInt("MynxChefMasterSpecialty", this.chefMasterSpecialty);
        output.putInt("MynxSorcererBlessingChoice", this.sorcererBlessingChoice);
        output.putInt("MynxProspectorBullionChoice", this.prospectorBullionChoice);
        output.putInt("MynxGuardBranch", this.guardBranch);
        output.putBoolean("MynxFishermanOpalGate", this.fishermanOpalGate);
        output.putBoolean("MynxSorcererBenzeneGate", this.sorcererBenzeneGate);
        output.putLong("MynxChefMenuDay", this.chefMenuDay);
        for (int tier = 1; tier <= 4; tier++) {
            output.putInt("MynxChefTier" + tier + "Menu", this.chefMenus[tier - 1]);
        }
        output.putLong("MynxRestockDay", this.restockDay);
        output.putInt("MynxRestocksUsedToday", this.restocksUsedToday);
        output.putLong("MynxLastRestockGameTime", this.lastRestockGameTime);
        output.putInt("MynxTradeSchema", this.tradeSchema);
    }

    public int rank() { return this.rank; }
    public void rank(int value) { this.rank = value; }
    public int xp() { return this.xp; }
    public void xp(int value) { this.xp = Math.max(0, value); }
    public int gardenerPair() { return this.gardenerPair; }
    public void gardenerPair(int value) { this.gardenerPair = value; }
    public int farmerTier2Choice() { return this.farmerTier2Choice; }
    public void farmerTier2Choice(int value) { this.farmerTier2Choice = value; }
    public int farmerTier3Choice() { return this.farmerTier3Choice; }
    public void farmerTier3Choice(int value) { this.farmerTier3Choice = value; }
    public int fishermanAquaticChoice() { return this.fishermanAquaticChoice; }
    public void fishermanAquaticChoice(int value) { this.fishermanAquaticChoice = value; }
    public int fishermanCoralFamily() { return this.fishermanCoralFamily; }
    public void fishermanCoralFamily(int value) { this.fishermanCoralFamily = value; }
    public int chefMasterSpecialty() { return this.chefMasterSpecialty; }
    public void chefMasterSpecialty(int value) { this.chefMasterSpecialty = value; }
    public int sorcererBlessingChoice() { return this.sorcererBlessingChoice; }
    public void sorcererBlessingChoice(int value) { this.sorcererBlessingChoice = value; }
    public int prospectorBullionChoice() { return this.prospectorBullionChoice; }
    public void prospectorBullionChoice(int value) { this.prospectorBullionChoice = value; }
    public int guardBranch() { return this.guardBranch; }
    public void guardBranch(int value) { this.guardBranch = value; }
    public boolean fishermanOpalGate() { return this.fishermanOpalGate; }
    public void fishermanOpalGate(boolean value) { this.fishermanOpalGate = value; }
    public boolean sorcererBenzeneGate() { return this.sorcererBenzeneGate; }
    public void sorcererBenzeneGate(boolean value) { this.sorcererBenzeneGate = value; }
    public long chefMenuDay() { return this.chefMenuDay; }
    public void chefMenuDay(long value) { this.chefMenuDay = value; }
    public int chefMenu(int tier) { return this.chefMenus[tier - 1]; }
    public void chefMenu(int tier, int value) { this.chefMenus[tier - 1] = value; }
    public long restockDay() { return this.restockDay; }
    public void restockDay(long value) { this.restockDay = value; }
    public int restocksUsedToday() { return this.restocksUsedToday; }
    public void restocksUsedToday(int value) { this.restocksUsedToday = Math.max(0, Math.min(2, value)); }
    public long lastRestockGameTime() { return this.lastRestockGameTime; }
    public void lastRestockGameTime(long value) { this.lastRestockGameTime = value; }
    public int tradeSchema() { return this.tradeSchema; }
    public void tradeSchema(int value) { this.tradeSchema = Math.max(0, value); }
}
