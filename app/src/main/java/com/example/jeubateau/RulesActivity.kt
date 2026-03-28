package com.example.jeubateau

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class RulesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // On bloque l'écran en mode portrait comme pour le reste du jeu
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        setContentView(R.layout.activity_rules)

        // Bouton de retour
        findViewById<Button>(R.id.btn_retour_accueil).setOnClickListener {
            finish() // Ferme cette page et retourne automatiquement au menu principal
        }
    }
}