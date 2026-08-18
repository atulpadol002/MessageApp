package com.ap.messages.ads

import android.util.Log
import com.google.android.gms.ads.LoadAdError
import java.util.concurrent.ConcurrentHashMap

internal object AdRuntimeReleaseLog {
    const val TAG = "AdRuntimeRelease"

    private val placementReasons = ConcurrentHashMap<String, String>()
    private var lastPremiumAdsAllowed: Boolean? = null

    fun remoteConfig(
        source: AdRemoteConfigSource,
        parseSuccessful: Boolean,
        masterEnabled: Boolean
    ) {
        Log.i(
            TAG,
            "RemoteConfig source=$source parseSuccess=$parseSuccessful " +
                "masterEnabled=$masterEnabled"
        )
    }

    fun ump(canRequestAds: Boolean) {
        Log.i(TAG, "UMP canRequestAds=$canRequestAds")
    }

    @Synchronized
    fun premium(adsAllowed: Boolean) {
        if (lastPremiumAdsAllowed != adsAllowed) {
            lastPremiumAdsAllowed = adsAllowed
            Log.i(TAG, "Premium adsAllowed=$adsAllowed")
        }
    }

    fun mobileAdsInitializationStarted(canRequestAds: Boolean) {
        Log.i(TAG, "MobileAds initialization=start canRequestAds=$canRequestAds")
    }

    fun testDeviceConfigured(configured: Boolean) {
        Log.i(TAG, "testDeviceConfigured=$configured")
    }

    fun mobileAdsInitializationCompleted() {
        Log.i(TAG, "MobileAds initialization=complete")
    }

    fun placementBlocked(placement: String, reason: String) {
        if (placementReasons.put(placement, reason) != reason) {
            Log.i(TAG, "Placement placement=$placement blockedReason=$reason")
        }
    }

    fun placementReady(placement: String) {
        placementReasons.remove(placement)
    }

    fun adRequest(format: String, source: AdLoadSource) {
        Log.i(TAG, "AdRequest format=$format source=$source")
    }

    fun loadError(format: String, source: AdLoadSource, error: LoadAdError) {
        val message = error.message.replace('\n', ' ').replace('\r', ' ')
        Log.w(
            TAG,
            "AdLoadError format=$format source=$source code=${error.code} " +
                "domain=${error.domain} message=$message"
        )
    }
}
