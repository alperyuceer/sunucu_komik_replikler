package com.alperyuceer.komik_replikler

import com.google.gson.annotations.SerializedName

data class Replik(
    @SerializedName("_id")
    val id: String,

    @SerializedName("baslik")
    val baslik: String?,

    @SerializedName("sesDosyasi")
    val sesDosyasi: String?,

    @SerializedName("kategoriler")
    val kategoriler: List<String>,

    var favorimi: Boolean = false,

    @SerializedName("eklemeTarihi")
    val eklemeTarihi: String? = null,

    @SerializedName("oynatmaSayisi")
    val oynatmaSayisi: Int = 0
)

