package com.yungnickyoung.minecraft.ribbits.chute;

/** Owner-visible result of one physical airborne jump request. */
public enum ChuteAckState {
    REJECTED(0),
    PENDING(1),
    DEPLOYED(2);

    private final byte networkId;

    ChuteAckState(int networkId) {
        this.networkId = (byte) networkId;
    }

    public byte networkId() {
        return this.networkId;
    }

    public static ChuteAckState fromNetworkId(byte id) {
        return switch (id) {
            case 0 -> REJECTED;
            case 1 -> PENDING;
            case 2 -> DEPLOYED;
            default -> throw new IllegalArgumentException("Unknown Chute acknowledgement state: " + id);
        };
    }
}
