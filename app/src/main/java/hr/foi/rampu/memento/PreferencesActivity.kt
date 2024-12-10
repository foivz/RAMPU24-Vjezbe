package hr.foi.rampu.memento

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.messaging.FirebaseMessaging
import hr.foi.rampu.memento.fragments.MementoSettingsFragment

const val RESULT_LANG_CHANGED = AppCompatActivity.RESULT_FIRST_USER
class PreferencesActivity : AppCompatActivity(),
        SharedPreferences.OnSharedPreferenceChangeListener
{
    private val possibleSubscriptions = listOf("RAMPU", "RWA", "RPP")

    companion object {
        fun switchDarkMode(isDarkModeSelected: Boolean?) {
            if (isDarkModeSelected == true) {
                AppCompatDelegate
                    .setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate
                    .setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_preferences)

        val preferences = androidx.preference.PreferenceManager
            .getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container_settings, MementoSettingsFragment())
            .commit()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?,
                                           key: String?) {
        when (key) {
            "preference_dark_mode" -> switchDarkMode(sharedPreferences?.getBoolean(key, false))
            "preference_language" -> notifyLanguageChangedAndClose()
            "preference_subscribed_categories" -> manageFCMSubscriptions(
                sharedPreferences?.getStringSet(key, setOf())!!
            )
        }
    }

    private fun notifyLanguageChangedAndClose() {
        setResult(RESULT_LANG_CHANGED)
        finish()
    }

    private fun manageFCMSubscriptions(selectedTopics: Set<String>) {
        possibleSubscriptions.forEach { availableTopic ->
            if (selectedTopics.contains(availableTopic)) {
                Log.i("MEMENTO_TOPICS", "Subscribing to $availableTopic")
                FirebaseMessaging.getInstance().subscribeToTopic(availableTopic)
            } else {
                Log.i("MEMENTO_TOPICS", "Unsubscribing from $availableTopic")
                FirebaseMessaging.getInstance().unsubscribeFromTopic(availableTopic)
            }
        }
    }


}