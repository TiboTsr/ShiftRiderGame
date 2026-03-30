package com.example.jeubateau

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

/**
 * GarageAdapter est utilisé par le ViewPager2 pour afficher les images 
 * des véhicules dans le garage.
 * @param items Liste des objets ItemGarage à afficher.
 */
class GarageAdapter(private val items: List<ItemGarage>) :
    RecyclerView.Adapter<GarageAdapter.GarageViewHolder>() {

    /**
     * ViewHolder contenant les vues d'un élément du carrousel.
     */
    class GarageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Variable pointant vers l'ImageView de l'item garage
        val ivItemGarage: ImageView = view.findViewById(R.id.iv_item_garage)
    }

    /**
     * Crée une nouvelle vue pour un élément.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GarageViewHolder {
        // Chargement du layout item_page_garage
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_page_garage, parent, false)
        return GarageViewHolder(view)
    }

    /**
     * Lie les données (image) à la vue correspondante.
     */
    override fun onBindViewHolder(holder: GarageViewHolder, position: Int) {
        val item = items[position]
        // On affiche l'image du véhicule à la position donnée
        holder.ivItemGarage.setImageResource(item.imageRes)
    }

    /**
     * Retourne le nombre total d'éléments dans le catalogue.
     */
    override fun getItemCount() = items.size
}