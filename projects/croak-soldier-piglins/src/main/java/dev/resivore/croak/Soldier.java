package dev.resivore.croak;

import com.geckolib.animatable.GeoReplacedEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.*;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

final class Soldier implements GeoReplacedEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation AGRO = RawAnimation.begin().thenLoop("agro");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Soldier>("movement", 4, test -> {
            var state = (SoldierRenderer.State)test.renderState();
            if (state.dying) return test.setAndContinue(DEATH);
            if (state.sprinting || state.aggressive && test.isMoving()) return test.setAndContinue(AGRO);
            if (!state.aggressive && (test.isMoving() || state.walkAnimationSpeed >= 0.15f))
                return test.setAndContinue(WALK);
            return test.setAndContinue(IDLE);
        }));
        controllers.add(new AnimationController<Soldier>("attacking", 4, test -> {
            var state = (SoldierRenderer.State)test.renderState();
            if (state.dying) return PlayState.STOP;
            if (test.isCurrentAnimation(ATTACK) && !test.controller().hasAnimationFinished())
                return PlayState.CONTINUE;
            if (state.meleeSwing) return test.setAndContinue(ATTACK);
            test.controller().reset();
            return PlayState.STOP;
        }));
    }
}
