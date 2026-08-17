package com.shl.meditation.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("preferences")

private val IMPORTED_TOTAL = intPreferencesKey("imported_total_seconds")

/**
 * Time meditated before this app existed, or before the data was lost.
 *
 * There is no sync and no backup, so this is the whole recovery story: the
 * total is a number the user can simply set. Sessions recorded here are added
 * on top of it.
 */
class Preferences(private val context: Context) {

    val importedTotalSeconds: Flow<Int> =
        context.dataStore.data.map { it[IMPORTED_TOTAL] ?: 0 }

    suspend fun setImportedTotalSeconds(seconds: Int) {
        context.dataStore.edit { it[IMPORTED_TOTAL] = seconds.coerceAtLeast(0) }
    }
}
