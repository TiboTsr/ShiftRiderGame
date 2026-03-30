package com.example.jeubateau

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

/**
 * GameOverActivity affiche le score final, les pièces gagnées et le classement mondial.
 */
class GameOverActivity : AppCompatActivity() {

    private val TAG = "GameOverActivity"

    // Référence Firebase pour le classement mondial
    private val db = FirebaseDatabase
        .getInstance("https://shiftrider-cce69-default-rtdb.europe-west1.firebasedatabase.app/")
        .getReference("Classement")

    // --- ATTRIBUTS DE L'INTERFACE ---
    private lateinit var tvScoreFinal: TextView
    private lateinit var tvCoinsGagnes: TextView
    private lateinit var tvThemeUsed: TextView
    private lateinit var tvTitreClassement: TextView
    private lateinit var btnRejouer: Button
    private lateinit var btnAccueil: Button
    private lateinit var layoutLignes: LinearLayout
    private lateinit var pbLoading: ProgressBar

    /**
     * Initialisation de l'écran de fin de partie.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Démarrage de GameOverActivity")
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        setContentView(R.layout.activity_game_over)

        // Récupération des données du jeu
        val score = intent.getIntExtra("SCORE_FINAL", 0)
        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)

        // Identification du thème utilisé pour le classement
        val themeNom = prefs.getString("THEME_NOM", "ATHLÈTE") ?: "ATHLÈTE"
        val themeKey = sanitizeKey(themeNom)
        val monId = prefs.getString("MON_ID_SECRET", "unknown") ?: "unknown"

        // Calcul des récompenses (1 pièce toutes les 10 points)
        val pieces = score / 10
        val totalCoins = prefs.getInt("TOTAL_COINS", 0) + pieces
        prefs.edit().putInt("TOTAL_COINS", totalCoins).apply()

        initViews()
        updateUI(score, pieces, themeNom)
        setupListeners()

        // Sauvegarde et chargement des scores
        sauvegarderScore(score, themeKey, themeNom, prefs)
        chargerClassementFirebase(themeKey, monId)
    }

    /**
     * Lie les composants XML aux variables de classe.
     */
    private fun initViews() {
        tvScoreFinal    = findViewById(R.id.tv_score_final)
        tvCoinsGagnes   = findViewById(R.id.tv_coins_gagnes)
        tvThemeUsed     = findViewById(R.id.tv_theme_used)
        tvTitreClassement = findViewById(R.id.tv_titre_classement)
        btnRejouer      = findViewById(R.id.btn_rejouer)
        btnAccueil      = findViewById(R.id.btn_accueil)
        layoutLignes    = findViewById(R.id.layout_lignes)
        pbLoading       = findViewById(R.id.pb_loading)
    }

    /**
     * Met à jour les textes de l'interface.
     */
    private fun updateUI(score: Int, pieces: Int, themeNom: String) {
        tvScoreFinal.text = score.toString()
        tvCoinsGagnes.text = "+$pieces 🪙"
        tvThemeUsed.text = themeNom
        tvTitreClassement.text = "🏆 Top — $themeNom"
    }

    /**
     * Définit les actions des boutons Rejouer et Accueil.
     */
    private fun setupListeners() {
        btnRejouer.setOnClickListener {
            Log.d(TAG, "L'utilisateur souhaite rejouer")
            startActivity(Intent(this, GameActivity::class.java))
            finish()
        }
        btnAccueil.setOnClickListener { 
            Log.d(TAG, "Retour à l'accueil")
            finish() 
        }
    }

    /**
     * Nettoie le nom du thème pour l'utiliser comme clé Firebase (sans accents/espaces).
     */
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

    /**
     * Sauvegarde le score localement et sur Firebase s'il s'agit d'un nouveau record.
     */
    private fun sauvegarderScore(score: Int, themeKey: String, themeNom: String, prefs: android.content.SharedPreferences) {
        val monId   = prefs.getString("MON_ID_SECRET", "unknown") ?: "unknown"
        val pseudo  = prefs.getString("PLAYER_PSEUDO", "Joueur") ?: "Joueur"
        val bestKey = "BEST_$themeKey"
        val best    = prefs.getInt(bestKey, 0)

        // Si c'est un record pour ce véhicule spécifique
        if (score > best) {
            Log.i(TAG, "Nouveau record personnel pour $themeNom : $score")
            prefs.edit().putInt(bestKey, score).apply()

            // Mise à jour du record général si nécessaire
            if (score > prefs.getInt("HIGH_SCORE", 0)) {
                prefs.edit().putInt("HIGH_SCORE", score).apply()
            }

            // Envoi des données vers le serveur Firebase
            db.child(themeKey).child(monId).setValue(
                mapOf("nom" to pseudo, "score" to score, "theme" to themeNom)
            ).addOnSuccessListener {
                Log.d("FB", "Score enregistré avec succès sur Firebase")
            }.addOnFailureListener { e ->
                Log.e("FB", "Échec de l'envoi du score : ${e.message}")
            }
        }
    }

    /**
     * Récupère les 100 meilleurs scores depuis Firebase pour le thème actuel.
     */
    private fun chargerClassementFirebase(themeKey: String, monId: String) {
        layoutLignes.removeAllViews()
        pbLoading.visibility = View.VISIBLE

        db.child(themeKey).orderByChild("score").limitToLast(100).get().addOnSuccessListener { snapshot ->
            pbLoading.visibility = View.GONE

            if (!snapshot.exists()) {
                addEmptyRow("Aucun score pour ce véhicule.\nSoyez le premier !")
                return@addOnSuccessListener
            }

            data class JoueurScore(val id: String, val nom: String, val score: Int)
            val joueurs = mutableListOf<JoueurScore>()

            // Extraction des données de l'instantané Firebase
            for (donnee in snapshot.children) {
                val idFirebase = donnee.key ?: ""
                val nom = donnee.child("nom").value?.toString() ?: "Anonyme"
                val scoreFirebase = donnee.child("score").value?.toString()?.toIntOrNull() ?: 0
                joueurs.add(JoueurScore(idFirebase, nom, scoreFirebase))
            }

            // Tri décroissant pour le classement
            joueurs.sortByDescending { it.score }

            // Création dynamique des lignes du tableau
            for ((index, joueur) in joueurs.withIndex()) {
                val isMe = (joueur.id == monId)
                addRow(index, joueur.nom, joueur.score.toString(), isMe)
            }

        }.addOnFailureListener { e ->
            pbLoading.visibility = View.GONE
            addEmptyRow("Erreur de connexion au serveur")
            Log.e(TAG, "Erreur Firebase : ${e.message}")
        }
    }

    /**
     * Ajoute une ligne de score au classement.
     */
    private fun addRow(rank: Int, nom: String, score: String, isMe: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))

            // Style différent pour la ligne du joueur actuel
            background = if (isMe) getDrawable(R.drawable.bg_row_hightlight) else getDrawable(R.drawable.bg_row)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dpToPx(8)) }
        }

        val medal = when (rank) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "#${rank + 1}" }

        row.addView(createText(medal, 15f, "#A0A5D0", true, dpToPx(52)))
        row.addView(createText(
            if (isMe) "$nom  ◀" else nom,
            15f,
            if (isMe) "#FFAA00" else "#FFFFFF",
            true, 0, weight = 1f
        ))
        row.addView(createText(score, 17f, "#FFAA00", true))
        layoutLignes.addView(row)
    }

    /**
     * Affiche un message lorsqu'aucun score n'est disponible.
     */
    private fun addEmptyRow(msg: String) {
        layoutLignes.addView(createText(msg, 14f, "#A0A5D0", false).apply {
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, dpToPx(24), 0, dpToPx(8))
        })
    }

    /**
     * Aide à la création de TextView dynamique avec style.
     */
    private fun createText(
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

    /**
     * Convertit les DP en pixels pour un affichage cohérent.
     */
    private fun dpToPx(dpValue: Int) = (dpValue * resources.displayMetrics.density).toInt()
}