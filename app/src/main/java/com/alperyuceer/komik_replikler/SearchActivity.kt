package com.alperyuceer.komik_replikler

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.alperyuceer.komik_replikler.api.FavoriteRequest
import com.alperyuceer.komik_replikler.api.RetrofitInstance
import com.alperyuceer.komik_replikler.databinding.ActivitySearchBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.View
import java.util.UUID
import android.widget.Toast

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private var searchJob: Job? = null
    private val deviceId: String by lazy {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.getString("device_uuid", null) ?: run {
            val newUuid = UUID.randomUUID().toString()
            prefs.edit().putString("device_uuid", newUuid).apply()
            newUuid
        }
    }

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

        // Arama işlemi
        binding.searchEditText.doOnTextChanged { text, _, _, _ ->
            // Önceki aramayı iptal et
            searchJob?.cancel()
            
            // Yeni arama başlat
            searchJob = CoroutineScope(Dispatchers.IO).launch {
                // Kullanıcı yazmayı bitirene kadar bekle
                delay(300)
                search(text.toString())
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private suspend fun search(query: String) {
        // Küfür filtresinin durumunu kontrol et
        val isKufurFiltresiActive = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getBoolean("kufur_filtresi", false)

        try {
            val response = if (query.isNotEmpty()) {
                RetrofitInstance.api.searchReplikler(query)
            } else {
                RetrofitInstance.api.getAllReplikler()
            }

            if (response.isSuccessful) {
                var replikler = response.body() ?: emptyList()

                // Favorileri kontrol et
                val favorilerResponse = RetrofitInstance.api.getFavorites(deviceId)
                if (favorilerResponse.isSuccessful) {
                    val favoriler = favorilerResponse.body() ?: emptyList()
                    replikler = replikler.map { replik ->
                        replik.copy(favorimi = favoriler.any { it.id == replik.id })
                    }
                }

                // Küfür filtresini uygula
                val filteredResults = if (isKufurFiltresiActive) {
                    replikler.filter { replik -> "Küfürlü" !in replik.kategoriler }
                } else {
                    replikler
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
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.searchRecyclerView.visibility = View.GONE
                binding.emptySearchIcon.visibility = View.VISIBLE
                Toast.makeText(this@SearchActivity, "İnternet bağlantısı hatası", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateFavorite(replik: Replik, isFavorite: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val favoriteRequest = FavoriteRequest(deviceId, replik.id)
                if (isFavorite) {
                    RetrofitInstance.api.addToFavorites(favoriteRequest)
                } else {
                    RetrofitInstance.api.removeFromFavorites(favoriteRequest)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}