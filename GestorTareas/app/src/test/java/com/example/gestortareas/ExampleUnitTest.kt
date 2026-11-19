package com.example.gestortareas

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun validarTituloFunciona() {
        assertNotNull(errorTitulo(""))
        assertNotNull(errorTitulo("   "))
        assertNotNull(errorTitulo("ab"))
        assertNull(errorTitulo("Comprar pan"))
    }
}
