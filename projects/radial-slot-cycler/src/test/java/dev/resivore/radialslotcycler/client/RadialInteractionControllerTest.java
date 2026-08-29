package dev.resivore.radialslotcycler.client;

import org.junit.jupiter.api.Test;

import static dev.resivore.radialslotcycler.client.RadialInteractionController.Action.CANCELLED;
import static dev.resivore.radialslotcycler.client.RadialInteractionController.Action.CLOSED_WITHOUT_MUTATION;
import static dev.resivore.radialslotcycler.client.RadialInteractionController.Action.CONFIRM;
import static dev.resivore.radialslotcycler.client.RadialInteractionController.Action.NONE;
import static dev.resivore.radialslotcycler.client.RadialInteractionController.Action.OPENED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadialInteractionControllerTest {
    @Test
    void holdReleaseConfirmsAStorageSelectionExactlyOnce() {
        var controller = new RadialInteractionController(RadialInteractionController.Mode.HOLD);

        assertEquals(NONE, controller.update(false, false, true, -1));
        assertEquals(OPENED, controller.update(true, true, true, -1));
        assertTrue(controller.isOpen());
        assertEquals(NONE, controller.update(true, false, true, 3));
        assertEquals(CONFIRM, controller.update(false, false, true, 3));
        assertFalse(controller.isOpen());
        assertEquals(NONE, controller.update(false, false, true, 3));
    }

    @Test
    void centerAndAnchorCloseWithoutMutation() {
        var center = new RadialInteractionController(RadialInteractionController.Mode.HOLD);
        center.update(true, true, true, -1);
        assertEquals(CANCELLED, center.update(false, false, true, -1));

        var anchor = new RadialInteractionController(RadialInteractionController.Mode.HOLD);
        anchor.update(true, true, true, -1);
        assertEquals(CLOSED_WITHOUT_MUTATION, anchor.update(false, false, true, 0));
    }

    @Test
    void guiSuppressionPreventsOpeningAndCancelsAnOpenWheel() {
        var controller = new RadialInteractionController(RadialInteractionController.Mode.HOLD);

        assertEquals(NONE, controller.update(true, true, false, -1));
        assertFalse(controller.isOpen());
        controller.update(false, false, true, -1);
        assertEquals(OPENED, controller.update(true, true, true, -1));
        assertEquals(CANCELLED, controller.update(true, false, false, 2));
        assertFalse(controller.isOpen());
    }

    @Test
    void toggleUsesOnePressToOpenAndTheNextToConfirmOrCancel() {
        var controller = new RadialInteractionController(RadialInteractionController.Mode.TOGGLE);

        assertEquals(OPENED, controller.update(true, true, true, -1));
        assertEquals(NONE, controller.update(false, false, true, 4));
        assertEquals(CONFIRM, controller.update(true, true, true, 4));
        assertEquals(NONE, controller.update(false, false, true, 4));

        assertEquals(OPENED, controller.update(true, true, true, -1));
        assertEquals(NONE, controller.update(false, false, true, -1));
        assertEquals(CANCELLED, controller.update(true, true, true, -1));
    }
}
