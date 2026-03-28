package com.example.jeubateau

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

class LeaderboardActivity : AppCompatActivity() {

    private val db = FirebaseDatabase
        .getInstance("https://shiftrider-cce69-default-rtdb.europe-west1.firebasedatabase.app/")
        .getReference("Classement")

    // La liste de tes véhicules dans le bon ordre
    private val vehicules = listOf(
        "ATHLÈTE", "BMX PRO", "CAVALIER", "BOLIDE", "FORMULE 1",
        "CORSAIRE", "VOL LÉGER", "AIRLINER", "STAR JUMPER", "COMÈTE"
    )

    private var indexActuel = 0
    private var monId = ""

    private lateinit var tvVehicule: TextView
    private lateinit var layoutLignes: LinearLayout
    private lateinit var pbLoading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        setContentView(R.layout.activity_leaderboard)

        tvVehicule = findViewById(R.id.tv_vehicule_actuel)
        layoutLignes = findViewById(R.id.layout_lignes)
        pbLoading = findViewById(R.id.pb_loading)

        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
        monId = prefs.getString("MON_ID_SECRET", "unknown") ?: "unknown"
        val themeActuel = prefs.getString("THEME_NOM", "ATHLÈTE") ?: "ATHLÈTE"

        // On ouvre la page directement sur le véhicule équipé par le joueur !
        val foundIndex = vehicules.indexOf(themeActuel)
        if (foundIndex != -1) indexActuel = foundIndex

        mettreAJourAffichage()

        // Bouton Flèche Gauche
        findViewById<Button>(R.id.btn_prev).setOnClickListener {
            if (indexActuel > 0) {
                indexActuel--
                mettreAJourAffichage()
            }
        }

        // Bouton Flèche Droite
        findViewById<Button>(R.id.btn_next).setOnClickListener {
            if (indexActuel < vehicules.size - 1) {
                indexActuel++
                mettreAJourAffichage()
            }
        }

        findViewById<Button>(R.id.btn_retour).setOnClickListener { finish() }
    }

    private fun mettreAJourAffichage() {
        val nomVehicule = vehicules[indexActuel]
        tvVehicule.text = nomVehicule
        chargerClassement(sanitizeKey(nomVehicule))
    }

    private fun sanitizeKey(nom: String): String {
        val map = mapOf(
            'É' to 'E', 'È' to 'E', 'Ê' to 'E', 'Ë' to 'E',
            'À' to 'A', 'Â' to 'A', 'Ä' to 'A', 'Î' to 'I', 'Ï' to 'I',
            'Ô' to 'O', 'Ö' to 'O', 'Û' to 'U', 'Ù' to 'U', 'Ü' to 'U',
            'Ç' to 'C', ' ' to '_'
        )
        return nom.uppercase().map { map[it] ?: it }.joinToString("").replace(Regex("[^A-Z0-9_]"), "_").trim('_')
    }

    private fun chargerClassement(themeKey: String) {
        layoutLignes.removeAllViews()
        pbLoading.visibility = View.VISIBLE

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

        }.addOnFailureListener {
            pbLoading.visibility = View.GONE
            addEmptyRow(layoutLignes, "Erreur de chargement de la base de données.")
        }
    }

    // --- Fonctions d'affichage des jolies lignes (comme dans le GameOver) ---
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
        row.addView(makeText(if (isMe) "$nom  ◀" else nom, 15f, if (isMe) "#FFAA00" else "#FFFFFF", true, 0, weight = 1f))
        row.addView(makeText(score, 17f, "#FFAA00", true))
        parent.addView(row)
    }

    private fun addEmptyRow(parent: LinearLayout, msg: String) {
        parent.addView(makeText(msg, 14f, "#A0A5D0", false).apply {
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, dp(24), 0, dp(8))
        })
    }

    private fun makeText(txt: String, size: Float, color: String, bold: Boolean, fixedWidth: Int = LinearLayout.LayoutParams.WRAP_CONTENT, weight: Float = 0f) = TextView(this).apply {
        text = txt; textSize = size
        setTextColor(Color.parseColor(color))
        if (bold) setTypeface(null, Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(fixedWidth, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}