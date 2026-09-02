package com.yungnickyoung.minecraft.ribbits.entity;

import com.yungnickyoung.minecraft.ribbits.data.RibbitProfession;
import com.yungnickyoung.minecraft.ribbits.module.RibbitProfessionModule;

final class RibbitPickResult {
    private RibbitPickResult() {
    }

    static Egg eggForProfession(RibbitProfession profession) {
        if (profession == RibbitProfessionModule.FISHERMAN) {
            return Egg.FISHERMAN;
        }
        if (profession == RibbitProfessionModule.GARDENER) {
            return Egg.GARDENER;
        }
        if (profession == RibbitProfessionModule.MERCHANT) {
            return Egg.MERCHANT;
        }
        if (profession == RibbitProfessionModule.SORCERER) {
            return Egg.SORCERER;
        }
        if (profession == RibbitProfessionModule.CHEF) {
            return Egg.CHEF;
        }
        if (profession == RibbitProfessionModule.FARMER) {
            return Egg.FARMER;
        }
        if (profession == RibbitProfessionModule.PROSPECTOR) {
            return Egg.PROSPECTOR;
        }
        if (profession == RibbitProfessionModule.GUARD) {
            return Egg.GUARD;
        }
        return Egg.NITWIT;
    }

    enum Egg {
        NITWIT,
        FISHERMAN,
        GARDENER,
        MERCHANT,
        SORCERER,
        CHEF,
        FARMER,
        PROSPECTOR,
        GUARD
    }
}
