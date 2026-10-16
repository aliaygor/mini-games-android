package com.thirtyseconds.platform

import android.app.Activity
import android.os.SystemClock
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.*
import com.google.android.gms.ads.rewarded.*
import com.google.android.ump.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdController(private val rewardedId: String, private val interstitialId: String) {
    private var rewarded: RewardedAd? = null
    private var interstitial: InterstitialAd? = null
    private var initialized = false
    private var showing = false
    private var rewardedLoading = false
    private var interstitialLoading = false
    private var lastLoadAttempt = -30_000L
    private var lastAd = SystemClock.elapsedRealtime()
    private val ready = MutableStateFlow(false)
    val rewardReady = ready.asStateFlow()
    private var consent: ConsentInformation? = null
    fun requestConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consent = info
        info.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { initializeIfAllowed(activity) }
        }, { initializeIfAllowed(activity) })
    }
    fun privacy(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            if (consent?.canRequestAds() != true) { rewarded = null; interstitial = null; ready.value = false }
            initializeIfAllowed(activity)
        }
    }
    fun privacyRequired() = consent?.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    private fun initializeIfAllowed(activity: Activity) {
        if (consent?.canRequestAds() != true) return
        if (initialized) { preload(activity); return }
        initialized = true
        MobileAds.initialize(activity.applicationContext) { activity.runOnUiThread { load(activity) } }
    }
    fun preload(activity: Activity) {
        if (!initialized || showing || SystemClock.elapsedRealtime() - lastLoadAttempt < 30_000) return
        load(activity)
    }
    private fun load(activity: Activity) {
        if (consent?.canRequestAds() != true) return
        val context = activity.applicationContext
        lastLoadAttempt = SystemClock.elapsedRealtime()
        if (rewarded == null && !rewardedLoading) {
        rewardedLoading = true
        RewardedAd.load(context, rewardedId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewardedLoading = false; if (consent?.canRequestAds() == true) { rewarded = ad; ready.value = true } }
            override fun onAdFailedToLoad(error: LoadAdError) { rewardedLoading = false; rewarded = null; ready.value = false }
        })
        }
        if (interstitial == null && !interstitialLoading) {
        interstitialLoading = true
        InterstitialAd.load(context, interstitialId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitialLoading = false; if (consent?.canRequestAds() == true) interstitial = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { interstitialLoading = false; interstitial = null }
        })
        }
    }
    fun reward(activity: Activity, onReward: () -> Unit, onDone: () -> Unit) {
        val ad = rewarded
        if (ad == null || showing || consent?.canRequestAds() != true) { onDone(); return }
        showing = true; rewarded = null; ready.value = false
        var granted = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { complete(activity, onDone) }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { complete(activity, onDone) }
        }
        ad.show(activity) { if (!granted) { granted = true; onReward() } }
    }
    fun transition(activity: Activity, completedGames: Int, onDone: () -> Unit) {
        val ad = interstitial
        if (ad == null || showing || completedGames < 4 || completedGames % 4 != 0 ||
            SystemClock.elapsedRealtime() - lastAd < 180_000 || consent?.canRequestAds() != true) { onDone(); return }
        showing = true; interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { complete(activity, onDone) }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { complete(activity, onDone) }
        }
        ad.show(activity)
    }
    private fun complete(activity: Activity, done: () -> Unit) {
        showing = false; lastAd = SystemClock.elapsedRealtime(); load(activity); done()
    }
}
