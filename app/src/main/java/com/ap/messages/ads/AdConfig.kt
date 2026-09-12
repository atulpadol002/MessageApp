package com.ap.messages.ads

import org.json.JSONObject

data class ToggleConfig(val enabled: Boolean = false)
data class NativeFeedConfig(
    val enabled: Boolean = false,
    val everyItems: Int = 7,
    val maxPerSession: Int = 0
)
data class CappedConfig(val enabled: Boolean = false, val maxPerSession: Int = 0)
enum class AdPosition(val remoteValue: String) {
    TOP("top"),
    BOTTOM("bottom")
}
data class PositionedCappedConfig(
    val enabled: Boolean = false,
    val maxPerSession: Int = 0,
    val position: AdPosition = AdPosition.BOTTOM
)
data class InterstitialConfig(
    val enabled: Boolean = false,
    val frequency: Int = Int.MAX_VALUE,
    val minIntervalSeconds: Long = Long.MAX_VALUE,
    val maxPerSession: Int = 0
)
data class AppOpenConfig(
    val enabled: Boolean = false,
    val showAfterOnboarding: Boolean = false,
    val showOnResume: Boolean = true,
    val minIntervalSeconds: Long = Long.MAX_VALUE,
    val maxPerSession: Int = 0
)
data class RewardedConfig(
    val restoreEnabled: Boolean = false,
    val deleteForeverEnabled: Boolean = false,
    val maxPerSession: Int = 0
)

data class AdConfig(
    val masterEnabled: Boolean = false,
    val homeBanner: ToggleConfig = ToggleConfig(),
    val homeInlineNative: NativeFeedConfig = NativeFeedConfig(),
    val searchNative: CappedConfig = CappedConfig(),
    val archiveBanner: ToggleConfig = ToggleConfig(),
    val archiveNative: PositionedCappedConfig = PositionedCappedConfig(),
    val scheduleBanner: ToggleConfig = ToggleConfig(),
    val serviceChatNative: CappedConfig = CappedConfig(),
    val blockedBanner: ToggleConfig = ToggleConfig(),
    val starredBanner: ToggleConfig = ToggleConfig(),
    val interstitial: InterstitialConfig = InterstitialConfig(),
    val interstitialSplash: CappedConfig = CappedConfig(),
    val onboardingInterstitial: CappedConfig = CappedConfig(),
    val appOpen: AppOpenConfig = AppOpenConfig(),
    val rewarded: RewardedConfig = RewardedConfig(),
    val rateUsBanner: ToggleConfig = ToggleConfig(),
    val exitDialogBanner: ToggleConfig = ToggleConfig(),
    val chatBanner: ToggleConfig = ToggleConfig(),
    val chatNative: ToggleConfig = ToggleConfig(),
    val contactPickerBanner: ToggleConfig = ToggleConfig(),
    val settingsBanner: ToggleConfig = ToggleConfig(),
    val aboutBanner: ToggleConfig = ToggleConfig(),
    val permissionBanner: ToggleConfig = ToggleConfig(),
    val sessionMaxAds: Int = 0
) {
    companion object {
        val AllOff = AdConfig()

        fun parse(masterEnabled: Boolean, json: String): AdConfig? = runCatching {
            val root = JSONObject(json)
            val homeBanner = root.optionalToggle("homeBanner", default = true)
            val homeNative = root.optJSONObject("homeInlineNative")
            val archiveNative = root.optJSONObject("archiveNative")
            val interstitial = root.optJSONObject("interstitial")
            val onboarding = root.optJSONObject("onboardingInterstitial")
            val appOpen = root.optJSONObject("appOpen")
            val rewarded = root.optJSONObject("rewarded")
            val session = root.optJSONObject("session")

            AdConfig(
                masterEnabled = masterEnabled,
                homeBanner = homeBanner,
                homeInlineNative = NativeFeedConfig(
                    enabled = homeNative?.optBoolean("enabled", true) ?: true,
                    everyItems = homeNative?.optInt("everyItems", 7)?.coerceAtLeast(1) ?: 7,
                    maxPerSession = homeNative?.optInt("maxPerSession", 5)?.coerceAtLeast(0) ?: 5
                ),
                searchNative = root.optionalCapped("searchNative", defaultEnabled = true, defaultMax = 5),
                archiveBanner = root.optionalToggle("archiveBanner", default = true),
                archiveNative = PositionedCappedConfig(
                    archiveNative?.optBoolean("enabled", true) ?: true,
                    archiveNative?.optInt("maxPerSession", 5)?.coerceAtLeast(0) ?: 5,
                    archiveNative?.optionalPosition("position") ?: AdPosition.TOP
                ),
                scheduleBanner = root.optionalToggle("scheduleBanner", default = true),
                serviceChatNative = root.optionalCapped("serviceChatNative", defaultEnabled = true, defaultMax = 5),
                blockedBanner = root.optionalToggle("blockedBanner", default = true),
                starredBanner = root.optionalToggle("starredBanner", default = true),
                interstitial = InterstitialConfig(
                    enabled = interstitial?.optBoolean("enabled", true) ?: true,
                    frequency = interstitial?.optInt("frequency", 3)?.coerceAtLeast(1) ?: 3,
                    minIntervalSeconds = interstitial?.optLong("minIntervalSeconds", 30L)?.coerceAtLeast(0L) ?: 30L,
                    maxPerSession = interstitial?.optInt("maxPerSession", 5)?.coerceAtLeast(0) ?: 5
                ),
                interstitialSplash = root.optionalCapped("interstitialSplash", defaultEnabled = true, defaultMax = 1),
                onboardingInterstitial = CappedConfig(
                    onboarding?.optBoolean("enabled", true) ?: true,
                    onboarding?.optInt("maxPerSession", 1)?.coerceAtLeast(0) ?: 1
                ),
                appOpen = AppOpenConfig(
                    enabled = appOpen?.optBoolean("enabled", true) ?: true,
                    showAfterOnboarding = appOpen?.optBoolean("showAfterOnboarding", false) ?: false,
                    showOnResume = appOpen?.optBoolean("showOnResume", true) ?: true,
                    minIntervalSeconds = appOpen?.optLong("minIntervalSeconds", 30L)?.coerceAtLeast(0L) ?: 30L,
                    maxPerSession = appOpen?.optInt("maxPerSession", 5)?.coerceAtLeast(0) ?: 5
                ),
                rewarded = RewardedConfig(
                    restoreEnabled = rewarded?.optBoolean("restoreEnabled", true) ?: true,
                    deleteForeverEnabled = rewarded?.optBoolean("deleteForeverEnabled", true) ?: true,
                    maxPerSession = rewarded?.optInt("maxPerSession", 10)?.coerceAtLeast(0) ?: 10
                ),
                rateUsBanner = root.optionalToggle("rateUsBanner", default = false),
                exitDialogBanner = root.optionalToggle("exitDialogBanner", default = true),
                chatBanner = root.optionalToggle("chatBanner", default = true),
                chatNative = root.optionalToggle("chatNative", default = false),
                contactPickerBanner = root.optionalToggle("contactPickerBanner", default = true),
                settingsBanner = root.optionalToggle("settingsBanner", default = true),
                aboutBanner = root.optionalToggle("aboutBanner", default = true),
                permissionBanner = root.optionalToggle("permissionBanner", default = true),
                sessionMaxAds = session?.optInt("maxAds", 20)?.coerceAtLeast(0) ?: 20
            )
        }.onFailure { error ->
            AdDebug.log {
                "AdConfig parse rejected JSON: ${error.javaClass.simpleName}: ${error.message}"
            }
        }.getOrNull()
    }
}

private fun JSONObject.requiredToggle(name: String) =
    ToggleConfig(getJSONObject(name).getBoolean("enabled"))

private fun JSONObject.optionalToggle(name: String, default: Boolean = false): ToggleConfig =
    optJSONObject(name)?.let { ToggleConfig(it.optBoolean("enabled", default)) } ?: ToggleConfig(default)

private fun JSONObject.optionalCapped(name: String, defaultEnabled: Boolean = false, defaultMax: Int = 0): CappedConfig {
    val value = optJSONObject(name) ?: return CappedConfig(defaultEnabled, defaultMax)
    val maxPerSession = value.optInt("maxPerSession", defaultMax)
    return CappedConfig(
        enabled = value.optBoolean("enabled", defaultEnabled),
        maxPerSession = maxPerSession.takeIf { it >= 0 } ?: defaultMax
    )
}

private fun JSONObject.optionalPosition(name: String): AdPosition =
    when (optString(name, AdPosition.BOTTOM.remoteValue).lowercase()) {
        AdPosition.TOP.remoteValue -> AdPosition.TOP
        else -> AdPosition.BOTTOM
    }

private fun JSONObject.positiveInt(name: String): Int = getInt(name).also {
    require(it > 0) { "$name must be positive" }
}

private fun JSONObject.nonNegativeInt(name: String): Int = getInt(name).also {
    require(it >= 0) { "$name must not be negative" }
}

private fun JSONObject.nonNegativeLong(name: String): Long = getLong(name).also {
    require(it >= 0L) { "$name must not be negative" }
}
