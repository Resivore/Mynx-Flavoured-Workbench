package dev.resivore.mynxregions;

import static org.junit.jupiter.api.Assertions.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShaderMaterialAliasesTest {
    @Test void explicitMappingsWinAndAbsentCounterpartsStayUnmapped() {
        var ids = new Object2IntOpenHashMap<String>();
        ids.put("vanilla", 10005); ids.put("explicit", 77);
        ShaderMaterialAliases.inheritUnmapped(ids, List.of("new", "explicit"), value -> "vanilla");
        assertEquals(10005, ids.getInt("new"));
        assertEquals(77, ids.getInt("explicit"));
        ShaderMaterialAliases.inheritUnmapped(ids, List.of("unknown"), value -> "missing");
        assertFalse(ids.containsKey("unknown"));
    }
}
