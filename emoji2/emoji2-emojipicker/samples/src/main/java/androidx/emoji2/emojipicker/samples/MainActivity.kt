/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.emoji2.emojipicker.samples

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.emoji2.emojipicker.RecentEmojiProvider

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        val view = findViewById<EmojiPickerView>(R.id.emoji_picker)
        view.setOnEmojiPickedListener {
            findViewById<EditText>(R.id.edit_text).append(it.emoji)
        }
        view.setRecentEmojiProvider(CustomRecentEmojiProvider(applicationContext))

        findViewById<Button>(R.id.button).setOnClickListener {
            view.emojiGridColumns = 8
            view.emojiGridRows = 8.3f
        }
    }
}

/**
 * Define a custom recent emoji provider which shows most frequently used emoji
 */
internal class CustomRecentEmojiProvider(
    context: Context
) : RecentEmojiProvider {

    companion object {
        private const val PREF_KEY_CUSTOM_EMOJI_FREQ = "pref_key_custom_emoji_freq"
        private const val RECENT_EMOJI_LIST_FILE_NAME =
            "androidx.emoji2.emojipicker.sample.preferences"
        private const val SPLIT_CHAR = ","
        private const val KEY_VALUE_DELIMITER = "="
    }

    private val sharedPreferences =
        context.getSharedPreferences(RECENT_EMOJI_LIST_FILE_NAME, Context.MODE_PRIVATE)

    private val emoji2Frequency: MutableMap<String, Int> by lazy {
        sharedPreferences.getString(PREF_KEY_CUSTOM_EMOJI_FREQ, null)?.split(SPLIT_CHAR)
            ?.associate { entry ->
                entry.split(KEY_VALUE_DELIMITER, limit = 2).takeIf { it.size == 2 }
                    ?.let { it[0] to it[1].toInt() } ?: ("" to 0)
            }?.toMutableMap() ?: mutableMapOf()
    }

    override suspend fun getRecentEmojiList() =
        emoji2Frequency.toList().sortedByDescending { it.second }.map { it.first }

    override fun recordSelection(emoji: String) {
        emoji2Frequency[emoji] = (emoji2Frequency[emoji] ?: 0) + 1
        saveToPreferences()
    }

    private fun saveToPreferences() {
        sharedPreferences
            .edit()
            .putString(PREF_KEY_CUSTOM_EMOJI_FREQ, emoji2Frequency.entries.joinToString(SPLIT_CHAR))
            .commit()
    }
}
