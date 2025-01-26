package com.alperyuceer.komik_replikler

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alperyuceer.komik_replikler.api.RetrofitInstance
import com.alperyuceer.komik_replikler.databinding.ActivitySplashBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Context
import androidx.appcompat.app.AlertDialog
import android.net.Proxy
import android.os.Build
import android.provider.Settings

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!isInternetAvailable()) {
            showNoInternetDialog()
            return
        }

        if (isProxyEnabled()) {
            showProxyDialog()
            return
        }

        // Kategorileri yükle
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.getKategoriler()
                if (response.isSuccessful) {
                    val kategoriler = response.body()
                    if (kategoriler != null) {
                        withContext(Dispatchers.Main) {
                            val intent = Intent(this@SplashActivity, MainActivity::class.java)
                            intent.putStringArrayListExtra("KATEGORILER", ArrayList(kategoriler))
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        throw Exception("Kategoriler boş")
                    }
                } else {
                    throw Exception("Kategoriler yüklenemedi")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SplashActivity, "Bağlantı hatası: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun isProxyEnabled(): Boolean {
        // Android 4.0 ve üzeri için proxy ayarlarını kontrol et
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            val proxyHost = System.getProperty("http.proxyHost")
            val proxyPort = System.getProperty("http.proxyPort")
            if (!proxyHost.isNullOrEmpty() && !proxyPort.isNullOrEmpty()) {
                return true
            }
        }

        // Global proxy ayarlarını kontrol et
        try {
            val host = Settings.Global.getString(contentResolver, Settings.Global.HTTP_PROXY)
            if (!host.isNullOrEmpty()) {
                return true
            }
        } catch (e: Exception) {
            // Bazı cihazlarda bu ayar olmayabilir
        }

        // VPN bağlantısını kontrol et
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.getNetworkCapabilities(cm.activeNetwork)?.let { capabilities ->
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    return true
                }
            }
        }

        return false
    }

    private fun showProxyDialog() {
        AlertDialog.Builder(this)
            .setTitle("Proxy Tespit Edildi")
            .setMessage("Güvenlik nedeniyle, proxy veya VPN kullanımı desteklenmemektedir. Lütfen proxy/VPN bağlantınızı kapatıp tekrar deneyin.")
            .setPositiveButton("Tekrar Dene") { _, _ ->
                if (!isProxyEnabled()) {
                    recreate()
                } else {
                    showProxyDialog()
                }
            }
            .setNegativeButton("Çıkış") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }

    private fun showNoInternetDialog() {
        AlertDialog.Builder(this)
            .setTitle("İnternet Bağlantısı Yok")
            .setMessage("Uygulamayı kullanabilmek için internet bağlantısı gerekiyor. Lütfen internet bağlantınızı kontrol edip tekrar deneyin.")
            .setPositiveButton("Tekrar Dene") { _, _ ->
                if (isInternetAvailable()) {
                    recreate()
                } else {
                    showNoInternetDialog()
                }
            }
            .setNegativeButton("Çıkış") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
} 