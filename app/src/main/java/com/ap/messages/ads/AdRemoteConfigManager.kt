package com.ap.messages.ads

import android.content.Context
import com.ap.messages.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdRemoteConfigManager {
    private const val MASTER_KEY = "ads_master_enabled"
    private const val CONFIG_KEY = "ads_config"
    private const val AUTO_INTERSTITIAL_CONFIG_KEY = "auto_interstitial_config"
    private const val AD_TYPE_CONFIG_KEY = "ad_type_config"
    private const val RELEASE_FETCH_INTERVAL_SECONDS = 12 * 60 * 60L
    private const val DEBUG_FETCH_INTERVAL_SECONDS = 0L

    private val _config = MutableStateFlow(AdConfig.AllOff)
    val config: StateFlow<AdConfig> = _config.asStateFlow()
    private val _autoInterstitialConfig = MutableStateFlow(AutoInterstitialConfig.Off)
    val autoInterstitialConfig: StateFlow<AutoInterstitialConfig> =
        _autoInterstitialConfig.asStateFlow()
    private val _adTypeConfig = MutableStateFlow(AdTypeConfig.CurrentBehaviorFallback)
    val adTypeConfig: StateFlow<AdTypeConfig> = _adTypeConfig.asStateFlow()
    private val lastKnownGood = LastKnownGoodRemoteConfig(EffectiveRemoteConfig.AllOff)
    private var fetchStarted = false
    private var appContext: Context? = null

    @Synchronized
    fun fetch(context: Context) {
        if (fetchStarted) {
            AdDebug.log { "Remote Config fetch skipped: already started in this process" }
            return
        }
        fetchStarted = true
        appContext = context.applicationContext

        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(
                if (BuildConfig.DEBUG) DEBUG_FETCH_INTERVAL_SECONDS else RELEASE_FETCH_INTERVAL_SECONDS
            )
            .build()
        val defaultsTask = remoteConfig.setDefaultsAsync(
            mapOf(
                MASTER_KEY to false,
                CONFIG_KEY to "{}",
                AUTO_INTERSTITIAL_CONFIG_KEY to "{}",
                AD_TYPE_CONFIG_KEY to "{}"
            )
        )

        AdDebug.log {
            "Remote Config fetch start: minimumIntervalSeconds=" +
                (if (BuildConfig.DEBUG) DEBUG_FETCH_INTERVAL_SECONDS else RELEASE_FETCH_INTERVAL_SECONDS)
        }

        val settingsTask = remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.ensureInitialized().addOnCompleteListener { initializationTask ->
            AdDebug.log {
                "Remote Config initialization result: success=${initializationTask.isSuccessful}, " +
                    "defaultsSuccess=${defaultsTask.isSuccessful}"
            }

            // Firebase persists activated values. Validate and publish them before any network fetch
            // so a normal process restart can immediately reuse the last activated configuration.
            applyRemoteValues(remoteConfig, AdRemoteConfigSource.ACTIVATED_EXISTING)

            settingsTask.addOnCompleteListener { completedSettingsTask ->
                if (!completedSettingsTask.isSuccessful) {
                    AdDebug.log {
                        "Remote Config settings failed: ${completedSettingsTask.exception?.message}"
                    }
                    retainLastKnownGood()
                    return@addOnCompleteListener
                }
                fetchAndActivate(remoteConfig)
            }
        }
    }

    private fun fetchAndActivate(remoteConfig: FirebaseRemoteConfig) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            AdDebug.log {
                "Remote Config fetch result: success=${task.isSuccessful}, " +
                    "lastFetchStatus=${remoteConfig.info.lastFetchStatus}, " +
                    "fetchTimeMillis=${remoteConfig.info.fetchTimeMillis}, " +
                    "error=${task.exception?.message}"
            }
            AdDebug.log {
                "Remote Config activate result: success=${task.isSuccessful}, " +
                    "changed=${if (task.isSuccessful) task.result else null}"
            }
            when {
                !task.isSuccessful -> retainLastKnownGood()
                task.result == true -> {
                    applyRemoteValues(remoteConfig, AdRemoteConfigSource.FETCHED_NEW)
                }
                !lastKnownGood.retainOrFallback().hasValidConfiguration -> {
                    // Covers initialization races while still failing closed if Firebase has no
                    // valid activated configuration.
                    applyRemoteValues(remoteConfig, AdRemoteConfigSource.ACTIVATED_EXISTING)
                }
                else -> retainLastKnownGood()
            }
        }
    }

    private fun applyRemoteValues(
        remoteConfig: FirebaseRemoteConfig,
        candidateSource: AdRemoteConfigSource
    ) {
        val masterValue = remoteConfig.getValue(MASTER_KEY)
        val configValue = remoteConfig.getValue(CONFIG_KEY)
        val autoInterstitialValue = remoteConfig.getValue(AUTO_INTERSTITIAL_CONFIG_KEY)
        val adTypeValue = remoteConfig.getValue(AD_TYPE_CONFIG_KEY)
        val rawMaster = masterValue.asString()
        val rawJson = configValue.asString()
        val rawAutoInterstitialJson = autoInterstitialValue.asString()
        val rawAdTypeJson = adTypeValue.asString()
        AdDebug.log {
            "ads_master_enabled raw value=$rawMaster, boolean=${masterValue.asBoolean()}, " +
                "source=${masterValue.source}"
        }
        AdDebug.log { "ads_config raw JSON=$rawJson, source=${configValue.source}" }
        AdDebug.log {
            "auto_interstitial_config raw JSON=$rawAutoInterstitialJson, " +
                "source=${autoInterstitialValue.source}"
        }
        AdDebug.log {
            "ad_type_config raw JSON=$rawAdTypeJson, source=${adTypeValue.source}"
        }
        val parsedConfig = AdConfig.parse(masterValue.asBoolean(), rawJson)
        val parsedAutoInterstitial = AutoInterstitialConfig.parse(rawAutoInterstitialJson)
        val parsedAdTypes = AdTypeConfig.parseValidated(rawAdTypeJson)
        val candidate = if (
            parsedConfig != null && parsedAutoInterstitial != null && parsedAdTypes != null
        ) {
            EffectiveRemoteConfig(parsedConfig, parsedAutoInterstitial, parsedAdTypes)
        } else {
            null
        }
        AdDebug.log { "parsed AdConfig=$parsedConfig" }
        publishDecision(
            lastKnownGood.apply(candidate, candidateSource),
            parseSuccessful = candidate != null
        )
    }

    private fun retainLastKnownGood() {
        val decision = lastKnownGood.retainOrFallback()
        publishDecision(
            decision,
            parseSuccessful = decision.hasValidConfiguration
        )
    }

    private fun publishDecision(
        decision: AdRemoteConfigDecision<EffectiveRemoteConfig>,
        parseSuccessful: Boolean
    ) {
        val effective = decision.effective
        _config.value = effective.config
        _autoInterstitialConfig.value = effective.autoInterstitialConfig
        _adTypeConfig.value = effective.adTypeConfig
        AutoInterstitialManager.onConfigUpdated(_autoInterstitialConfig.value)
        AdRuntimeReleaseLog.remoteConfig(
            source = decision.source,
            parseSuccessful = parseSuccessful,
            masterEnabled = _config.value.masterEnabled
        )
        appContext?.let { context ->
            AdRuntime.preloadConfiguredAds(context, "remote_config_activated")
        }
        logEffectiveConfig()
    }

    private fun logEffectiveConfig() {
        AdDebug.log { "final effective master enabled=${_config.value.masterEnabled}" }
        AdDebug.log { "final homeBanner.enabled=${_config.value.homeBanner.enabled}" }
        AdDebug.log {
            "final archiveNative.position=${_config.value.archiveNative.position.remoteValue}"
        }
        AdDebug.log {
            "final scheduleBanner.enabled=${_config.value.scheduleBanner.enabled} " +
                "adType=${_adTypeConfig.value[AdTypePlacement.SCHEDULED].remoteValue}"
        }
        AdDebug.log {
            "final serviceChatNative.enabled=${_config.value.serviceChatNative.enabled} " +
                "adType=${_adTypeConfig.value[AdTypePlacement.SERVICE_CHAT].remoteValue}"
        }
        AdDebug.log {
            "final auto interstitial enabled=" +
                _autoInterstitialConfig.value.enabled
        }
    }

    private data class EffectiveRemoteConfig(
        val config: AdConfig,
        val autoInterstitialConfig: AutoInterstitialConfig,
        val adTypeConfig: AdTypeConfig
    ) {
        companion object {
            val AllOff = EffectiveRemoteConfig(
                config = AdConfig.AllOff,
                autoInterstitialConfig = AutoInterstitialConfig.Off,
                adTypeConfig = AdTypeConfig.CurrentBehaviorFallback
            )
        }
    }
}
