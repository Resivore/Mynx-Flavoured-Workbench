package com.crispytwig.naturalist.server.entity.util;

import org.jetbrains.annotations.Nullable;

public interface MultipartLevel {
    void naturalist$addMobPart(MobPart part);

    void naturalist$removeMobPart(MobPart part);

    @Nullable
    MobPart naturalist$getMobPart(int id);
}
