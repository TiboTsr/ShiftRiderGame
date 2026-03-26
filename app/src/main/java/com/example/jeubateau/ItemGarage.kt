package com.example.jeubateau

data class ItemGarage(
    val id: String,
    val nom: String,
    val description: String,
    val prix: Int,
    val imageRes: Int,
    val vitesseBase: Float,
    val niveauDifficulte: String
)