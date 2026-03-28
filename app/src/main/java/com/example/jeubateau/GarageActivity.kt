package com.example.jeubateau

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class GarageActivity : AppCompatActivity() {

    private lateinit var items: List<ItemGarage>
    private var positionActuelle = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Force le mode portrait avant même de charger la vue
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_garage)

        items = listOf(
            ItemGarage(
                "coureur",
                "ATHLÈTE",
                "Rien ne bat la vitesse humaine. Courez vers la victoire !",
                0,
                R.drawable.coureur_image,
                15f,
                "TRÈS FACILE"
            ),
            ItemGarage(
                "velo",
                "BMX PRO",
                "Léger et agile. Parfait pour slalomer entre les obstacles.",
                50,
                R.drawable.velo_image,
                18f,
                "FACILE"
            ),
            ItemGarage(
                "cavalier",
                "CAVALIER",
                "Un destrier puissant pour franchir tous les obstacles du domaine.",
                100,
                R.drawable.cavalier_image,
                21f,
                "MOYEN"
            ),
            ItemGarage(
                "route",
                "BOLIDE",
                "La puissance du moteur au service de vos réflexes.",
                150,
                R.drawable.voiture_image,
                24f,
                "NORMAL"
            ),
            ItemGarage(
                "course",
                "FORMULE 1",
                "Le summum de la vitesse. Accrochez-vous !",
                300,
                R.drawable.voituredecourse_image,
                28f,
                "DIFFICILE"
            ),
            ItemGarage(
                "mer",
                "CORSAIRE",
                "Défiez les vagues à bord de ce navire légendaire.",
                500,
                R.drawable.bateau_image,
                32f,
                "EXPERT"
            ),
            ItemGarage(
                "avion_p",
                "VOL LÉGER",
                "Prenez de la hauteur avec cet avion de tourisme.",
                750,
                R.drawable.petitavion_image,
                36f,
                "EXPERT"
            ),
            ItemGarage(
                "avion_g",
                "AIRLINER",
                "Le géant des airs. Imposant et rapide.",
                1000,
                R.drawable.grandavion_image,
                40f,
                "MAÎTRE"
            ),
            ItemGarage(
                "espace",
                "STAR JUMPER",
                "L'infini n'attend que vous. Voyagez vers les étoiles.",
                1500,
                R.drawable.fusee_image,
                46f,
                "MAÎTRE"
            ),
            ItemGarage(
                "comete",
                "COMÈTE",
                "Une entité cosmique dévastatrice. La vitesse ultime.",
                2500,
                R.drawable.comete_image,
                55f,
                "LÉGENDAIRE"
            )
        )

        val viewPager = findViewById<ViewPager2>(R.id.viewPagerGarage)
        val tvNom = findViewById<TextView>(R.id.tv_nom_item)
        val tvDesc = findViewById<TextView>(R.id.tv_desc_item)
        val btnAction = findViewById<Button>(R.id.btn_action_garage)
        val tvMesPieces = findViewById<TextView>(R.id.tv_mes_pieces)

        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
        val themeActuel = prefs.getString("THEME_NOM", "ATHLÈTE")

        tvMesPieces.text = "🪙 ${prefs.getInt("TOTAL_COINS", 0)}"

        val adapter = GarageAdapter(items)
        viewPager.adapter = adapter

        // --- CORRECTION DE L'AFFICHAGE AU DÉMARRAGE ---

        // 1. On cherche à quel index se trouve notre véhicule actuel
        var indexDepart = items.indexOfFirst { it.nom == themeActuel }
        if (indexDepart == -1) indexDepart = 0 // Sécurité si rien n'est trouvé

        positionActuelle = indexDepart

        // 2. On place le carrousel d'images directement sur le bon véhicule
        viewPager.setCurrentItem(indexDepart, false)

        // 3. On force la mise à jour des textes (Titre, Description) pour le BON véhicule !
        tvNom.post { actualiserUI(items[indexDepart], tvNom, tvDesc, btnAction) }

        // 4. On écoute les prochains glissements de doigt du joueur
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                positionActuelle = position
                actualiserUI(items[position], tvNom, tvDesc, btnAction)
            }
        })

        // ----------------------------------------------

        // Fonctionnement des flèches latérales (Correction : ImageButton)
        val btnLeft = findViewById<ImageButton>(R.id.btn_left)
        val btnRight = findViewById<ImageButton>(R.id.btn_right)

        btnLeft.setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.currentItem -= 1
            }
        }

        btnRight.setOnClickListener {
            if (viewPager.currentItem < items.size - 1) {
                viewPager.currentItem += 1
            }
        }

        btnAction.setOnClickListener {
            tenterAchat(items[positionActuelle], btnAction)
        }

        findViewById<Button>(R.id.btn_retour_accueil).setOnClickListener { finish() }
    }

    private fun actualiserUI(item: ItemGarage, tvNom: TextView, tvDesc: TextView, btn: Button) {
        tvNom.text = "${item.nom}  ·  ${item.niveauDifficulte}"
        tvDesc.text = item.description

        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
        val dejaAchete = prefs.getBoolean("HAS_${item.id.uppercase()}", item.prix == 0)
        val estActif   = prefs.getString("THEME_ACTIF", "coureur") == item.id

        when {
            estActif   -> { btn.text = "SÉLECTIONNÉ"; btn.isEnabled = false; btn.setBackgroundColor(Color.parseColor("#1B5E20")) }
            dejaAchete -> { btn.text = "SÉLECTIONNER"; btn.isEnabled = true; btn.setBackgroundColor(Color.parseColor("#4CAF50")) }
            else       -> { btn.text = "ACHETER (${item.prix} 🪙)"; btn.isEnabled = true; btn.setBackgroundColor(Color.parseColor("#FFAA00")) }
        }
    }

    private fun tenterAchat(item: ItemGarage, btn: Button) {
        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
        var pieces = prefs.getInt("TOTAL_COINS", 0)
        val dejaAchete = prefs.getBoolean("HAS_${item.id.uppercase()}", item.prix == 0)

        if (dejaAchete) {
            prefs.edit()
                .putString("THEME_ACTIF", item.id)
                .putString("THEME_NOM", item.nom)
                .putFloat("VITESSE_ACTUELLE", item.vitesseBase)
                .putString("DIFF_ACTUELLE", item.niveauDifficulte)
                .apply()
            Toast.makeText(this, "${item.nom} activé !", Toast.LENGTH_SHORT).show()
            actualiserUI(item, findViewById(R.id.tv_nom_item), findViewById(R.id.tv_desc_item), btn)
        } else if (pieces >= item.prix) {
            pieces -= item.prix
            prefs.edit()
                .putInt("TOTAL_COINS", pieces)
                .putBoolean("HAS_${item.id.uppercase()}", true)
                .putString("THEME_ACTIF", item.id)
                .putString("THEME_NOM", item.nom)
                .putFloat("VITESSE_ACTUELLE", item.vitesseBase)
                .putString("DIFF_ACTUELLE", item.niveauDifficulte)
                .apply()
            findViewById<TextView>(R.id.tv_mes_pieces).text = "🪙 $pieces"
            actualiserUI(item, findViewById(R.id.tv_nom_item), findViewById(R.id.tv_desc_item), btn)
            Toast.makeText(this, "Débloqué : ${item.nom}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Il vous manque ${item.prix - pieces} pièces !", Toast.LENGTH_SHORT).show()
        }
    }
}