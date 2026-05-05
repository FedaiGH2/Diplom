package com.example.diplom.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.diplom.R
import com.example.diplom.models.ProductItem

class SearchAdapter(
    private val context: Context,
    private val products: MutableList<ProductItem>,
    private val listener: OnProductClickListener
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    interface OnProductClickListener {
        fun onProductClick(product: ProductItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]
        holder.nameText.text = product.name
        holder.itemView.setOnClickListener { listener.onProductClick(product) }
    }

    override fun getItemCount(): Int = products.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.nameText)
    }

    fun updateList(newList: List<ProductItem>) {
        products.clear()
        products.addAll(newList)
        notifyDataSetChanged()
    }
}