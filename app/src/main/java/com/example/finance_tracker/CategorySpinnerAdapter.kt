package com.example.finance_tracker

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class CategorySpinnerAdapter(context: Context, categories: List<Category>) :
    ArrayAdapter<Category>(context, 0, categories) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val category = getItem(position)!!
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_category_spinner, parent, false)

        val icon = view.findViewById<ImageView>(R.id.ivSpinnerCategoryIcon)
        val name = view.findViewById<TextView>(R.id.tvSpinnerCategoryName)

        name.text = category.name

        val iconResId = getIconResourceId(context, category.iconName)
        icon.setImageResource(iconResId)

        val background = icon.background as GradientDrawable
        background.setColor(Color.parseColor(category.colorHex))

        return view
    }

    private fun getIconResourceId(context: Context, iconName: String): Int {
        return context.resources.getIdentifier(iconName, "drawable", context.packageName).let {
            if (it == 0) R.drawable.ic_category_other else it
        }
    }
}