package com.example.jeubateau

import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)

        val switchMusique = findViewById<Switch>(R.id.switch_musique)
        val switchEffets = findViewById<Switch>(R.id.switch_effets)
        val switchVibration = findViewById<Switch>(R.id.switch_vibration)
        val switchFps = findViewById<Switch>(R.id.switch_fps)
        val btnRetour = findViewById<Button>(R.id.btn_retour_settings)
        val btnReset = findViewById<Button>(R.id.btn_reset_progress)

        switchMusique.isChecked = prefs.getBoolean("SETTING_MUSIC", true)
        switchEffets.isChecked = prefs.getBoolean("SETTING_SFX", true)
        switchVibration.isChecked = prefs.getBoolean("SETTING_VIBRATION", true)
        switchFps.isChecked = prefs.getBoolean("SETTING_60FPS", true)

        switchMusique.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("SETTING_MUSIC", isChecked).apply()
        }
        switchEffets.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("SETTING_SFX", isChecked).apply()
        }
        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("SETTING_VIBRATION", isChecked).apply()
        }
        switchFps.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("SETTING_60FPS", isChecked).apply()
        }

        btnRetour.setOnClickListener {
            finish()
        }

        btnReset.setOnClickListener {
            val monId = prefs.getString("MON_ID_SECRET", "")
            val monPseudo = prefs.getString("PLAYER_PSEUDO", "")

            prefs.edit().clear()
                .putString("MON_ID_SECRET", monId)
                .putString("PLAYER_PSEUDO", monPseudo)
                .putBoolean("SETTING_MUSIC", true)
                .putBoolean("SETTING_SFX", true)
                .putBoolean("SETTING_VIBRATION", true)
                .putBoolean("SETTING_60FPS", true)
                .apply()

            switchMusique.isChecked = true
            switchEffets.isChecked = true
            switchVibration.isChecked = true
            switchFps.isChecked = true

            Toast.makeText(this, "Scores et pièces remis à zéro !", Toast.LENGTH_SHORT).show()
        }
    }
}