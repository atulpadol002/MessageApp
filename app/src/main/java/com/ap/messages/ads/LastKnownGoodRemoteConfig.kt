package com.ap.messages.ads

internal enum class AdRemoteConfigSource {
    ACTIVATED_EXISTING,
    FETCHED_NEW,
    FALLBACK_ALL_OFF
}

internal data class AdRemoteConfigDecision<T>(
    val effective: T,
    val source: AdRemoteConfigSource,
    val hasValidConfiguration: Boolean
)

/**
 * Keeps a validated Remote Config value active when a later fetch is unavailable or invalid.
 * The fallback is used only until the first valid Remote Config value is accepted.
 */
internal class LastKnownGoodRemoteConfig<T>(
    private val fallback: T
) {
    private var effective = fallback
    private var source = AdRemoteConfigSource.FALLBACK_ALL_OFF
    private var hasValidConfiguration = false

    fun apply(
        candidate: T?,
        candidateSource: AdRemoteConfigSource
    ): AdRemoteConfigDecision<T> {
        require(candidateSource != AdRemoteConfigSource.FALLBACK_ALL_OFF)
        if (candidate != null) {
            effective = candidate
            source = candidateSource
            hasValidConfiguration = true
        }
        return current()
    }

    fun retainOrFallback(): AdRemoteConfigDecision<T> = current()

    private fun current() = AdRemoteConfigDecision(
        effective = if (hasValidConfiguration) effective else fallback,
        source = if (hasValidConfiguration) source else AdRemoteConfigSource.FALLBACK_ALL_OFF,
        hasValidConfiguration = hasValidConfiguration
    )
}
