package com.myanmar.twodthreed

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

/** Initializes Google Mobile Ads and reserves a bottom banner area in the app UI. */
class AdsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity is MainActivity) attachBanner(activity)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

            override fun onActivityDestroyed(activity: Activity) {
                val content = activity.findViewById<FrameLayout?>(android.R.id.content)
                (content?.getTag(BANNER_TAG_KEY) as? AdView)?.destroy()
            }
        })
    }

    private fun attachBanner(activity: Activity) {
        val content = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        if (content.getTag(CONTAINER_TAG_KEY) == true) return

        val density = activity.resources.displayMetrics.density
        val widthDp = (activity.resources.displayMetrics.widthPixels / density).toInt()
        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp)
        val banner = AdView(activity).apply {
            setAdSize(adSize)
            adUnitId = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
            loadAd(AdRequest.Builder().build())
        }
        val bannerHeightPx = adSize.getHeightInPixels(activity)
        val container = FrameLayout(activity).apply { setTag(CONTAINER_TAG_KEY, true) }
        val oldChildren = (0 until content.childCount).map { content.getChildAt(it) }
        content.removeAllViews()
        content.addView(container, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        oldChildren.forEach { child ->
            (child.parent as? ViewGroup)?.removeView(child)
            container.addView(child, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.TOP
            ).apply { bottomMargin = bannerHeightPx })
        }
        container.addView(banner, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            bannerHeightPx,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ))
        content.setTag(BANNER_TAG_KEY, banner)
    }

    private companion object {
        // Stable, app-specific keyed view tags (IDs need not be resource IDs).
        const val CONTAINER_TAG_KEY = 0x7f0b00a1
        const val BANNER_TAG_KEY = 0x7f0b00a2
    }
}
