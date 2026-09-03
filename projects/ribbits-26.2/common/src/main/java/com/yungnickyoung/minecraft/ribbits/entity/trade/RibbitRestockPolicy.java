package com.yungnickyoung.minecraft.ribbits.entity.trade;

/** Pure policy seam for the persistent three-stock-batches-per-day contract. */
public final class RibbitRestockPolicy {
    public static final int MAX_ADDITIONAL_RESTOCKS = 2;
    public static final long MIN_RESTOCK_SPACING = 2400L;

    private RibbitRestockPolicy() {
    }

    public static boolean mayOrdinarilyRestock(boolean exhausted, boolean dayChangeDeferred,
                                               int restocksUsed, long lastRestockGameTime,
                                               long currentGameTime) {
        if (!exhausted || dayChangeDeferred || restocksUsed < 0
                || restocksUsed >= MAX_ADDITIONAL_RESTOCKS) {
            return false;
        }
        if (restocksUsed == 0) {
            return true;
        }
        return currentGameTime >= lastRestockGameTime
                && currentGameTime - lastRestockGameTime >= MIN_RESTOCK_SPACING;
    }

    /** A backward or repeated day never grants fresh stock. */
    public static boolean beginsFreshDay(long persistedDay, long observedDay) {
        return persistedDay != RibbitTradeState.UNSET_DAY && observedDay > persistedDay;
    }
}
