package com.kunpitech.zyvixa.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobManager {
    private const val TAG = "AdMobManager"
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    // AdMob Official Test Rewarded Ad Unit ID
    private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    fun initialize(context: Context) {
        MobileAds.initialize(context) { status ->
            Log.d(TAG, "AdMob Initialized: $status")
        }
        loadRewardedAd(context)
    }

    fun loadRewardedAd(context: Context) {
        if (rewardedAd != null || isLoading) {
            Log.d(TAG, "Ad already loaded or currently loading. Skipping request.")
            return
        }
        isLoading = true
        Log.d(TAG, "Loading rewarded ad...")

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Failed to load rewarded ad: ${adError.message}")
                rewardedAd = null
                isLoading = false
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Rewarded ad loaded successfully.")
                rewardedAd = ad
                isLoading = false
            }
        })
    }

    fun isAdLoaded(): Boolean {
        return rewardedAd != null
    }

    fun showRewardedAd(activity: Activity, onAdDismissed: (Boolean) -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            var rewardEarned = false
            
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed by user. Reward earned status: $rewardEarned")
                    rewardedAd = null
                    loadRewardedAd(activity)
                    onAdDismissed(rewardEarned)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Ad failed to show: ${adError.message}")
                    rewardedAd = null
                    loadRewardedAd(activity)
                    // If ad fails to show, reward them anyway to prevent blocking
                    onAdDismissed(true)
                }
            }

            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User completed watching ad. Reward: ${rewardItem.amount} ${rewardItem.type}")
                rewardEarned = true
            }
        } else {
            Log.w(TAG, "Rewarded ad not loaded yet. Loading and letting user proceed.")
            loadRewardedAd(activity)
            // Proceed with execution directly so user is not blocked if connection is slow
            onAdDismissed(true)
        }
    }
}
