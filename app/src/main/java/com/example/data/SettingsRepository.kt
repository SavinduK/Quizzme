package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.GeminiModelOption
import com.example.model.QuestionStyle
import com.example.model.QuizSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quizzme_prefs")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val QUESTION_COUNT = intPreferencesKey("question_count")
        val QUESTION_STYLE = stringPreferencesKey("question_style")
        val CUSTOM_PROMPT = stringPreferencesKey("custom_prompt")
        val INITIAL_TASKS_SEEDED = booleanPreferencesKey("initial_tasks_seeded")
    }

    val settingsFlow: Flow<QuizSettings> = context.dataStore.data.map { prefs ->
        QuizSettings(
            geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: "",
            selectedModel = GeminiModelOption.fromIdOrName(prefs[Keys.GEMINI_MODEL]),
            questionCount = prefs[Keys.QUESTION_COUNT] ?: 5,
            questionStyle = QuestionStyle.fromString(prefs[Keys.QUESTION_STYLE]),
            customPrompt = prefs[Keys.CUSTOM_PROMPT] ?: ""
        )
    }

    suspend fun getSettingsOnce(): QuizSettings = settingsFlow.first()

    suspend fun saveSettings(settings: QuizSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GEMINI_API_KEY] = settings.geminiApiKey.trim()
            prefs[Keys.GEMINI_MODEL] = settings.selectedModel.name
            prefs[Keys.QUESTION_COUNT] = settings.questionCount
            prefs[Keys.QUESTION_STYLE] = settings.questionStyle.name
            prefs[Keys.CUSTOM_PROMPT] = settings.customPrompt.trim()
        }
    }

    suspend fun isInitialTasksSeeded(): Boolean {
        return context.dataStore.data.map { it[Keys.INITIAL_TASKS_SEEDED] ?: false }.first()
    }

    suspend fun markInitialTasksSeeded() {
        context.dataStore.edit { prefs ->
            prefs[Keys.INITIAL_TASKS_SEEDED] = true
        }
    }
}
