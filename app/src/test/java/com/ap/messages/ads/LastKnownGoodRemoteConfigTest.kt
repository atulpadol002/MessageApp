package com.ap.messages.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LastKnownGoodRemoteConfigTest {

    @Test
    fun noActivatedConfigAndFetchFailureUsesAllOff() {
        val state = LastKnownGoodRemoteConfig("ALL_OFF")

        val decision = state.retainOrFallback()

        assertEquals("ALL_OFF", decision.effective)
        assertEquals(AdRemoteConfigSource.FALLBACK_ALL_OFF, decision.source)
        assertFalse(decision.hasValidConfiguration)
    }

    @Test
    fun validActivatedConfigAndCachedFetchRetainsExisting() {
        val state = LastKnownGoodRemoteConfig("ALL_OFF")
        state.apply("ACTIVATED", AdRemoteConfigSource.ACTIVATED_EXISTING)

        val decision = state.retainOrFallback()

        assertEquals("ACTIVATED", decision.effective)
        assertEquals(AdRemoteConfigSource.ACTIVATED_EXISTING, decision.source)
        assertTrue(decision.hasValidConfiguration)
    }

    @Test
    fun validActivatedConfigAndNetworkFailureRetainsExisting() {
        val state = LastKnownGoodRemoteConfig("ALL_OFF")
        state.apply("ACTIVATED", AdRemoteConfigSource.ACTIVATED_EXISTING)

        val decision = state.retainOrFallback()

        assertEquals("ACTIVATED", decision.effective)
        assertTrue(decision.hasValidConfiguration)
    }

    @Test
    fun validNewFetchReplacesExistingConfig() {
        val state = LastKnownGoodRemoteConfig("ALL_OFF")
        state.apply("ACTIVATED", AdRemoteConfigSource.ACTIVATED_EXISTING)

        val decision = state.apply("FETCHED", AdRemoteConfigSource.FETCHED_NEW)

        assertEquals("FETCHED", decision.effective)
        assertEquals(AdRemoteConfigSource.FETCHED_NEW, decision.source)
    }

    @Test
    fun malformedNewFetchPreservesLastKnownGoodConfig() {
        val state = LastKnownGoodRemoteConfig("ALL_OFF")
        state.apply("ACTIVATED", AdRemoteConfigSource.ACTIVATED_EXISTING)

        val decision = state.apply(null, AdRemoteConfigSource.FETCHED_NEW)

        assertEquals("ACTIVATED", decision.effective)
        assertEquals(AdRemoteConfigSource.ACTIVATED_EXISTING, decision.source)
        assertTrue(decision.hasValidConfiguration)
    }

    @Test
    fun freshInstallAcceptsFirstValidFetch() {
        val state = LastKnownGoodRemoteConfig("ALL_OFF")

        val decision = state.apply("FETCHED", AdRemoteConfigSource.FETCHED_NEW)

        assertEquals("FETCHED", decision.effective)
        assertEquals(AdRemoteConfigSource.FETCHED_NEW, decision.source)
        assertTrue(decision.hasValidConfiguration)
    }

    @Test
    fun processRestartInsideFetchIntervalReusesActivatedConfig() {
        val firstProcess = LastKnownGoodRemoteConfig("ALL_OFF")
        firstProcess.apply("FETCHED", AdRemoteConfigSource.FETCHED_NEW)

        val restartedProcess = LastKnownGoodRemoteConfig("ALL_OFF")
        val decision = restartedProcess.apply(
            "FETCHED",
            AdRemoteConfigSource.ACTIVATED_EXISTING
        )

        assertEquals("FETCHED", decision.effective)
        assertEquals(AdRemoteConfigSource.ACTIVATED_EXISTING, decision.source)
        assertTrue(decision.hasValidConfiguration)
    }
}
