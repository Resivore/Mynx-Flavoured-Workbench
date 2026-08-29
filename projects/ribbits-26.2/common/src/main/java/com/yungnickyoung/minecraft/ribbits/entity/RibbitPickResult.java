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
        return Egg.NITWIT;
    }

    enum Egg {
        NITWIT,
        FISHERMAN,
        GARDENER,
        MERCHANT,
        SORCERER
    }
}
