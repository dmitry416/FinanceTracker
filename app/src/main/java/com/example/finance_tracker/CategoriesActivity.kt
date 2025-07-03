package com.example.finance_tracker

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoriesActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var categoryAdapter: CategoryAdapter

    private val iconList = listOf(
        "ic_category_food",
        "ic_category_transport",
        "ic_category_shopping",
        "ic_category_health",
        "ic_category_salary",
        "ic_category_other",
        "ic_category_accessibility",
        "ic_category_bitcoin",
        "ic_category_exercise",
        "ic_category_flight",
        "ic_category_portal",
        "ic_category_star",
        "ic_category_stories"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        setupRecyclerView()
        loadCategories()

        findViewById<FloatingActionButton>(R.id.fabAddCategory).setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(emptyList()) { category ->
            deleteCategory(category)
        }
        findViewById<RecyclerView>(R.id.rvCategories).adapter = categoryAdapter
    }

    private fun loadCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            val userId = sessionManager.getUserId()
            val categories = db.categoryDao().getAllCategories(userId)
            withContext(Dispatchers.Main) {
                categoryAdapter.updateCategories(categories)
            }
        }
    }

    private fun deleteCategory(category: Category) {
        AlertDialog.Builder(this)
            .setTitle("Удалить категорию")
            .setMessage("Вы уверены, что хотите удалить категорию '${category.name}'?")
            .setPositiveButton("Да") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    db.categoryDao().delete(category)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@CategoriesActivity,
                            "Категория удалена",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadCategories()
                    }
                }
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
        val etCategoryName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val ivSelectedIcon = dialogView.findViewById<ImageView>(R.id.ivSelectedIcon)
        val btnChooseIcon = dialogView.findViewById<View>(R.id.btnChooseIcon)
        val viewSelectedColor = dialogView.findViewById<View>(R.id.viewSelectedColor)

        var selectedIconName = "ic_category_other"
        var selectedColorHex = "#808080"

        (viewSelectedColor.background as GradientDrawable).setColor(
            Color.parseColor(
                selectedColorHex
            )
        )
        (ivSelectedIcon.background as GradientDrawable).setColor(Color.parseColor(selectedColorHex))


        btnChooseIcon.setOnClickListener {
            showIconPickerDialog { iconName ->
                selectedIconName = iconName
                val resId = resources.getIdentifier(iconName, "drawable", packageName)
                ivSelectedIcon.setImageResource(resId)
            }
        }

        viewSelectedColor.setOnClickListener {
            ColorPickerDialog
                .Builder(this)
                .setTitle("Выберите цвет")
                .setColorShape(ColorShape.CIRCLE)
                .setDefaultColor(selectedColorHex)
                .setColorListener { color, colorHex ->
                    selectedColorHex = colorHex
                    val color = Color.parseColor(colorHex)
                    (viewSelectedColor.background as GradientDrawable).setColor(color)
                    (ivSelectedIcon.background as GradientDrawable).setColor(color)
                }
                .show()
        }

        AlertDialog.Builder(this)
            .setTitle("Добавить категорию")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = etCategoryName.text.toString().trim()
                if (name.isBlank()) {
                    Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newCategory = Category(
                    name = name,
                    iconName = selectedIconName,
                    colorHex = selectedColorHex,
                    isDefault = false,
                    userId = sessionManager.getUserId()
                )

                CoroutineScope(Dispatchers.IO).launch {
                    db.categoryDao().insert(newCategory)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@CategoriesActivity,
                            "Категория добавлена",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadCategories()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showIconPickerDialog(onIconSelected: (String) -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_icon_picker, null)
        val gridView = dialogView.findViewById<GridView>(R.id.iconGridView)

        val adapter = object : ArrayAdapter<String>(this, R.layout.item_icon, iconList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_icon, parent, false)
                val iconView = view.findViewById<ImageView>(R.id.iconImageView)
                val iconName = getItem(position)!!
                val resId = resources.getIdentifier(iconName, "drawable", packageName)
                iconView.setImageResource(resId)
                return view
            }
        }
        gridView.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setTitle("Выберите иконку")
            .setView(dialogView)
            .setNegativeButton("Отмена", null)
            .create()

        gridView.setOnItemClickListener { _, _, position, _ ->
            onIconSelected(iconList[position])
            dialog.dismiss()
        }

        dialog.show()
    }
}
