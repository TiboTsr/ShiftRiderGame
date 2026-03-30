package com.example.jeubateau

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 * RulesActivity affiche les instructions de jeu et les informations sur les récompenses.
 */
class RulesActivity : AppCompatActivity() {

    private val TAG = "RulesActivity"

    /**
     * Initialisation de l'écran des règles.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verrouillage de l'orientation pour éviter les problèmes de mise en page
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        setContentView(R.layout.activity_rules)

        // Initialisation du bouton de retour
        val btnRetourAccueil = findViewById<Button>(R.id.btn_retour_accueil)
        
        // Action de retour au menu principal
        btnRetourAccueil.setOnClickListener {
            Log.d(TAG, "Retour au menu principal depuis les règles")
            // Ferme cette activité pour revenir à l'activité précédente (MainActivity)
            finish()
        }
    }
}