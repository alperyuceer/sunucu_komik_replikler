package com.alperyuceer.komik_replikler

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.alperyuceer.komik_replikler.api.RetrofitInstance
import com.alperyuceer.komik_replikler.databinding.RecyclerRowBinding
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

private var exoPlayer: ExoPlayer? = null
private var currentlyPlayingReplikName: String? = null

class ReplikAdapter(
    private var replikList: List<Replik>,
    private val category: String
) : RecyclerView.Adapter<ReplikAdapter.ReplikHolder>() {

    interface OnListEmptyListener {
        fun onListEmpty()
    }

    private var onListEmptyListener: OnListEmptyListener? = null

    fun setOnListEmptyListener(listener: OnListEmptyListener) {
        onListEmptyListener = listener
    }

    interface OnReplikPlayedListener {
        fun onReplikPlayed()
    }

    private var onReplikPlayedListener: OnReplikPlayedListener? = null

    fun setOnReplikPlayedListener(listener: OnReplikPlayedListener) {
        onReplikPlayedListener = listener
    }

    class ReplikHolder(val binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplikHolder {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReplikHolder(binding)
    }

    override fun getItemCount(): Int {
        return replikList.size
    }

    private fun shareAudioFile(context: Context, replik: Replik) {
        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("$BASE_URL/audio/${replik.sesDosyasi}")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Ses dosyası indirilemedi")

                    val audioData = response.body?.bytes() ?: throw Exception("Ses verisi boş")
                    
                    // Cache dizininde geçici dosya oluştur
                    val cacheFile = File(context.cacheDir, "${replik.baslik}.ogg")
                    
                    // Veriyi cache dosyasına yaz
                    FileOutputStream(cacheFile).use { output ->
                        output.write(audioData)
                    }

                    // FileProvider URI oluştur
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        cacheFile
                    )

                    // UI thread'inde paylaşım dialog'unu göster
                    context.startActivity(Intent.createChooser(Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "audio/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }, "Sesi Paylaş"))

                    // Geçici dosyayı sil
                    cacheFile.deleteOnExit()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // UI thread'inde hata mesajını göster
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, "Paylaşım sırasında bir hata oluştu", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onBindViewHolder(holder: ReplikHolder, position: Int) {
        val replik = replikList[position]

        holder.binding.replikView.text = replik.baslik ?: "İsimsiz Replik"
        
        holder.binding.favoriyeEkleButonu.setIconResource(
            if (replik.favorimi) R.drawable.ic_favorite_light else R.drawable.ic_favorite_border
        )

        // Çalan repliği vurgula
        if (replik.baslik == currentlyPlayingReplikName) {
            holder.binding.cardView.setCardBackgroundColor(holder.itemView.context.getColor(R.color.playing_background))
        } else {
            holder.binding.cardView.setCardBackgroundColor(holder.itemView.context.getColor(R.color.card_background))
        }

        fun sesOynat(holder: ReplikHolder, sesDosyasi: String?) {
            if (sesDosyasi == null) {
                Toast.makeText(holder.itemView.context, "Ses dosyası bulunamadı", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val context = holder.itemView.context
                Log.d("ReplikDebug", "Ses çalınıyor: $sesDosyasi")
                
                // Eğer aynı replik çalıyorsa durdur
                if (currentlyPlayingReplikName == replikList[holder.bindingAdapterPosition].baslik) {
                    exoPlayer?.stop()
                    exoPlayer?.release()
                    exoPlayer = null
                    currentlyPlayingReplikName = null
                    holder.binding.cardView.setCardBackgroundColor(context.getColor(R.color.card_background))
                    return
                }

                // Önceki ExoPlayer'ı temizle
                exoPlayer?.release()
                
                // Yeni ExoPlayer oluştur
                exoPlayer = ExoPlayer.Builder(context).build()
                
                // Ses URL'ini oluştur ve MediaItem'a dönüştür
                val mediaUrl = "$BASE_URL/audio/$sesDosyasi"
                Log.d("ReplikDebug", "Ses URL: $mediaUrl")
                val mediaItem = MediaItem.fromUri(mediaUrl)
                exoPlayer?.setMediaItem(mediaItem)
                
                // Önceki çalan repliğin arka plan rengini sıfırla
                currentlyPlayingReplikName?.let { previousReplik ->
                    val previousPosition = replikList.indexOfFirst { it.baslik == previousReplik }
                    if (previousPosition != -1) {
                        notifyItemChanged(previousPosition)
                    }
                }

                // Yeni çalan repliği kaydet ve arka planını değiştir
                currentlyPlayingReplikName = replikList[holder.bindingAdapterPosition].baslik
                holder.binding.cardView.setCardBackgroundColor(context.getColor(R.color.playing_background))

                // Player listener'ları ayarla
                exoPlayer?.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            currentlyPlayingReplikName = null
                            holder.binding.cardView.setCardBackgroundColor(context.getColor(R.color.card_background))
                            exoPlayer?.release()
                            exoPlayer = null
                        }
                    }
                })

                // Sesi çal
                exoPlayer?.prepare()
                exoPlayer?.play()

                // Oynatma sayacını artır
                onReplikPlayedListener?.onReplikPlayed()

                // Oynatma sayısını artır
                val playingReplik = replikList[holder.bindingAdapterPosition]
                Log.d("ReplikDebug", "Oynatma sayısı artırılıyor. Replik ID: ${playingReplik.id}")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = RetrofitInstance.api.incrementPlayCount(playingReplik.id)
                        if (!response.isSuccessful) {
                            Log.e("ReplikDebug", "Oynatma sayısı güncellenemedi: ${response.code()}")
                        } else {
                            Log.d("ReplikDebug", "Oynatma sayısı başarıyla güncellendi")
                        }
                    } catch (e: Exception) {
                        Log.e("ReplikDebug", "Oynatma sayısı güncelleme hatası: ${e.message}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e("ReplikDebug", "Ses oynatma hatası: ${e.message}", e)
                Toast.makeText(holder.itemView.context, "Ses dosyası oynatılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Paylaş ikonuna tıklama
        holder.binding.shareIcon.setOnClickListener {
            if (replikList[position].sesDosyasi == null) {
                Toast.makeText(holder.itemView.context, "Paylaşılacak ses dosyası bulunamadı", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            shareAudioFile(holder.itemView.context, replikList[position])
        }

        // Kartın tamamına tıklama olayı
        holder.binding.cardView.setOnClickListener {
            sesOynat(holder, replikList[position].sesDosyasi)
        }

        // Favorilere ekleme butonuna tıklama
        holder.binding.favoriyeEkleButonu.setOnClickListener {
            val newFavoriteState = !replik.favorimi
            replik.favorimi = newFavoriteState
            
            // API'ye bildir
            when (val context = holder.itemView.context) {
                is MainActivity -> context.updateFavorite(replik, newFavoriteState)
                is SearchActivity -> context.updateFavorite(replik, newFavoriteState)
            }

            if (category == "favoriler" && !newFavoriteState) {
                val mutableList = replikList.toMutableList()
                mutableList.removeAt(position)
                replikList = mutableList
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, replikList.size)
                
                // Liste boşsa listener'ı çağır
                if (replikList.isEmpty()) {
                    onListEmptyListener?.onListEmpty()
                }
            } else {
                notifyItemChanged(position)
            }
        }
    }

    companion object {
        private const val BASE_URL = "http://138.68.111.170:3000"
    }
}