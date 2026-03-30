package com.example.jeubateau

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

/**
 * GarageActivity gère la boutique du jeu.
 * Elle permet de visualiser, acheter et sélectionner différents véhicules/thèmes.
 */
class GarageActivity : AppCompatActivity() {

    private val TAG = "GarageActivity"

    // --- ATTRIBUTS DE CLASSE ---
    private lateinit var items: List<ItemGarage>
    private var positionActuelle = 0

    // --- ATTRIBUTS DE L'INTERFACE ---
    private lateinit var viewPager: ViewPager2
    private lateinit var tvNomItem: TextView
    private lateinit var tvDescItem: TextView
    private lateinit var btnAction: Button
    private lateinit var tvMesPieces: TextView
    private lateinit var btnLeft: ImageButton
    private lateinit var btnRight: ImageButton

    /**
     * Initialisation du garage.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Ouverture du garage")
        
        // Verrouillage de l'orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_garage)

        // Initialisation de la liste des véhicules disponibles
        initCatalog()

        // Liaison des widgets XML
        initViews()

        // Configuration du carrousel ViewPager2
        setupViewPager()

        // Configuration des boutons de navigation
        setupListeners()
    }

    /**
     * Remplit la liste des véhicules avec leurs caractéristiques (prix, vitesse, difficulté).
     */
    private fun initCatalog() {
        items = listOf(
            ItemGarage("coureur", "ATHLÈTE", "Rien ne bat la vitesse humaine. Courez vers la victoire !", 0, R.drawable.coureur_image, 15f, "TRÈS FACILE"),
            ItemGarage("velo", "BMX PRO", "Léger et agile. Parfait pour slalomer.", 50, R.drawable.velo_image, 18f, "FACILE"),
            ItemGarage("cavalier", "CAVALIER", "Un destrier puissant pour franchir les obstacles.", 100, R.drawable.cavalier_image, 21f, "MOYEN"),
            ItemGarage("route", "BOLIDE", "La puissance du moteur au service des réflexes.", 150, R.drawable.voiture_image, 24f, "NORMAL"),
            ItemGarage("course", "FORMULE 1", "Le summum de la vitesse. Accrochez-vous !", 300, R.drawable.voituredecourse_image, 28f, "DIFFICILE"),
            ItemGarage("mer", "CORSAIRE", "Défiez les vagues à bord de ce navire légendaire.", 500, R.drawable.bateau_image, 32f, "EXPERT"),
            ItemGarage("avion_p", "VOL LÉGER", "Prenez de la hauteur avec cet avion de tourisme.", 750, R.drawable.petitavion_image, 36f, "EXPERT"),
            ItemGarage("avion_g", "AIRLINER", "Le géant des airs. Imposant et rapide.", 1000, R.drawable.grandavion_image, 40f, "MAÎTRE"),
            ItemGarage("espace", "STAR JUMPER", "L'infini n'attend que vous vers les étoiles.", 1500, R.drawable.fusee_image, 46f, "MAÎTRE"),
            ItemGarage("comete", "COMÈTE", "Une entité cosmique dévastatrice. Vitesse ultime.", 2500, R.drawable.comete_image, 55f, "LÉGENDAIRE")
        )
    }

    /**
     * Lie les composants graphiques aux variables.
     */
    private fun initViews() {
        viewPager   = findViewById(R.id.viewPagerGarage)
        tvNomItem   = findViewById(R.id.tv_nom_item)
        tvDescItem  = findViewById(R.id.tv_desc_item)
        btnAction   = findViewById(R.id.btn_action_garage)
        tvMesPieces = findViewById(R.id.tv_mes_pieces)
        btnLeft     = findViewById(R.id.btn_left)
        btnRight    = findViewById(R.id.btn_right)

        // Affichage du solde de pièces actuel
        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
        tvMesPieces.text = getString(R.string.coins_prefix, prefs.getInt("TOTAL_COINS", 0))
    }

    /**
     * Configure le ViewPager pour afficher les images des véhicules.
     */
    private fun setupViewPager() {
        val adapter = GarageAdapter(items)
        viewPager.adapter = adapter

        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
        val themeActuel = prefs.getString("THEME_NOM", "ATHLÈTE")

        // Positionnement sur le véhicule actuellement sélectionné par le joueur
        var indexDepart = items.indexOfFirst { it.nom == themeActuel }
        if (indexDepart == -1) indexDepart = 0
        
        positionActuelle = indexDepart
        viewPager.setCurrentItem(indexDepart, false)

        // Mise à jour de l'UI pour l'élément de départ
        tvNomItem.post { actualiserUI(items[indexDepart]) }

        // Callback lors du changement de page (glissement)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                positionActuelle = position
                actualiserUI(items[position])
            }
        })
    }

    /**
     * Configure les actions des boutons du garage.
     */
    private fun setupListeners() {
        btnLeft.setOnClickListener {
            if (viewPager.currentItem > 0) viewPager.currentItem -= 1
        }

        btnRight.setOnClickListener {
            if (viewPager.currentItem < items.size - 1) viewPager.currentItem += 1
        }

        btnAction.setOnClickListener {
            tenterAction(items[positionActuelle])
        }

        findViewById<Button>(R.id.btn_retour_accueil).setOnClickListener { finish() }
    }

    /**
     * Met à jour les textes et le bouton selon l'état du véhicule (possédé ou non).
     */
    private fun actualiserUI(item: ItemGarage) {
        tvNomItem.text = "${item.nom}  ·  ${item.niveauDifficulte}"
        tvDescItem.text = item.description

        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
        val dejaAchete = prefs.getBoolean("HAS_${item.id.uppercase()}", item.prix == 0)
        val estActif   = prefs.getString("THEME_ACTIF", "coureur") == item.id

        // Modification visuelle du bouton d'action
        when {
            estActif   -> { 
                btnAction.text = "SÉLECTIONNÉ"
                btnAction.isEnabled = false
                btnAction.setBackgroundColor(Color.parseColor("#1B5E20")) 
            }
            dejaAchete -> { 
                btnAction.text = "SÉLECTIONNER"
                btnAction.isEnabled = true
                btnAction.setBackgroundColor(Color.parseColor("#4CAF50")) 
            }
            else       -> { 
                btnAction.text = "ACHETER (${item.prix} 🪙)"
                btnAction.isEnabled = true
                btnAction.setBackgroundColor(Color.parseColor("#FFAA00")) 
            }
        }
    }

    /**
     * Gère l'achat ou l'activation d'un véhicule.
     */
    private fun tenterAction(item: ItemGarage) {
        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
        var pieces = prefs.getInt("TOTAL_COINS", 0)
        val dejaAchete = prefs.getBoolean("HAS_${item.id.uppercase()}", item.prix == 0)

        if (dejaAchete) {
            // Activation simple
            activerVehicule(item, prefs)
        } else if (pieces >= item.prix) {
            // Achat réussi
            pieces -= item.prix
            Log.i(TAG, "Achat réussi : ${item.nom}")
            prefs.edit().putInt("TOTAL_COINS", pieces).apply()
            prefs.edit().putBoolean("HAS_${item.id.uppercase()}", true).apply()
            
            tvMesPieces.text = getString(R.string.coins_prefix, pieces)
            activerVehicule(item, prefs)
            Toast.makeText(this, "Débloqué : ${item.nom}", Toast.LENGTH_SHORT).show()
        } else {
            // Fonds insuffisants
            Log.d(TAG, "Fonds insuffisants pour ${item.nom}")
            Toast.makeText(this, "Il vous manque ${item.prix - pieces} pièces !", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Enregistre les réglages du véhicule sélectionné dans les SharedPreferences.
     */
    private fun activerVehicule(item: ItemGarage, prefs: android.content.SharedPreferences) {
        prefs.edit()
            .putString("THEME_ACTIF", item.id)
            .putString("THEME_NOM", item.nom)
            .putFloat("VITESSE_ACTUELLE", item.vitesseBase)
            .putString("DIFF_ACTUELLE", item.niveauDifficulte)
            .apply()
        
        Log.d(TAG, "Véhicule activé : ${item.nom}")
        Toast.makeText(this, "${item.nom} activé !", Toast.LENGTH_SHORT).show()
        actualiserUI(item)
    }
}