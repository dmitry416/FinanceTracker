package com.example.finance_tracker

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.progressindicator.LinearProgressIndicator

class SplashActivity : AppCompatActivity() {

    private lateinit var splashAnimation: LottieAnimationView
    private lateinit var appNameText: TextView
    private lateinit var taglineText: TextView
    private lateinit var loadingIndicator: LinearProgressIndicator
    private lateinit var versionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        initializeViews()
        startAnimations()
    }

    private fun initializeViews() {
        splashAnimation = findViewById(R.id.splashAnimation)
        appNameText = findViewById(R.id.appNameText)
        taglineText = findViewById(R.id.taglineText)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        versionText = findViewById(R.id.versionText)

        // Set version name from build config
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            versionText.text = "Version ${packageInfo.versionName}"
        } catch (e: Exception) {
            versionText.text = "Version 1.0.0"
        }
    }

    private fun startAnimations() {
        // Start Lottie animation
        splashAnimation.apply {
            setAnimation(R.raw.animation)
            playAnimation()
        }

        // Fade in animations
        val appNameAnim = createFadeInAnimation(appNameText, 500, 300)
        val taglineAnim = createFadeInAnimation(taglineText, 500, 500)
        val loadingAnim = createFadeInAnimation(loadingIndicator, 500, 700)
        val versionAnim = createFadeInAnimation(versionText, 500, 1000)

        // Combine animations
        AnimatorSet().apply {
            play(appNameAnim)
            play(taglineAnim).after(appNameAnim)
            play(loadingAnim).after(taglineAnim)
            play(versionAnim).after(loadingAnim)
            start()
        }

        // Navigate to login after animation completion
        splashAnimation.addAnimatorListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Delay for a moment after animation ends
                loadingIndicator.postDelayed({
                    navigateToLogin()
                }, 2000)
            }
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
    }

    private fun createFadeInAnimation(view: View, duration: Long, delay: Long): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
            this.duration = duration
            this.startDelay = delay
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

}