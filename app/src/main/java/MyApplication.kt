package com.hananali.weatherapp

import android.app.Activity
import android.app.Application
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.appopen.AppOpenAd

class MyApplication : Application() {

    private var appOpenAd: AppOpenAd? = null
    private var isAdShowing = false

    override fun onCreate() {
        super.onCreate()
        loadAppOpenAd()
    }

    private fun loadAppOpenAd() {
        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            this, "ca-app-pub-6586396850430917/8901983646", adRequest,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                }

                override fun onAdFailedToLoad(loadAdError: com.google.android.gms.ads.LoadAdError) {
                    appOpenAd = null
                }
            }
        )
    }

    fun showAppOpenAdIfAvailable(activity: Activity) {
        if (appOpenAd != null && !isAdShowing) {
            isAdShowing = true
            appOpenAd?.show(activity)
        } else {
            loadAppOpenAd() // Ensure the ad is loaded for future display
        }
    }
}
