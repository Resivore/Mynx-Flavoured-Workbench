package workbench.yungsapi.fixture;

import com.yungnickyoung.minecraft.yungsapi.api.autoregister.AutoRegister;
import com.yungnickyoung.minecraft.yungsapi.api.autoregister.AutoRegisterSoundEvent;
import com.yungnickyoung.minecraft.yungsapi.api.autoregister.AutoRegisterStructureProcessor;

@AutoRegister(YungsApiConsumerFixture.MOD_ID)
public final class FixtureRegistrants {
    @AutoRegister("fixture_sound")
    public static final AutoRegisterSoundEvent SOUND = AutoRegisterSoundEvent.create();

    @AutoRegister("replace_block")
    public static final AutoRegisterStructureProcessor PROCESSOR =
            AutoRegisterStructureProcessor.of(() -> ReplaceBlockProcessor.CODEC);

    private FixtureRegistrants() {
    }
}
