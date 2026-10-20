package com.imaegoo.memoswatch

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.abs

class DetailActivity : Activity() {

    companion object {
        const val EXTRA_TEXT = "text"
        const val EXTRA_TIME = "time"
    }

    private var downX = 0f
    private var downY = 0f
    private var trackingBack = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val side = minOf(
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels
        )
        val inset = (side * 0.1465f).toInt() + dp(2f)

        val memoText = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        val timeMillis = intent.getLongExtra(EXTRA_TIME, 0L)

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        val contentColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(inset, inset, inset, inset)
        }

        val timeView = TextView(this).apply {
            setText(fullTime(timeMillis))
            setTextColor(0x8AFFFFFF.toInt())
            textSize = 10.5f
            includeFontPadding = false
        }
        contentColumn.addView(
            timeView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val bodyView = TextView(this).apply {
            setText(memoText)
            setTextColor(Color.WHITE)
            textSize = 15f
            setLineSpacing(dp(3f).toFloat(), 1f)
            includeFontPadding = false
        }
        contentColumn.addView(
            bodyView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(7f) }
        )

        root.addView(
            ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                addView(
                    contentColumn,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)

        window.setDecorFitsSystemWindows(false)
        window.decorView.windowInsetsController?.apply {
            hide(WindowInsets.Type.systemBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // Swipe right from the left edge to go back, like the system back gesture.
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                trackingBack = ev.x <= dp(48f)
            }
            MotionEvent.ACTION_MOVE -> if (trackingBack) {
                val dx = ev.x - downX
                if (dx > dp(64f) && abs(ev.y - downY) < dx * 0.8f) {
                    trackingBack = false
                    finish()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> trackingBack = false
        }
        return super.dispatchTouchEvent(ev)
    }
}
