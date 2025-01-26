# Komik Replikler Android Uygulaması

Bu uygulama, popüler dizi ve filmlerden komik replikleri kategorize eden, oynatmaya ve paylaşmaya olanak sağlayan bir Android uygulamasıdır.

## 📱 Uygulama Özellikleri

- Replikleri kategorilere göre filtreleme
- Favori replikleri kaydetmee
- Replikleri paylaşma
- Replik arama
- Karanlık tema desteği
- Özel font kullanımı

## 🏗️ Proje Yapısı
alper
### 📂 Temel Dosyalar ve Görevleri

#### Veri Modeli ve Veritabanı
- `Replik.kt`: Ana veri modeli sınıfı
  - Replik ismi, yolu, kategorileri ve favori durumunu içerir
  - Room Entity olarak işaretlenmiş
  
- `ReplikDatabase.kt`: Room veritabanı tanımı
  - Veritabanı konfigürasyonu
  - Singleton pattern kullanımı

- `ReplikDao.kt`: Veritabanı işlemleri
  - CRUD operasyonları
  - RxJava3 ile reaktif sorgular
  - Kategori filtreleme
  - Arama fonksiyonları
  - Favori işlemleri

#### Adaptörler
- `ReplikAdapter.kt`: Replik listesi görünümü
  - Ses dosyası oynatma/durdurma
  - Favori ekleme/çıkarma
  - Replik paylaşımı
  - MediaPlayer yönetimi

- `TabPageAdapter.kt`: Kategori sekmeleri yönetimi
  - ViewPager2 entegrasyonu
  - Fragment yönetimi

- `CategoryAdapter.kt`: Kategori listesi görünümü
  - Kategori seçimi
  - Tıklama olayları

#### Aktiviteler
- `MainActivity.kt`: Ana uygulama ekranı
  - Karanlık tema ayarı
  - NavigationView (yan menü) yönetimi
  - TabLayout ile kategori sekmelerini gösterme
  - Depolama izinleri yönetimi
  - Veritabanı güncelleme işlemleri

- `SearchActivity.kt`: Arama ekranı
  - Gerçek zamanlı arama
  - Coroutines kullanımı
  - Boş sonuç durumu yönetimi

#### Yardımcı Sınıflar
- `KategoriConverter.kt`: Veritabanı tip dönüşümleri
  - String <-> List<String> dönüşümleri
  - Room TypeConverter işlemleri

## 🛠️ Teknik Detaylar

### Kullanılan Teknolojiler
- Room: Veritabanı işlemleri
- RxJava3: Reaktif programlama
- Coroutines: Asenkron işlemler
- ViewBinding: View erişimi
- ViewPager2: Sayfa kaydırma
- RecyclerView: Liste görünümleri
- MediaPlayer: Ses dosyası yönetimi
- FileProvider: Dosya paylaşımı

### Veritabanı Şeması
```kotlin
@Entity
data class Replik(
    @ColumnInfo("replik_ismi") val replikIsmi: String,
    @ColumnInfo("replik_yolu") val replikYolu: Int,
    @ColumnInfo("replik_kategori") val kategori: List<String>,
    @ColumnInfo("favorimi") var favorimi: Boolean
) {
    @PrimaryKey(autoGenerate = true) var id = 0
}
```

## 📝 Geliştirici Notları

### Yeni Özellik Eklerken
1. Veri modelinde değişiklik gerekiyorsa:
   - `Replik.kt`'de modeli güncelle
   - `ReplikDao.kt`'de gerekli sorguları ekle
   - `ReplikDatabase.kt`'de versiyon numarasını artır

2. UI değişiklikleri için:
   - İlgili layout XML dosyasını güncelle
   - Adapter sınıflarında gerekli değişiklikleri yap
   - ViewBinding referanslarını güncelle

3. Yeni kategori eklerken:
   - `MainActivity.kt`'deki kategori listesini güncelle
   - Gerekli ses dosyalarını `raw` klasörüne ekle
   - `ReplikData.kt`'de yeni replikleri tanımla

### Dikkat Edilmesi Gerekenler
- MediaPlayer kaynaklarının doğru yönetimi
- Veritabanı işlemlerinin arka planda yapılması
- Depolama izinlerinin kontrolü
- Ses dosyalarının optimizasyonu
- UI thread'in bloklanmaması

## 🔄 Veri Akışı
1. Uygulama başlatıldığında:
   - Veritabanı kontrolü ve güncelleme
   - Kategori listesi oluşturma
   - TabLayout ve ViewPager2 hazırlama

2. Replik listesi gösterilirken:
   - RxJava ile veritabanından veri çekme
   - RecyclerView adapter'ına veri aktarma
   - Görünüm güncelleme

3. Arama yapılırken:
   - Coroutines ile arka plan işlemi
   - Veritabanında filtreleme
   - UI güncelleme

## 📦 Gereksinimler
- Android Studio Arctic Fox veya üzeri
- Minimum SDK: API 21 (Android 5.0)
- Target SDK: API 33 (Android 13)
- Kotlin 1.8.0 veya üzeri

## 🔧 Kurulum
1. Projeyi klonlayın
2. Android Studio'da açın
3. Gradle sync işlemini tamamlayın
4. Uygulamayı çalıştırın

## 📚 Kullanılan Kütüphaneler
```gradle
dependencies {
    // Room
    implementation "androidx.room:room-runtime:$room_version"
    implementation "androidx.room:room-rxjava3:$room_version"
    kapt "androidx.room:room-compiler:$room_version"

    // RxJava
    implementation "io.reactivex.rxjava3:rxjava:$rxjava_version"
    implementation "io.reactivex.rxjava3:rxandroid:$rxandroid_version"

    // Material Design
    implementation "com.google.android.material:material:$material_version"

    // ViewBinding
    implementation "androidx.viewbinding:viewbinding:$viewbinding_version"
}
``` 