package com.alperyuceer.komik_replikler.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.alperyuceer.komik_replikler.MainActivity
import com.alperyuceer.komik_replikler.Replik
import com.alperyuceer.komik_replikler.ReplikAdapter
import com.alperyuceer.komik_replikler.databinding.FragmentGenericBinding
import com.alperyuceer.komik_replikler.R
import com.alperyuceer.komik_replikler.api.RetrofitInstance
import kotlinx.coroutines.*
import java.util.UUID

class GenericFragment : Fragment() {
    private lateinit var binding: FragmentGenericBinding
    private lateinit var category: String
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val deviceId: String by lazy {
        val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.getString("device_uuid", null) ?: run {
            val newUuid = UUID.randomUUID().toString()
            prefs.edit().putString("device_uuid", newUuid).apply()
            newUuid
        }
    }

    companion object {
        // Kategori bazlı önbellek
        private val replikCache = mutableMapOf<String, List<Replik>>()

        fun newInstance(category: String): GenericFragment {
            val fragment = GenericFragment()
            val args = Bundle()
            args.putString("category", category)
            fragment.arguments = args
            return fragment
        }

        // Önbelleği temizle
        fun clearCache() {
            replikCache.clear()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = arguments?.getString("category") ?: "general"
        Log.d("GenericFragment", "Category: $category")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGenericBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // SwipeRefreshLayout'u ayarla
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.white)
        binding.swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.black)
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Favoriler kategorisi için önbelleği temizle
            if (category.equals("favoriler", ignoreCase = true)) {
                replikCache.remove(category.lowercase())
            }
            loadRepliks()
        }

        // Önbellekte varsa direkt kullan
        replikCache[category.lowercase()]?.let {
            handleResponse(it)
            return
        }

        loadRepliks()
    }

    private fun loadRepliks() {
        // İlk yüklemede loading göster, yenileme sırasında gösterme
        if (!binding.swipeRefreshLayout.isRefreshing) {
            binding.loadingProgressBar.visibility = View.VISIBLE
        }
        binding.recyclerView.visibility = View.GONE
        binding.emptyFavoritesIcon.visibility = View.GONE

        coroutineScope.launch {
            try {
                // API'den replikleri al
                val response = when (category.lowercase()) {
                    "favoriler" -> {
                        Log.d("ReplikDebug", "Favoriler yükleniyor...")
                        RetrofitInstance.api.getFavorites(deviceId)
                    }
                    else -> {
                        val formattedCategory = category.replaceFirstChar { it.uppercase() }
                        Log.d("ReplikDebug", "$formattedCategory kategorisi yükleniyor...")
                        RetrofitInstance.api.getRepliklerByKategori(formattedCategory)
                    }
                }

                if (response.isSuccessful) {
                    var replikler = response.body() ?: emptyList()
                    
                    if (category.lowercase() == "favoriler") {
                        if (replikler.isEmpty()) {
                            Log.d("ReplikDebug", "Favoriler listesi boş")
                        } else {
                            replikler = replikler.map { it.copy(favorimi = true) }
                        }
                    } else {
                        // Favori durumlarını kontrol et
                        val favorilerResponse = RetrofitInstance.api.getFavorites(deviceId)
                        if (favorilerResponse.isSuccessful) {
                            val favoriler = favorilerResponse.body() ?: emptyList()
                            replikler = replikler.map { replik ->
                                replik.copy(favorimi = favoriler.any { it.id == replik.id })
                            }
                            Log.d("ReplikDebug", "${replikler.count { it.favorimi }} adet favori replik bulundu")
                        }
                    }

                    Log.d("ReplikDebug", "${replikler.size} adet replik yüklendi")

                    // Favoriler hariç diğer kategorileri önbellekle
                    if (category.lowercase() != "favoriler") {
                        replikCache[category.lowercase()] = replikler
                    }

                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false
                        handleResponse(replikler)
                    }
                } else {
                    Log.e("ReplikDebug", "API Hatası: ${response.code()}")
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.recyclerView.visibility = View.VISIBLE
                        Toast.makeText(context, "Replikler yüklenirken bir hata oluştu", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ReplikDebug", "Bağlantı hatası: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.recyclerView.visibility = View.VISIBLE
                    Toast.makeText(context, "Bağlantı hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleResponse(replikList: List<Replik>) {
        // Küfür filtresinin durumunu kontrol et
        val isKufurFiltresiActive = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("kufur_filtresi", false)

        // Küfür filtresini uygula
        val filteredList = if (isKufurFiltresiActive) {
            replikList.filter { replik -> "Küfürlü" !in replik.kategoriler }
        } else {
            replikList
        }

        binding.apply {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            val adapter = ReplikAdapter(filteredList, category).apply {
                // Eğer kategori favorilerse, boş durum listener'ını ekle
                if (category == "favoriler") {
                    setOnListEmptyListener(object : ReplikAdapter.OnListEmptyListener {
                        override fun onListEmpty() {
                            // Favorilerden öğe çıkarıldığında animasyonlu göster
                            showEmptyStateWithAnimation()
                        }
                    })
                }
                
                // Replik oynatma listener'ını ekle
                setOnReplikPlayedListener(object : ReplikAdapter.OnReplikPlayedListener {
                    override fun onReplikPlayed() {
                        (activity as? MainActivity)?.onReplikPlayed()
                    }
                })
            }
            
            recyclerView.adapter = adapter

            // İlk yüklemede boş durum kontrolü - animasyonsuz
            if (category == "favoriler" && filteredList.isEmpty()) {
                showEmptyStateWithoutAnimation()
            } else {
                emptyFavoritesIcon.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun showEmptyStateWithAnimation() {
        binding.apply {
            recyclerView.visibility = View.GONE
            emptyFavoritesIcon.visibility = View.VISIBLE
            val fadeIn = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.fade_in)
            emptyFavoritesIcon.startAnimation(fadeIn)
        }
    }

    private fun showEmptyStateWithoutAnimation() {
        binding.apply {
            recyclerView.visibility = View.GONE
            emptyFavoritesIcon.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
