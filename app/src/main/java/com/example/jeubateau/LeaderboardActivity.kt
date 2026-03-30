package com.example.jeubateau

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
 * LeaderboardActivity affiche les 100 meilleurs scores mondiaux par catégorie de véhicule.
 * Elle permet de naviguer entre les différents classements.
 */
class LeaderboardActivity : AppCompatActivity() {

    private val TAG = "LeaderboardActivity"

    // Référence à la base de données Firebase
    private val db = FirebaseDatabase
        .getInstance("https://shiftrider-cce69-default-rtdb.europe-west1.firebasedatabase.app/")
        .getReference("Classement")

    // Liste ordonnée des véhicules disponibles dans le jeu
    private val vehicules = listOf(
        "ATHLÈTE", "BMX PRO", "CAVALIER", "BOLIDE", "FORMULE 1",
        "CORSAIRE", "VOL LÉGER", "AIRLINER", "STAR JUMPER", "COMÈTE"
    )

    private var indexActuel = 0
    private var monId = ""

    // --- ATTRIBUTS DE L'INTERFACE ---
    private lateinit var tvVehicule: TextView
    private lateinit var layoutLignes: LinearLayout
    private lateinit var pbLoading: ProgressBar
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var btnRetour: Button

    /**
     * Initialisation de l'activité du classement.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verrouillage de l'écran en mode portrait
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        setContentView(R.layout.activity_leaderboard)

        // Liaison des vues XML
        initViews()

        // Récupération des préférences utilisateur
        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
        monId = prefs.getString("MON_ID_SECRET", "unknown") ?: "unknown"
        val themeActuel = prefs.getString("THEME_NOM", "ATHLÈTE") ?: "ATHLÈTE"

        // Positionnement automatique sur le véhicule actuellement équipé
        val foundIndex = vehicules.indexOf(themeActuel)
        if (foundIndex != -1) indexActuel = foundIndex

        setupListeners()
        mettreAJourAffichage()
    }

    /**
     * Lie les composants XML aux variables de classe.
     */
    private fun initViews() {
        tvVehicule = findViewById(R.id.tv_vehicule_actuel)
        layoutLignes = findViewById(R.id.layout_lignes)
        pbLoading = findViewById(R.id.pb_loading)
        btnPrev = findViewById(R.id.btn_prev)
        btnNext = findViewById(R.id.btn_next)
        btnRetour = findViewById(R.id.btn_retour)
    }

    /**
     * Configure les boutons de navigation (Précédent, Suivant, Retour).
     */
    private fun setupListeners() {
        // Navigation vers le véhicule précédent
        btnPrev.setOnClickListener {
            if (indexActuel > 0) {
                indexActuel--
                mettreAJourAffichage()
            }
        }

        // Navigation vers le véhicule suivant
        btnNext.setOnClickListener {
            if (indexActuel < vehicules.size - 1) {
                indexActuel++
                mettreAJourAffichage()
            }
        }

        // Retour à l'accueil
        btnRetour.setOnClickListener { 
            finish()
        }
    }

    /**
     * Met à jour le nom du véhicule affiché et lance le chargement des scores.
     */
    private fun mettreAJourAffichage() {
        val nomVehicule = vehicules[indexActuel]
        tvVehicule.text = nomVehicule
        chargerClassement(sanitizeKey(nomVehicule))
    }

    /**
     * Nettoie le nom du véhicule pour correspondre à la clé utilisée dans Firebase.
     */
    private fun sanitizeKey(nom: String): String {
        val map = mapOf(
            'É' to 'E', 'È' to 'E', 'Ê' to 'E', 'Ë' to 'E',
            'À' to 'A', 'Â' to 'A', 'Ä' to 'A', 'Î' to 'I', 'Ï' to 'I',
            'Ô' to 'O', 'Ö' to 'O', 'Û' to 'U', 'Ù' to 'U', 'Ü' to 'U',
            'Ç' to 'C', ' ' to '_'
        )
        return nom.uppercase()
            .map { map[it] ?: it }
            .joinToString("")
            .replace(Regex("[^A-Z0-9_]"), "_")
            .trim('_')
    }

    /**
     * Récupère les scores depuis Firebase et les affiche dans la liste.
     */
    private fun chargerClassement(themeKey: String) {
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

            // Extraction et conversion des données Firebase
            for (donnee in snapshot.children) {
                val idFirebase = donnee.key ?: ""
                val nom = donnee.child("nom").value?.toString() ?: "Anonyme"
                val scoreFirebase = donnee.child("score").value?.toString()?.toIntOrNull() ?: 0
                joueurs.add(JoueurScore(idFirebase, nom, scoreFirebase))
            }

            // Tri pour avoir les meilleurs scores en haut
            joueurs.sortByDescending { it.score }

            // Construction dynamique de la liste
            for ((index, joueur) in joueurs.withIndex()) {
                val isMe = (joueur.id == monId)
                addRow(index, joueur.nom, joueur.score.toString(), isMe)
            }

        }.addOnFailureListener { e ->
            pbLoading.visibility = View.GONE
            addEmptyRow("Erreur de chargement.")
            Log.e(TAG, "Erreur lors de la récupération des scores : ${e.message}")
        }
    }

    /**
     * Ajoute visuellement une ligne de score au classement.
     */
    private fun addRow(rank: Int, nom: String, score: String, isMe: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
            
            // Couleur de fond spécifique si c'est le score du joueur actuel
            background = if (isMe) getDrawable(R.drawable.bg_row_hightlight) else getDrawable(R.drawable.bg_row)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dpToPx(8)) }
        }

        // Affichage du rang avec une médaille pour le podium
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
     * Affiche un message d'information si la liste est vide.
     */
    private fun addEmptyRow(msg: String) {
        layoutLignes.addView(createText(msg, 14f, "#A0A5D0", false).apply {
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, dpToPx(24), 0, dpToPx(8))
        })
    }

    /**
     * Utilitaire pour créer un TextView stylisé par code.
     */
    private fun createText(
        txt: String, size: Float, color: String, bold: Boolean, 
        fixedWidth: Int = LinearLayout.LayoutParams.WRAP_CONTENT, 
        weight: Float = 0f
    ) = TextView(this).apply {
        text = txt; textSize = size
        setTextColor(Color.parseColor(color))
        if (bold) setTypeface(null, Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(fixedWidth, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
    }

    /**
     * Convertit les DP en Pixels pour garantir un affichage identique sur tous les écrans.
     */
    private fun dpToPx(dpValue: Int) = (dpValue * resources.displayMetrics.density).toInt()
}