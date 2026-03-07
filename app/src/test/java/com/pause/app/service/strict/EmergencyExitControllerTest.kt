package com.pause.app.service.strict

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyExitControllerTest {

    @Test
    fun tap1_returnsTapRegistered1() {
        val controller = EmergencyExitController()
        val result = controller.onEmergencyTapped()
        assertTrue(result is EmergencyTapResult.TapRegistered)
        assertEquals(1, (result as EmergencyTapResult.TapRegistered).count)
    }

    @Test
    fun tap2WithinWindow_returnsTapRegistered2() {
        val controller = EmergencyExitController()
        controller.onEmergencyTapped()
        val result = controller.onEmergencyTapped()
        assertTrue(result is EmergencyTapResult.TapRegistered)
        assertEquals(2, (result as EmergencyTapResult.TapRegistered).count)
    }

    @Test
    fun tap3WithinWindow_returnsShowConfirmation() {
        val controller = EmergencyExitController()
        controller.onEmergencyTapped()
        controller.onEmergencyTapped()
        val result = controller.onEmergencyTapped()
        assertTrue(result is EmergencyTapResult.ShowConfirmation)
    }

    @Test
    fun reset_clearsTapCount() {
        val controller = EmergencyExitController()
        controller.onEmergencyTapped()
        controller.reset()
        val result = controller.onEmergencyTapped()
        assertTrue(result is EmergencyTapResult.TapRegistered)
        assertEquals(1, (result as EmergencyTapResult.TapRegistered).count)
    }
}
