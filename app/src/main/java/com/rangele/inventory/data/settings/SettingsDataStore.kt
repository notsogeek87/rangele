package com.rangele.inventory.data.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore by preferencesDataStore(name = "rangele_settings")
