package dev.resivore.ribbitsxaeroicons;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Proxy;
class SuccessorDiagnosticsTest {
    @Test void manuallyTestedGeckoVersionDeclinesBeforeTheRibbitsMismatch() {
        var badGecko=new CompatibilityActivation.DependencyIdentity("geckolib","5.5.4",1183876L,
                "a5770f9ea0c21db157559fe266874fd84be8c7da689d37aa7bf2b06304a6a65d");
        var decision=CompatibilityActivation.evaluate(CompatibilityActivation.SUPPORTED_XAERO,
                CompatibilityActivation.SUPPORTED_XAEROLIB,badGecko,CompatibilityActivation.SUPPORTED_RIBBITS);
        assertFalse(decision.active());assertTrue(decision.reason().startsWith("GeckoLib exact binary mismatch"));
    }
    @Test void emptyDestinationIsDistinctFromSubmittedVertices() {
        int[] calls={0};
        VertexConsumer delegate=(VertexConsumer)Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{VertexConsumer.class},(proxy,method,args)->{calls[0]++;return proxy;});
        var counted=new CountingVertexConsumer(delegate);
        assertEquals(0,counted.vertices());
        counted.addVertex(1,2,3).setColor(0xFFFFFFFF).setUv(0,1);
        assertEquals(1,counted.vertices());assertEquals(3,calls[0]);
    }
}
