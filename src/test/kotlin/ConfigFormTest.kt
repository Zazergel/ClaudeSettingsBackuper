package ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigFormTest {

    @Test
    fun envIsTheFirstFieldOfUiMode() {
        // требование: переменные окружения — самый верх UI-режима
        assertEquals(listOf("env"), formFields.first().path)
    }

    @Test
    fun secretEnvKeysAreDetected() {
        assertTrue(isSecretEnvKey("ANTHROPIC_AUTH_TOKEN"))
        assertFalse(isSecretEnvKey("ANTHROPIC_BASE_URL"))
        assertFalse(isSecretEnvKey("SOME_TOKEN"))
    }
}
