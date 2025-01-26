package com.alperyuceer.komik_replikler

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.alperyuceer.komik_replikler.databinding.ActivitySearchBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {
  /*  private lateinit var db: ReplikDatabase
    private lateinit var replikDao: ReplikDao
    private lateinit var binding: ActivitySearchBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TopAppBar'ı ayarla
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Geri tuşunun rengini ayarla
        binding.topAppBar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.menu_item_color))

        binding.searchEditText.requestFocus()
        db = Room.databaseBuilder(applicationContext,ReplikDatabase::class.java, ReplikDatabase.DATABASE_NAME).build()

        binding.searchEditText.doOnTextChanged { text, start, before, count ->
            search(text.toString())
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun search(query: String) {
        // Küfür filtresinin durumunu kontrol et
        val isKufurFiltresiActive = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getBoolean("kufur_filtresi", false)

        CoroutineScope(Dispatchers.IO).launch {
            replikDao = db.replikDao()
            val searchResults = replikDao.searchByName(query)

            // Küfür filtresini uygula
            val filteredResults = if (isKufurFiltresiActive) {
                searchResults.filter { replik -> "Küfürlü" !in replik.kategori }
            } else {
                searchResults
            }

            // Arayüzü güncellemek için ana iş parçacığına geçin
            withContext(Dispatchers.Main) {
                // RecyclerView için layoutManager ve adapter oluşturun
                val layoutManager = LinearLayoutManager(this@SearchActivity)
                val category = "dizi"
                val adapter = ReplikAdapter(filteredResults, category)

                binding.searchRecyclerView.layoutManager = layoutManager
                binding.searchRecyclerView.adapter = adapter

                // Boş durum kontrolü
                if (filteredResults.isEmpty() && query.isNotEmpty()) {
                    binding.searchRecyclerView.visibility = View.GONE
                    binding.emptySearchIcon.visibility = View.VISIBLE
                } else {
                    binding.searchRecyclerView.visibility = View.VISIBLE
                    binding.emptySearchIcon.visibility = View.GONE
                }
            }
        }
    }*/
}