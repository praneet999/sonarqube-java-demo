package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {

    @Test
    void testGetMessage() {

        App app = new App();

        assertEquals(
            "Hello from DevOps CI/CD Pipeline!",
            app.getMessage()
        );
    }
}
