package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.yungnickyoung.minecraft.ribbits.module.RibbitTradeModule;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * A narrow, contribution-owned normal Ribbit offer.  A non-null selection key denotes one
 * persistent mutually-exclusive choice shared by all entries with the same owner/key pair.
 */
public record RibbitExternalTradeOffer(
        String id,
        String profession,
        int tier,
        RibbitTradeModule.CostSpec first,
        RibbitTradeModule.CostSpec second,
        RibbitTradeModule.StackRef result,
        int resultCount,
        int maxUses,
        int merchantXp,
        String selectionKey,
        int selectionOptions,
        int selectionOption
) {
    public RibbitExternalTradeOffer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(profession, "profession");
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(result, "result");
        if (id.isBlank() || profession.isBlank() || tier < 1 || resultCount < 1 || maxUses < 1
                || merchantXp < 0 || merchantXp > 4) {
            throw new IllegalArgumentException("Invalid external Ribbit trade offer " + id);
        }
        if (selectionKey == null) {
            if (selectionOptions != 0 || selectionOption != 0) {
                throw new IllegalArgumentException("Unselected external offer has choice metadata");
            }
        } else if (selectionKey.isBlank() || selectionOptions < 2
                || selectionOption < 0 || selectionOption >= selectionOptions) {
            throw new IllegalArgumentException("Invalid external choice metadata for " + id);
        }
    }

    public RibbitTradeModule.TradeOfferSpec asTemplate(Identifier owner) {
        return new RibbitTradeModule.TradeOfferSpec(owner + "/" + id, profession, tier, first, second,
                result, resultCount, maxUses, merchantXp, "always", 0, RibbitTradeModule.Gate.NONE);
    }
}
