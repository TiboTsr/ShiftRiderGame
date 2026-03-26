package com.example.jeubateau

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class GarageAdapter(private val items: List<ItemGarage>) :
    RecyclerView.Adapter<GarageAdapter.GarageViewHolder>() {

    class GarageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.iv_item_garage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GarageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_page_garage, parent, false)
        return GarageViewHolder(view)
    }

    override fun onBindViewHolder(holder: GarageViewHolder, position: Int) {
        val item = items[position]
        holder.imageView.setImageResource(item.imageRes)
    }

    override fun getItemCount() = items.size
}