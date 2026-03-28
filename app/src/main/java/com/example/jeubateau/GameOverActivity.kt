package com.example.jeubateau

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class GameOverActivity : AppCompatActivity() {

    // Référence racine — les sous-nœuds seront : Classement/<themeKey>/<playerId>
    private val db = FirebaseDatabase
        .getInstance("https://shiftrider-cce69-default-rtdb.europe-west1.firebasedatabase.app/")
        .getReference("Classement")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        setContentView(R.layout.activity_game_over)

        val score = intent.getIntExtra("SCORE_FINAL", 0)
        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
        val themeNom = prefs.getString("THEME_NOM", "ATHLÈTE") ?: "ATHLÈTE"
        val themeKey = sanitizeKey(themeNom) // clé Firebase safe

        // Pièces
        val pieces = score / 10
        prefs.edit().putInt("TOTAL_COINS", prefs.getInt("TOTAL_COINS", 0) + pieces).apply()

        // UI
        findViewById<TextView>(R.id.tv_score_final).text  = score.toString()
        findViewById<TextView>(R.id.tv_coins_gagnes).text = "+$pieces 🪙"
        findViewById<TextView>(R.id.tv_theme_used).text   = themeNom
        findViewById<TextView>(R.id.tv_titre_classement).text = "🏆 Top — $themeNom"

        sauvegarder(score, themeKey, themeNom, prefs)
        chargerClassement(themeKey, prefs.getString("MON_ID_SECRET", "") ?: "")

        findViewById<Button>(R.id.btn_rejouer).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java)); finish()
        }
        findViewById<Button>(R.id.btn_accueil).setOnClickListener { finish() }
    }

    /**
     * Transforme un nom de thème en clé Firebase valide :
     * - pas d'accents, espaces → underscore, tout en minuscules
     * Firebase interdit : . # $ [ ] /
     */
    private fun sanitizeKey(nom: String): String {
        val map = mapOf(
            'É' to 'E', 'È' to 'E', 'Ê' to 'E', 'Ë' to 'E',
            'À' to 'A', 'Â' to 'A', 'Ä' to 'A',
            'Î' to 'I', 'Ï' to 'I',
            'Ô' to 'O', 'Ö' to 'O',
            'Û' to 'U', 'Ù' to 'U', 'Ü' to 'U',
            'Ç' to 'C'
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
        val bestKey = "BEST_$themeKey"
        val best    = prefs.getInt(bestKey, 0)

        if (score > best) {
            prefs.edit().putInt(bestKey, score).apply()
            if (score > prefs.getInt("HIGH_SCORE", 0)) prefs.edit().putInt("HIGH_SCORE", score).apply()

            db.child(themeKey).child(monId).setValue(
                mapOf("nom" to pseudo, "score" to score, "theme" to themeNom)
            ).addOnSuccessListener {
                android.util.Log.d("FB", "Score $score envoyé pour $themeNom")
            }.addOnFailureListener { e ->
                android.util.Log.e("FB", "Erreur Firebase : ${e.message}")
            }
        }
    }

    private fun chargerClassement(themeKey: String, monId: String) {
        val container = findViewById<android.widget.LinearLayout>(R.id.layout_lignes)
        val progress  = findViewById<android.widget.ProgressBar>(R.id.pb_loading)
        container.removeAllViews()

        db.child(themeKey).orderByChild("score").limitToLast(50).get()
            .addOnSuccessListener { snap ->
                progress.visibility = android.view.View.GONE

                data class Entry(val id: String, val nom: String, val score: Long)
                val entries = snap.children.map {
                    Entry(
                        it.key ?: "",
                        it.child("nom").value?.toString() ?: "?",
                        (it.child("score").value as? Long) ?: 0L
                    )
                }.sortedByDescending { it.score }

                if (entries.isEmpty()) {
                    addEmptyRow(container, "Aucun score pour ce véhicule.\nSoyez le premier !")
                    return@addOnSuccessListener
                }

                entries.forEachIndexed { i, e ->
                    addRow(container, i, e.nom, e.score.toString(), e.id == monId)
                }
            }
            .addOnFailureListener { err ->
                progress.visibility = android.view.View.GONE
                android.util.Log.e("FB", "Chargement échoué : ${err.message}")
                addEmptyRow(container, "Impossible de charger le classement.\nVérifiez votre connexion.")
            }
    }

    private fun addRow(parent: android.widget.LinearLayout, rank: Int, nom: String, score: String, isMe: Boolean) {
        val row = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = if (isMe) getDrawable(R.drawable.bg_row_hightlight) else getDrawable(R.drawable.bg_row)
            (layoutParams as? android.widget.LinearLayout.LayoutParams)?.setMargins(0, 0, 0, dp(8))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
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

    private fun addEmptyRow(parent: android.widget.LinearLayout, msg: String) {
        parent.addView(makeText(msg, 14f, "#A0A5D0", false).apply {
            textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
            setPadding(0, dp(24), 0, dp(8))
        })
    }

    private fun makeText(
        txt: String, size: Float, color: String, bold: Boolean,
        fixedWidth: Int = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
        weight: Float = 0f
    ) = TextView(this).apply {
        text = txt; textSize = size
        setTextColor(android.graphics.Color.parseColor(color))
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        layoutParams = android.widget.LinearLayout.LayoutParams(
            fixedWidth, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, weight
        )
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}