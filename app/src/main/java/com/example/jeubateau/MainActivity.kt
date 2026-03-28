package com.example.jeubateau

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)

        // Récupération des vues
        val btnPlay = findViewById<Button>(R.id.btn_play)
        val btnGarage = findViewById<Button>(R.id.btn_garage)
        val etPseudo = findViewById<EditText>(R.id.et_pseudo)
        val btnRules = findViewById<Button>(R.id.btn_rules)
        val btnSettings = findViewById<Button>(R.id.btn_settings)

        actualiserInfosAccueil()

        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)

        if (!prefs.contains("MON_ID_SECRET")) {
            val nouvelId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("MON_ID_SECRET", nouvelId).apply()
        }

        // Lancement du jeu
        btnPlay.setOnClickListener {
            val pseudo = etPseudo.text.toString()
            prefs.edit().putString("PLAYER_PSEUDO", pseudo).apply()

            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        // Ouverture du garage
        btnGarage.setOnClickListener {
            val intent = Intent(this, GarageActivity::class.java)
            startActivity(intent)
        }

        btnRules.setOnClickListener {
            startActivity(Intent(this, RulesActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }


    private fun actualiserInfosAccueil() {
        val tvTopScore = findViewById<TextView>(R.id.tv_top_score)
        val tvCoins = findViewById<TextView>(R.id.tv_coins)
        val tvVehicle = findViewById<TextView>(R.id.tv_current_vehicle)
        val ivMainVehicle = findViewById<ImageView>(R.id.iv_main_vehicle)
        val etPseudo = findViewById<EditText>(R.id.et_pseudo)

        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)

        val highScore = prefs.getInt("HIGH_SCORE", 0)
        val coins = prefs.getInt("TOTAL_COINS", 0)
        val themeNom = prefs.getString("THEME_NOM", "ATHLÈTE") ?: "ATHLÈTE"
        val difficulte = prefs.getString("DIFF_ACTUELLE", "TRÈS FACILE") ?: "TRÈS FACILE"
        val pseudo = prefs.getString("PLAYER_PSEUDO", "")

        // Mise à jour des textes
        tvTopScore.text = "🏆 Meilleur score: $highScore"
        tvCoins.text = "🪙 $coins"

        tvVehicle.text = "Sélection : $themeNom ($difficulte)"

        etPseudo.setText(pseudo)

        when (themeNom) {
            "ATHLÈTE" -> ivMainVehicle.setImageResource(R.drawable.coureur_image)
            "BMX PRO" -> ivMainVehicle.setImageResource(R.drawable.velo_image)
            "BOLIDE" -> ivMainVehicle.setImageResource(R.drawable.voiture_image)
            "FORMULE 1" -> ivMainVehicle.setImageResource(R.drawable.voituredecourse_image)
            "CORSAIRE" -> ivMainVehicle.setImageResource(R.drawable.bateau_image)
            "VOL LÉGER" -> ivMainVehicle.setImageResource(R.drawable.petitavion_image)
            "AIRLINER" -> ivMainVehicle.setImageResource(R.drawable.grandavion_image)
            "STAR JUMPER" -> ivMainVehicle.setImageResource(R.drawable.fusee_image)
            "COMÈTE" -> ivMainVehicle.setImageResource(R.drawable.comete_image)
            else -> ivMainVehicle.setImageResource(R.drawable.coureur_image)
        }
    }

    override fun onResume() {
        super.onResume()
        actualiserInfosAccueil()
    }
}