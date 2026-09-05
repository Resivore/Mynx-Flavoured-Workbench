package dev.resivore.slotreservations.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TooltipGridTest {
    @Test void everyCellAndBoundaryAtEdgesAndGuiScales() {
        for (int scale : new int[]{1,2,3}) for (int x : new int[]{0, 7, 1920/scale-169})
            for (int y : new int[]{0, 7, 1080/scale-61}) {
                var grid = new TooltipGrid(x,y);
                for(int slot=0;slot<27;slot++) {
                    double left=x+(slot%9)*18, top=y+(slot/9)*18;
                    assertEquals(slot,grid.slot(left,top)); assertEquals(slot,grid.slot(left+17.999,top+17.999));
                    assertEquals(slot,grid.slot(left+9,top+9));
                }
                assertEquals(-1,grid.slot(x-0.001,y)); assertEquals(-1,grid.slot(x,y-0.001));
                assertEquals(-1,grid.slot(x+162,y)); assertEquals(-1,grid.slot(x,y+54));
            }
    }
    @Test void repositionDoesNotReuseOldGeometry() {
        var old = new TooltipGrid(0,0); var moved = new TooltipGrid(200,200);
        assertEquals(0,old.slot(1,1)); assertEquals(-1,moved.slot(1,1)); assertEquals(26,moved.slot(361,253));
    }
}
