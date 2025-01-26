package com.alperyuceer.komik_replikler

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.alperyuceer.komik_replikler.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import android.net.Uri
import android.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.alperyuceer.komik_replikler.databinding.DialogCategoriesBinding
import com.google.android.material.appbar.MaterialToolbar
import android.view.Window
import com.google.android.material.switchmaterial.SwitchMaterial
import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import android.app.Dialog
import android.widget.RatingBar
import android.widget.Button
import android.content.ActivityNotFoundException
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdListener
import android.util.Log
import kotlinx.coroutines.*
import com.google.android.gms.ads.LoadAdError
import android.widget.RelativeLayout
import com.alperyuceer.komik_replikler.api.RetrofitInstance
import android.provider.Settings
import com.alperyuceer.komik_replikler.api.FavoriteRequest
import androidx.activity.OnBackPressedCallback


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var navView: NavigationView
    private val prefsName = "ReplikPrefs"
    private val playCountKey = "playCount"
    private val ratedKey = "hasRated"
    private val adScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var deviceId: String

    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
    }

    fun onReplikPlayed() {
        Log.d("RatingDebug", "onReplikPlayed called")
        incrementPlayCount()
    }

    private fun checkAndShowRatingDialog() {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val playCount = prefs.getInt(playCountKey, 0)
        val hasRated = prefs.getBoolean(ratedKey, false)
        
        Log.d("RatingDebug", "checkAndShowRatingDialog - playCount: $playCount, hasRated: $hasRated")
        
        if (playCount >= 10 && !hasRated) {
            Log.d("RatingDebug", "Showing rating dialog")
            showRatingDialog()
        }
    }

    private fun incrementPlayCount() {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val currentCount = prefs.getInt(playCountKey, 0)
        val newCount = currentCount + 1
        Log.d("RatingDebug", "incrementPlayCount - currentCount: $currentCount, newCount: $newCount")
        
        prefs.edit().putInt(playCountKey, newCount).apply()
        
        checkAndShowRatingDialog()
    }

    @SuppressLint("SetTextI18n", "HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Geri tuşu davranışını ayarla
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Reklam alanını başlangıçta gizle
        binding.adView.visibility = View.INVISIBLE

        // Initialize views
        drawerLayout = binding.drawerLayout
        topAppBar = binding.topAppBar
        navView = binding.navView

        // Navigation header'daki sürüm bilgisini ayarla
        val headerView = navView.getHeaderView(0)
        val versionText = headerView.findViewById<TextView>(R.id.versionText)
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        versionText.text = "Sürüm $versionName"

        // Set up TopAppBar
        setSupportActionBar(topAppBar)
        
        // Device ID'yi al
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        // SplashActivity'den gelen kategorileri al
        val kategoriler = intent.getStringArrayListExtra("KATEGORILER")
        if (kategoriler != null) {
            // Küfür filtresinin durumunu kontrol et
            val isKufurFiltresiActive = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getBoolean("kufur_filtresi", false)

            // Küfürlü kategorisini filtrele
            val filteredKategoriler = if (isKufurFiltresiActive) {
                kategoriler.filter { it != "Küfürlü" }
            } else {
                kategoriler
            }

            // TabLayout'u filtrelenmiş kategorilerle kur
            val adapter = TabPageAdapter(this, ArrayList(filteredKategoriler))
            binding.viewPager.adapter = adapter

            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.text = filteredKategoriler[position]
            }.attach()

            // Popüler kategorisine git
            val popIndex = filteredKategoriler.indexOfFirst { it.equals("Popüler", ignoreCase = true) }
            if (popIndex != -1) {
                binding.viewPager.currentItem = popIndex
            }
        }

        // Navigation Drawer kontrolü
        topAppBar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        // Navigation Drawer menu item tıklama olayları
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_replik_iste -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:") // only email apps should handle this
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("komikrepliklerapp@gmail.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Replik İsteği")
                        putExtra(Intent.EXTRA_TEXT, "Merhaba sizden bu repliği eklemenizi istiyorum: ")
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Mail uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.menu_puanla -> {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                    } catch (e: Exception) {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                    }
                    true
                }
                R.id.menu_paylas -> {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Komik Replikler uygulamasıyla çok eğleniyorum sen de katıl sen de eğlen!\n\nhttps://play.google.com/store/apps/details?id=$packageName")
                    }
                    startActivity(Intent.createChooser(shareIntent, "Uygulamayı Paylaş"))
                    true
                }
                R.id.menu_diger_uygulama -> {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://dev?id=6013975058200018384")))
                    } catch (e: Exception) {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=6013975058200018384")))
                    }
                    true
                }
                R.id.menu_hakkinda -> {
                    showAboutDialog()
                    true
                }
                else -> false
            }
        }

        // Küfür filtresi switch kontrolü
        val menu = navView.menu
        val menuItem = menu.findItem(R.id.menu_kufur_filtresi)
        val actionView = menuItem.actionView
        val kufurSwitch = actionView?.findViewById<SwitchMaterial>(R.id.kufurSwitch)
        val switchContainer = actionView?.findViewById<View>(R.id.switchContainer)

        // Switch'in durumunu değiştiren fonksiyon
        fun toggleSwitch() {
            kufurSwitch?.let {
                it.isChecked = !it.isChecked
                // Switch durumunu SharedPreferences'a kaydet
                getSharedPreferences("app_prefs", MODE_PRIVATE).edit().apply {
                    putBoolean("kufur_filtresi", it.isChecked)
                    apply()
                }
                // TabLayout'u güncelle
                setUpTabBar()
            }
        }

        // Switch container'a tıklama olayı
        switchContainer?.setOnClickListener {
            toggleSwitch()
        }

        // Switch'in kendi değişim olayı
        kufurSwitch?.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed) return@setOnCheckedChangeListener // Kullanıcı tıklaması değilse işlemi yapma
            
            // Switch durumunu SharedPreferences'a kaydet
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().apply {
                putBoolean("kufur_filtresi", isChecked)
                apply()
            }
            // TabLayout'u güncelle
            setUpTabBar()
        }

        // Switch'in önceki durumunu yükle
        val isKufurFiltresiActive = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getBoolean("kufur_filtresi", false)
        kufurSwitch?.isChecked = isKufurFiltresiActive

        checkAndRequestPermissions()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // Initialize ads after UI is ready
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000) // UI'nin hazır olması için kısa bir gecikme
            try {
                MobileAds.initialize(this@MainActivity) { _ ->
                    // Reklam SDK'sı başlatıldıktan sonra reklam yükle
                    loadAd()
                }
            } catch (e: Exception) {
                Log.e("AdMob", "Ad initialization error: ${e.message}")
            }
        }
    }

    private var adRetryCount = 0
    private val maxAdRetry = 3
    
    private fun loadAd() {
        if (adRetryCount >= maxAdRetry) {
            Log.w("AdMob", "Maximum ad retry count reached")
            return
        }
        
        val adRequest = AdRequest.Builder().build()
        binding.adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                adRetryCount = 0 // Başarılı yüklemede sayacı sıfırla
                // Reklam başarıyla yüklendiğinde göster
                binding.adView.visibility = View.VISIBLE
                // TabLayout'u reklamın üstüne taşı
                val params = binding.tabLayout.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.ABOVE, R.id.adView)
                params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                binding.tabLayout.layoutParams = params
            }
            
            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)
                Log.e("AdMob", "Ad failed to load: ${error.message}")
                
                // Reklam yüklenemezse tamamen gizle
                binding.adView.visibility = View.GONE
                // TabLayout'u en alta sabitle
                val params = binding.tabLayout.layoutParams as RelativeLayout.LayoutParams
                params.removeRule(RelativeLayout.ABOVE)
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                binding.tabLayout.layoutParams = params
                
                // Belirli bir süre sonra tekrar dene
                adRetryCount++
                if (adRetryCount < maxAdRetry) {
                    adScope.launch {
                        delay(5000) // 5 saniye bekle
                        loadAd() // Tekrar dene
                    }
                }
            }
        }
        
        // Reklam yüklemeyi başlat
        binding.adView.loadAd(adRequest)
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 ve üzeri için özel işlem gerekmez
        } else {
            // Android 9 ve altı için eski izin sistemini kullanalım
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // İzin verildi
                    Toast.makeText(this, "Depolama izni verildi", Toast.LENGTH_SHORT).show()
                } else {
                    // İzin reddedildi
                    Toast.makeText(this, "Depolama izni gerekli", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.custom_menu,menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (drawerLayout.isOpen) {
                    drawerLayout.close()
                } else {
                    drawerLayout.open()
                }
                return true
            }
            R.id.search -> {
                val intent = Intent(this,SearchActivity::class.java)
                startActivity(intent)
            }
            R.id.favorites -> {
                binding.viewPager.currentItem = 0
            }
            R.id.categories -> {
                showCategoriesDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }
    private fun setUpTabBar() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // API'den kategorileri al
                val response = RetrofitInstance.api.getKategoriler()
                if (response.isSuccessful) {
                    val kategoriler = response.body()
                    if (kategoriler != null) {
                        withContext(Dispatchers.Main) {
                            val adapter = TabPageAdapter(this@MainActivity, kategoriler)
                            binding.viewPager.adapter = adapter

                            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                                tab.text = kategoriler[position]
                            }.attach()

                            binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                                override fun onTabSelected(tab: TabLayout.Tab?) {
                                    binding.viewPager.currentItem = tab!!.position
                                }

                                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                                override fun onTabReselected(tab: TabLayout.Tab?) {}
                            })

                            // Varsayılan olarak Popüler sekmesini seç
                            binding.viewPager.currentItem = 1
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Kategoriler yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Kategoriler yüklenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCategoriesDialog() {
        val dialog = AlertDialog.Builder(this, R.style.Theme_MaterialComponents_Dialog)
            .create()
        
        val dialogBinding = DialogCategoriesBinding.inflate(layoutInflater)
        dialog.setView(dialogBinding.root)

        // API'den kategorileri al
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.getKategoriler()
                if (response.isSuccessful) {
                    var kategoriler = response.body()
                    if (kategoriler != null) {
                        // Küfür filtresinin durumunu kontrol et
                        val isKufurFiltresiActive = getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .getBoolean("kufur_filtresi", false)

                        // Küfürlü kategorisini filtrele
                        if (isKufurFiltresiActive) {
                            kategoriler = kategoriler.filter { it != "Küfürlü" }
                        }

                        // Kategorileri alfabetik olarak sırala
                        val siraliKategoriler = kategoriler.sortedBy { it }

                        withContext(Dispatchers.Main) {
                            dialogBinding.categoriesRecyclerView.apply {
                                layoutManager = GridLayoutManager(context, 2)
                                adapter = CategoryAdapter(siraliKategoriler) { category ->
                                    val index = (binding.viewPager.adapter as? TabPageAdapter)
                                        ?.getCategories()
                                        ?.indexOfFirst { it.equals(category, ignoreCase = true) } ?: -1
                                    
                                    if (index != -1) {
                                        binding.viewPager.currentItem = index
                                        dialog.dismiss()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Kategoriler yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Kategoriler yüklenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    @SuppressLint("SetTextI18n", "InflateParams")
    private fun showAboutDialog() {
        val dialog = Dialog(this, R.style.Theme_MaterialComponents_Dialog)
        val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)
        dialog.setContentView(dialogView)

        // Sürüm bilgisini ayarla
        val versionText = dialogView.findViewById<TextView>(R.id.appVersion)
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        versionText.text = "Sürüm $versionName"

        // İletişim butonuna tıklama olayı
        dialogView.findViewById<MaterialButton>(R.id.contactButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // only email apps should handle this
                putExtra(Intent.EXTRA_EMAIL, arrayOf("komikrepliklerapp@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Destek")
            }
            try {
                startActivity(intent)
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(this, "Mail uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showRatingDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_rating)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Dialog'un dışına tıklanarak kapatılmasını engelle
        dialog.setCanceledOnTouchOutside(false)
        // Geri tuşuyla kapatılmasını engelle
        dialog.setCancelable(false)
        
        val ratingBar = dialog.findViewById<RatingBar>(R.id.ratingBar)
        val btnLater = dialog.findViewById<Button>(R.id.btnLater)
        
        // Yıldız seçildiğinde direkt olarak Play Store'a yönlendir
        ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser && rating > 0) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    startActivity(intent)
                    // Kullanıcı puanladı olarak işaretle
                    getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(ratedKey, true)
                        .apply()
                    dialog.dismiss()
                } catch (e: ActivityNotFoundException) {
                    // Play Store bulunamazsa web sayfasına yönlendir
                    startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=$packageName")))
                    dialog.dismiss()
                }
            }
        }
        
        // "Daha Sonra" butonuna basıldığında sayacı sıfırla
        btnLater.setOnClickListener {
            // Sayacı sıfırla
            getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                .edit()
                .putInt(playCountKey, 0)
                .apply()
            dialog.dismiss()
        }
        
        dialog.show()
    }

    override fun onPause() {
        try {
            if (::binding.isInitialized) {
                binding.adView.pause()
            }
        } catch (e: Exception) {
            Log.e("AdMob", "onPause error: ${e.message}")
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        try {
            if (::binding.isInitialized) {
                binding.adView.resume()
            }
        } catch (e: Exception) {
            Log.e("AdMob", "onResume error: ${e.message}")
        }
    }

    override fun onDestroy() {
        try {
            adScope.cancel()
            if (::binding.isInitialized) {
                binding.adView.destroy()
            }
        } catch (e: Exception) {
            Log.e("AdMob", "onDestroy error: ${e.message}")
        }
        super.onDestroy()
    }

    // Favori ekleme/çıkarma işlemi için yeni fonksiyon
    fun updateFavorite(replik: Replik, isFavorite: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = FavoriteRequest(deviceId, replik.id)
                Log.d("FavoriteDebug", "Request: deviceId=${request.deviceId}, replikId=${request.replikId}")
                
                val response = if (isFavorite) {
                    RetrofitInstance.api.addToFavorites(request)
                } else {
                    RetrofitInstance.api.removeFromFavorites(request)
                }

                if (!response.isSuccessful) {
                    Log.e("FavoriteDebug", "Error Response: ${response.code()} - ${response.errorBody()?.string()}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Favori işlemi başarısız: ${response.code()} - ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.d("FavoriteDebug", "Success: ${response.body()}")
                }
            } catch (e: Exception) {
                Log.e("FavoriteDebug", "Exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Favori işlemi sırasında hata: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Yeni intent'i mevcut intent olarak ayarla
        setIntent(intent)
        
        // Yeni kategorileri al ve uygula
        val kategoriler = intent?.getStringArrayListExtra("KATEGORILER")
        if (kategoriler != null) {
            val adapter = TabPageAdapter(this, kategoriler)
            binding.viewPager.adapter = adapter

            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.text = kategoriler[position]
            }.attach()

            // Popüler kategorisine git
            val popIndex = kategoriler.indexOfFirst { it.equals("Popüler", ignoreCase = true) }
            if (popIndex != -1) {
                binding.viewPager.currentItem = popIndex
            }
        }
    }
}