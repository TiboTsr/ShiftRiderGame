package com.example.jeubateau

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class GameOverActivity : AppCompatActivity() {

    private val database = FirebaseDatabase.getInstance("https://jeushiftraider-default-rtdb.europe-west1.firebasedatabase.app/").getReference("Classement")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_over)

        val tvScoreFinal = findViewById<TextView>(R.id.tv_score_final)
        val tvCoinsGagnes = findViewById<TextView>(R.id.tv_coins_gagnes)
        val btnRejouer = findViewById<Button>(R.id.btn_rejouer)
        val btnAccueil = findViewById<Button>(R.id.btn_accueil)

        val score = intent.getIntExtra("SCORE_FINAL", 0)
        tvScoreFinal.text = score.toString()

        val piecesGagnees = score / 10
        tvCoinsGagnes.text = "Pièces gagnées : +$piecesGagnees"

        sauvegarderScoreFirebase(score)

        chargerClassementFirebase()

        btnRejouer.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnAccueil.setOnClickListener {
            finish()
        }
    }

    // --- FONCTION POUR ENVOYER LE SCORE ---
    private fun sauvegarderScoreFirebase(scoreActuel: Int) {
        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
        val monBadge = prefs.getString("MON_ID_SECRET", "ID_INCONNU") ?: "ID_INCONNU"
        val monVraiPseudo = prefs.getString("MON_PSEUDO", "Joueur Inconnu") ?: "Joueur Inconnu"
        val meilleurScoreLocal = prefs.getInt("HIGH_SCORE", 0)

        if (scoreActuel >= meilleurScoreLocal && monBadge != "ID_INCONNU") {
            prefs.edit().putInt("HIGH_SCORE", scoreActuel).apply()

            val infosScore = mapOf(
                "nom" to monVraiPseudo,
                "score" to scoreActuel
            )

            database.child(monBadge).setValue(infosScore)
        }
    }

    // --- FONCTION POUR AFFICHER LE CLASSEMENT ---
    private fun chargerClassementFirebase() {
        val layoutLignes = findViewById<android.widget.LinearLayout>(R.id.layout_lignes)
        layoutLignes.removeAllViews()
        val pbLoading = findViewById<android.widget.ProgressBar>(R.id.pb_loading)

        layoutLignes.removeAllViews()

        database.orderByChild("score").limitToLast(100).get().addOnSuccessListener { snapshot ->

            pbLoading.visibility = android.view.View.GONE

            val joueurs = mutableListOf<Pair<String, String>>()
            for (donnee in snapshot.children) {
                val nom = donnee.child("nom").value.toString()
                val scoreFirebase = donnee.child("score").value.toString()
                joueurs.add(Pair(nom, scoreFirebase))
            }

            joueurs.reverse()

            for ((index, joueur) in joueurs.withIndex()) {

                val ligne = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, 16) }
                    background = getDrawable(R.drawable.bg_row)
                    setPadding(32, 32, 32, 32)
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val tvRang = TextView(this).apply {
                    text = "#${index + 1}"
                    setTextColor(android.graphics.Color.parseColor("#A0A5D0"))
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    layoutParams = android.widget.LinearLayout.LayoutParams(100, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                }

                val tvNom = TextView(this).apply {
                    text = joueur.first
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f // 1f = Prend toute la place au milieu
                    )
                }

                val tvScore = TextView(this).apply {
                    text = joueur.second
                    setTextColor(android.graphics.Color.parseColor("#FFAA00")) // Orange !
                    textSize = 18f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }

                ligne.addView(tvRang)
                ligne.addView(tvNom)
                ligne.addView(tvScore)

                layoutLignes.addView(ligne)
            }

        }.addOnFailureListener {
            pbLoading.visibility = android.view.View.GONE

            val tvErreur = TextView(this).apply {
                text = "Impossible de charger le classement. Réessayez plus tard !"
                setTextColor(android.graphics.Color.WHITE)
            }
            layoutLignes.addView(tvErreur)
        }
    }
}