package com.example.jeubateau

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class GameOverActivity : AppCompatActivity() {

    private val db = FirebaseDatabase
        .getInstance("https://shiftrider-cce69-default-rtdb.europe-west1.firebasedatabase.app/")
        .getReference("Classement")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        setContentView(R.layout.activity_game_over)

        val score = intent.getIntExtra("SCORE_FINAL", 0)
        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)

        // On récupère le nom du véhicule !
        val themeNom = prefs.getString("THEME_NOM", "ATHLÈTE") ?: "ATHLÈTE"
        val themeKey = sanitizeKey(themeNom)

        // Pièces
        val pieces = score / 10
        prefs.edit().putInt("TOTAL_COINS", prefs.getInt("TOTAL_COINS", 0) + pieces).apply()

        // UI
        findViewById<TextView>(R.id.tv_score_final).text  = score.toString()
        findViewById<TextView>(R.id.tv_coins_gagnes).text = "+$pieces 🪙"

        findViewById<TextView>(R.id.tv_theme_used)?.text   = themeNom

        // On affiche le nom du véhicule dans le titre du classement !
        findViewById<TextView>(R.id.tv_titre_classement)?.text = "🏆 Top — $themeNom"

        val monId = prefs.getString("MON_ID_SECRET", "unknown") ?: "unknown"

        // On appelle les fonctions avec la clé du VÉHICULE (themeKey)
        sauvegarder(score, themeKey, themeNom, prefs)
        chargerClassementFirebase(themeKey, monId)

        findViewById<Button>(R.id.btn_rejouer).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.btn_accueil).setOnClickListener { finish() }
    }

    private fun sanitizeKey(nom: String): String {
        val map = mapOf(
            'É' to 'E', 'È' to 'E', 'Ê' to 'E', 'Ë' to 'E',
            'À' to 'A', 'Â' to 'A', 'Ä' to 'A',
            'Î' to 'I', 'Ï' to 'I',
            'Ô' to 'O', 'Ö' to 'O',
            'Û' to 'U', 'Ù' to 'U', 'Ü' to 'U',
            'Ç' to 'C', ' ' to '_'
        )
        return nom.uppercase()
            .map { map[it] ?: it }
            .joinToString("")
            .replace(Regex("[^A-Z0-9_]"), "_")
            .trim('_')
    }

    private fun sauvegarder(score: Int, themeKey: String, themeNom: String, prefs: android.content.SharedPreferences) {
        val monId   = prefs.getString("MON_ID_SECRET", "unknown") ?: "unknown"
        val pseudo  = prefs.getString("PLAYER_PSEUDO", "Joueur") ?: "Joueur"
        val bestKey = "BEST_$themeKey" // On sauvegarde le record par véhicule
        val best    = prefs.getInt(bestKey, 0)

        if (score > best) {
            prefs.edit().putInt(bestKey, score).apply()

            if (score > prefs.getInt("HIGH_SCORE", 0)) {
                prefs.edit().putInt("HIGH_SCORE", score).apply()
            }

            // On envoie à Firebase dans le dossier du véhicule
            db.child(themeKey).child(monId).setValue(
                mapOf("nom" to pseudo, "score" to score, "theme" to themeNom)
            ).addOnSuccessListener {
                android.util.Log.d("FB", "Score $score envoyé pour le véhicule $themeKey")
            }.addOnFailureListener { e ->
                android.util.Log.e("FB", "Erreur Firebase : ${e.message}")
            }
        }
    }

    private fun chargerClassementFirebase(themeKey: String, monId: String) {
        val layoutLignes = findViewById<LinearLayout>(R.id.layout_lignes)
        layoutLignes.removeAllViews()
        val pbLoading = findViewById<ProgressBar>(R.id.pb_loading)

        // On cherche dans le dossier du véhicule actuel
        db.child(themeKey).orderByChild("score").limitToLast(100).get().addOnSuccessListener { snapshot ->
            pbLoading.visibility = View.GONE

            if (!snapshot.exists()) {
                addEmptyRow(layoutLignes, "Aucun score pour ce véhicule.\nSoyez le premier !")
                return@addOnSuccessListener
            }

            data class JoueurScore(val id: String, val nom: String, val score: Int)
            val joueurs = mutableListOf<JoueurScore>()

            for (donnee in snapshot.children) {
                val idFirebase = donnee.key ?: ""
                val nom = donnee.child("nom").value?.toString() ?: "Anonyme"
                val scoreFirebase = donnee.child("score").value?.toString()?.toIntOrNull() ?: 0
                joueurs.add(JoueurScore(idFirebase, nom, scoreFirebase))
            }

            joueurs.sortByDescending { it.score }

            for ((index, joueur) in joueurs.withIndex()) {
                val isMe = (joueur.id == monId)
                addRow(layoutLignes, index, joueur.nom, joueur.score.toString(), isMe)
            }

        }.addOnFailureListener { e ->
            pbLoading.visibility = View.GONE
            addEmptyRow(layoutLignes, "Erreur : ${e.message}")
            android.util.Log.e("FIREBASE_ERREUR", "Erreur complète : ${e.message}")
        }
    }

    private fun addRow(parent: LinearLayout, rank: Int, nom: String, score: String, isMe: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))

            background = if (isMe) getDrawable(R.drawable.bg_row_hightlight) else getDrawable(R.drawable.bg_row)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(8)) }
        }

        val medal = when (rank) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "#${rank + 1}" }

        row.addView(makeText(medal, 15f, "#A0A5D0", true, dp(52)))
        row.addView(makeText(
            if (isMe) "$nom  ◀" else nom,
            15f,
            if (isMe) "#FFAA00" else "#FFFFFF",
            true, 0, weight = 1f
        ))
        row.addView(makeText(score, 17f, "#FFAA00", true))
        parent.addView(row)
    }

    private fun addEmptyRow(parent: LinearLayout, msg: String) {
        parent.addView(makeText(msg, 14f, "#A0A5D0", false).apply {
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, dp(24), 0, dp(8))
        })
    }

    private fun makeText(
        txt: String, size: Float, color: String, bold: Boolean,
        fixedWidth: Int = LinearLayout.LayoutParams.WRAP_CONTENT,
        weight: Float = 0f
    ) = TextView(this).apply {
        text = txt; textSize = size
        setTextColor(Color.parseColor(color))
        if (bold) setTypeface(null, Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(
            fixedWidth, LinearLayout.LayoutParams.WRAP_CONTENT, weight
        )
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}