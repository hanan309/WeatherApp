package com.hananali.weatherapp

import android.os.Bundle
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.hananali.weatherapp.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bannerAdView: AdView
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var nativeAd: NativeAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Google Mobile Ads SDK
        MobileAds.initialize(this) {}

        // Setup Banner Ad
        bannerAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        bannerAdView.loadAd(adRequest)

        // Load Interstitial Ad
        loadInterstitialAd(adRequest)

        // Load Rewarded Ad
        loadRewardedAd(adRequest)

        // Load Native Ad
        loadNativeAd(adRequest)

        // Fetch weather data
        fetchWeatherData("Lahore")  // Default city
        setupSearchView()
    }
    override fun onResume() {
        super.onResume()
        (application as MyApplication).showAppOpenAdIfAvailable(this)
    }

    private fun setupSearchView() {
        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { fetchWeatherData(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    // Load Interstitial Ad
    private fun loadInterstitialAd(adRequest: AdRequest) {
        InterstitialAd.load(this, "ca-app-pub-6586396850430917/7777942968", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    showInterstitialAd()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }
            })
    }

    private fun showInterstitialAd() {
        interstitialAd?.show(this)
    }

    // Load Rewarded Ad
    private fun loadRewardedAd(adRequest: AdRequest) {
        RewardedAd.load(this, "ca-app-pub-6586396850430917/3673707696", adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    showRewardedAd()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                }
            })
    }

    private fun showRewardedAd() {
        rewardedAd?.show(this) { rewardItem ->
            val rewardAmount = rewardItem.amount
            val rewardType = rewardItem.type
            Toast.makeText(this, "Rewarded with $rewardAmount $rewardType", Toast.LENGTH_LONG).show()
        }
    }

    // Load Native Ad
    private fun loadNativeAd(adRequest: AdRequest) {
        val adLoader = AdLoader.Builder(this, "ca-app-pub-6586396850430917/3482136005")
            .forNativeAd { ad ->
                nativeAd = ad
                // Inflate your native ad layout and populate it here
                populateNativeAdView(ad)
            }
            .build()
        adLoader.loadAd(adRequest)
    }

    private fun populateNativeAdView(nativeAd: NativeAd) {
        val adView = findViewById<NativeAdView>(R.id.nativeAdView)
        // Populate NativeAdView with ad content
        // e.g., adView.headlineView.text = nativeAd.headline
        adView.setNativeAd(nativeAd)
    }

    private fun fetchWeatherData(cityName: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterface::class.java)

        val response = retrofit.getWeatherData(cityName, "73df4f72b0279f1745ed177ce1df1775", "matrix")
        response.enqueue(object : Callback<WeatherApp> {
            override fun onResponse(call: Call<WeatherApp>, response: Response<WeatherApp>) {
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    updateUI(responseBody, cityName)
                }
            }

            override fun onFailure(call: Call<WeatherApp>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Failed to fetch data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUI(weatherData: WeatherApp, cityName: String) {
        val tempInCelsius = weatherData.main.temp - 273.15
        val tempMinInCelsius = weatherData.main.temp_min - 273.15
        val tempMaxInCelsius = weatherData.main.temp_max - 273.15
        val humidity = weatherData.main.humidity
        val windSpeed = weatherData.wind.speed
        val sunrise = weatherData.sys.sunrise.toLong()
        val sunset = weatherData.sys.sunset.toLong()
        val seaLevel = weatherData.main.pressure
        val condition = weatherData.weather.firstOrNull()?.main ?: "unknown"

        binding.temp.text = String.format("%.2f°C", tempInCelsius)
        binding.maxTemp.text = "Max Temp: ${String.format("%.2f°C", tempMaxInCelsius)}"
        binding.minTemp.text = "Min Temp: ${String.format("%.2f°C", tempMinInCelsius)}"
        binding.weather.text = condition
        binding.humidity.text = "${humidity}%"
        binding.windSpeed.text = "${windSpeed}m/s"
        binding.sunrise.text = time(sunrise)
        binding.sunset.text = time(sunset)
        binding.sea.text = "${seaLevel}hPa"
        binding.condition.text = condition
        binding.day.text = dayName(System.currentTimeMillis())
        binding.date.text = date()
        binding.cityName.text = cityName

        val isNight = isNightTime(sunrise, sunset)
        changeImagesAccordingToWeatherCondition(condition, isNight)
    }

    // ... (rest of the functions remain unchanged)


private fun date(): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun time(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }

    private fun isNightTime(sunrise: Long, sunset: Long): Boolean {
        val currentTime = System.currentTimeMillis() / 1000  // Convert to seconds
        return currentTime < sunrise || currentTime > sunset
    }

    private fun changeImagesAccordingToWeatherCondition(condition: String, isNight: Boolean) {
        if (isNight) {
            when (condition) {
                "Clear Sky", "Sunny", "Clear" -> {
                    binding.root.setBackgroundResource(R.drawable.clear_night)
                    binding.lottieAnimationView.setAnimation(R.raw.moon)
                }
                "Partly Clouds", "Clouds", "Overcast", "Mist", "Foggy" -> {
                    binding.root.setBackgroundResource(R.drawable.cloud_night_bg)
                    binding.lottieAnimationView.setAnimation(R.raw.night_cloud)
                }
                "Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain" -> {
                    binding.root.setBackgroundResource(R.drawable.rainy_night_bg)
                    binding.lottieAnimationView.setAnimation(R.raw.night_rain)
                }
                "Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard" -> {
                    binding.root.setBackgroundResource(R.drawable.snow_night_bg)
                    binding.lottieAnimationView.setAnimation(R.raw.night_snow)
                }
                else -> {
                    binding.root.setBackgroundResource(R.drawable.clear_night)
                    binding.lottieAnimationView.setAnimation(R.raw.moon)
                }
            }
        } else {
            when (condition) {
                "Clear Sky", "Sunny", "Clear" -> {
                    binding.root.setBackgroundResource(R.drawable.sunny_bg)
                    binding.lottieAnimationView.setAnimation(R.raw.sun)
                }
                "Partly Clouds", "Clouds", "Overcast", "Mist", "Foggy" -> {
                    binding.root.setBackgroundResource(R.drawable.cloudy_bg)
                    binding.lottieAnimationView.setAnimation(R.raw.cloud)
                }
                "Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain" -> {
                    binding.root.setBackgroundResource(R.drawable.rainy_bg)
                    binding.lottieAnimationView.setAnimation(R.raw.rain)
                }
                "Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard" -> {
                    binding.root.setBackgroundResource(R.drawable.snowy_bg)
                    binding.lottieAnimationView.setAnimation(R.raw.snow)
                }
                else -> {
                    binding.root.setBackgroundResource(R.drawable.sunny_bg)
                    binding.lottieAnimationView.setAnimation(R.raw.sun)
                }
            }
        }
    }

    private fun dayName(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
