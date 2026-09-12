package com.ap.messages.data.preferences

import android.content.Context
import com.ap.messages.utils.ContactPresentationResolver

class DraftPreferences(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveDraft(threadId: Long, address: String?, text: String) {
        val trimmed = text.trim()
        val editor = preferences.edit()
        if (trimmed.isEmpty()) {
            if (threadId > 0L) editor.remove(KEY_THREAD_PREFIX + threadId)
            if (!address.isNullOrBlank()) editor.remove(KEY_ADDR_PREFIX + normalize(address))
        } else {
            if (threadId > 0L) editor.putString(KEY_THREAD_PREFIX + threadId, trimmed)
            if (!address.isNullOrBlank()) editor.putString(KEY_ADDR_PREFIX + normalize(address), trimmed)
        }
        editor.apply()
    }

    fun getDraft(threadId: Long, address: String? = null): String? {
        if (threadId > 0L) {
            val draft = preferences.getString(KEY_THREAD_PREFIX + threadId, null)
            if (!draft.isNullOrBlank()) return draft
        }
        if (!address.isNullOrBlank()) {
            val draft = preferences.getString(KEY_ADDR_PREFIX + normalize(address), null)
            if (!draft.isNullOrBlank()) return draft
        }
        return null
    }

    fun clearDraft(threadId: Long, address: String? = null) {
        val editor = preferences.edit()
        if (threadId > 0L) editor.remove(KEY_THREAD_PREFIX + threadId)
        if (!address.isNullOrBlank()) editor.remove(KEY_ADDR_PREFIX + normalize(address))
        editor.apply()
    }

    fun getAllThreadDrafts(): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        preferences.all.forEach { (key, value) ->
            if (key.startsWith(KEY_THREAD_PREFIX) && value is String && value.isNotBlank()) {
                val threadId = key.removePrefix(KEY_THREAD_PREFIX).toLongOrNull()
                if (threadId != null && threadId > 0L) {
                    result[threadId] = value
                }
            }
        }
        return result
    }

    fun getAllAddressDrafts(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        preferences.all.forEach { (key, value) ->
            if (key.startsWith(KEY_ADDR_PREFIX) && value is String && value.isNotBlank()) {
                val addrKey = key.removePrefix(KEY_ADDR_PREFIX)
                if (addrKey.isNotBlank()) {
                    result[addrKey] = value
                }
            }
        }
        return result
    }

    fun normalize(address: String): String =
        ContactPresentationResolver.cacheKey(address)

    private companion object {
        const val PREFS_NAME = "draft_preferences"
        const val KEY_THREAD_PREFIX = "thread_"
        const val KEY_ADDR_PREFIX = "addr_"
    }
}
