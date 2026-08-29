package dev.resivore.dragonbound.channel;

enum ChannelCancellation {
    ALREADY_CHANNELING("message.dragonbound_waystone.already_channeling"),
    NO_WAYSTONE("message.dragonbound_waystone.no_waystone"),
    MOUNTED("message.dragonbound_waystone.blocked_mounted"),
    FALLING("message.dragonbound_waystone.blocked_falling"),
    LAVA("message.dragonbound_waystone.blocked_lava"),
    ELYTRA("message.dragonbound_waystone.blocked_elytra"),
    CROSS_DIMENSION_DISABLED("message.dragonbound_waystone.cross_dimension_disabled"),
    STAFF_COOLDOWN("message.dragonbound_waystone.staff_cooldown"),
    MOVEMENT("message.dragonbound_waystone.cancelled_movement"),
    DAMAGE("message.dragonbound_waystone.cancelled_damage"),
    DEATH("message.dragonbound_waystone.cancelled_death"),
    DISCONNECT(null),
    DIMENSION_CHANGED("message.dragonbound_waystone.cancelled_dimension"),
    HELD_ITEM("message.dragonbound_waystone.cancelled_item"),
    ANCHOR_CHANGED("message.dragonbound_waystone.cancelled_anchor"),
    DESTINATION_UNAVAILABLE("message.dragonbound_waystone.destination_unavailable"),
    DESTINATION_OBSTRUCTED("message.dragonbound_waystone.destination_obstructed"),
    TELEPORT_FAILED("message.dragonbound_waystone.teleport_failed"),
    SERVER_STOPPING(null);

    private final String translationKey;

    ChannelCancellation(String translationKey) {
        this.translationKey = translationKey;
    }

    String translationKey() {
        return translationKey;
    }
}
