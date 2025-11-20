package com.ecs160;
import static org.junit.Assert.*;

import org.junit.Test;

public class AppTest 
{
    @Test
    public void testFrameworkScansAndInvokesEndpoints() throws Exception {
        MicroserviceFramework framework = new MicroserviceFramework();

        MockService service = new MockService();
        framework.scanMicroservice(service);

        assertTrue(framework.hasEndpoint("/greeting"));

        String res1 = framework.invoke("/greeting", "World");
        assertEquals("Hello World", res1);

        assertTrue(framework.hasEndpoint("/echo"));

        String res2 = framework.invoke("/echo", "Hi");
        assertEquals("Hi", res2);
    }
}
