package dev.resivore.slotreservations.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TooltipHitStateTest {
    @Test void staleFrameTooltipHostScreenAndPointerCannotReuseHit() {
        Object tooltip=new Object(),host=new Object(),screen=new Object();
        var hit=new TooltipHitState(12,tooltip,host,screen,100,200);
        assertTrue(hit.matches(12,tooltip,host,screen,100,200));
        assertFalse(hit.matches(13,tooltip,host,screen,100,200));
        assertFalse(hit.matches(12,new Object(),host,screen,100,200));
        assertFalse(hit.matches(12,tooltip,new Object(),screen,100,200));
        assertFalse(hit.matches(12,tooltip,host,new Object(),100,200));
        assertFalse(hit.matches(12,tooltip,host,screen,100.01,200));
        assertFalse(hit.matches(12,tooltip,host,screen,100,200.01));
    }
}
