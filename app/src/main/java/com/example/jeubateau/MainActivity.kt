package com.example.jeubateau

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Récupération des vues
        val btnPlay = findViewById<Button>(R.id.btn_play)
        val btnGarage = findViewById<Button>(R.id.btn_garage)
        val etPseudo = findViewById<EditText>(R.id.et_pseudo)

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
    }


    private fun actualiserInfosAccueil() {
        val tvTopScore = findViewById<TextView>(R.id.tv_top_score)
        val tvCoins = findViewById<TextView>(R.id.tv_coins)
        val tvVehicle = findViewById<TextView>(R.id.tv_current_vehicle)
        val etPseudo = findViewById<EditText>(R.id.et_pseudo)

        val prefs = getSharedPreferences("GAME_PREFS", Context.MODE_PRIVATE)
        
        val highScore = prefs.getInt("HIGH_SCORE", 0)
        val coins = prefs.getInt("TOTAL_COINS", 0)
        val themeNom = prefs.getString("THEME_NOM", "ATHLÈTE")
        val difficulte = prefs.getString("DIFF_ACTUELLE", "TRÈS FACILE")
        val pseudo = prefs.getString("PLAYER_PSEUDO", "")

        tvTopScore.text = "🏆 Score : $highScore"
        tvCoins.text = "🪙 $coins"
        
        tvVehicle.text = "Véhicule : $themeNom ($difficulte)"

        etPseudo.setText(pseudo)
    }

    override fun onResume() {
        super.onResume()
        actualiserInfosAccueil()
    }
}