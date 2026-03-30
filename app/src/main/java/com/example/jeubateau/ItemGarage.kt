package com.example.jeubateau

/**
 * ItemGarage est une classe de données représentant un véhicule du garage.
 * 
 * @property id Identifiant unique utilisé pour les SharedPreferences (ex: "coureur").
 * @property nom Nom affiché à l'utilisateur (ex: "ATHLÈTE").
 * @property description Texte descriptif des caractéristiques du véhicule.
 * @property prix Coût en pièces d'or pour débloquer l'item.
 * @property imageRes Identifiant de la ressource image (R.drawable.xxx).
 * @property vitesseBase Vitesse initiale de défilement pour ce véhicule.
 * @property niveauDifficulte Label de difficulté (TRÈS FACILE, EXPERT, etc.).
 */
data class ItemGarage(
    val id: String,
    val nom: String,
    val description: String,
    val prix: Int,
    val imageRes: Int,
    val vitesseBase: Float,
    val niveauDifficulte: String
)