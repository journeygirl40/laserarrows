package com.laserarrows.app.ads

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

/**
 * 復活(ライフ全回復リトライ)用のリワード広告の読み込み・表示を扱う。
 * AdMobアカウント/広告ユニットが未発行のため、現在はGoogle公式のテスト用リワード広告ユニットIDを使用している。
 * 本番配信前に実際の広告ユニットIDへ差し替えること。
 */
class RewardedAdManager(context: Context) {
    private val appContext = context.applicationContext
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    init {
        MobileAds.initialize(appContext)
        loadAd()
    }

    fun isReady(): Boolean = rewardedAd != null

    /** 広告を表示する。読み込みが済んでいなければ視聴不可として扱い、裏で次回分の読み込みを試みる。 */
    fun show(activity: Activity, onRewarded: () -> Unit, onUnavailable: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            onUnavailable()
            loadAd()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                loadAd()
            }
        }
        ad.show(activity) { onRewarded() }
    }

    private fun loadAd() {
        if (isLoading || rewardedAd != null) return
        isLoading = true
        RewardedAd.load(
            appContext,
            TEST_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoading = false
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    rewardedAd = null
                    Log.w(TAG, "リワード広告の読み込みに失敗しました: ${error.message}")
                }
            },
        )
    }

    companion object {
        private const val TAG = "RewardedAdManager"

        // AdMobアカウント未設定のため、Google公式のテスト用リワード広告ユニットID(Android)を使用。
        const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }
}
