package com.imaegoo.memoswatch

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.text.StaticLayout
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var listScroll: ScrollView
    private lateinit var listContainer: LinearLayout
    private lateinit var statusView: TextView

    private var contentWidth = 0
    private var firstStart = true
    private var returningFromDetail = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(buildUi())
        hideSystemBars()
        load()
    }

    override fun onStart() {
        super.onStart()
        if (firstStart) {
            firstStart = false
            return
        }
        if (!returningFromDetail) load()
        returningFromDetail = false
    }

    private fun hideSystemBars() {
        window.setDecorFitsSystemWindows(false)
        window.decorView.windowInsetsController?.apply {
            hide(WindowInsets.Type.systemBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun buildUi(): View {
        val side = minOf(
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels
        )
        val inset = (side * 0.1465f).toInt() + dp(2f)
        contentWidth = side - inset * 2

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(inset, inset, inset, inset)
        }
        listScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(
                listContainer,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        root.addView(
            listScroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        statusView = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(0xB3FFFFFF.toInt())
            textSize = 13f
            setLineSpacing(dp(3f).toFloat(), 1f)
            visibility = View.GONE
        }
        root.addView(
            statusView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply {
                leftMargin = inset + dp(8f)
                rightMargin = inset + dp(8f)
            }
        )
        return root
    }

    private fun load() {
        showStatus(if (listContainer.childCount == 0) "加载中…" else null)
        MemosApi.fetchRecent(10) { result ->
            if (isFinishing || isDestroyed) return@fetchRecent
            when (result) {
                is FetchResult.Success -> {
                    renderMemos(result.memos)
                }
                is FetchResult.Failure -> {
                    if (listContainer.childCount == 0) {
                        showStatus("${result.message}\n\n点击重试", retry = true)
                    } else {
                        Toast.makeText(this, "刷新失败：${result.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun renderMemos(memos: List<Memo>) {
        listContainer.removeAllViews()
        if (memos.isEmpty()) {
            showStatus("暂无 memo")
            return
        }
        showStatus(null)
        memos.forEach { listContainer.addView(createMemoView(it)) }
        listScroll.scrollTo(0, 0)
    }

    private fun createMemoView(memo: Memo): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(11f), 0, dp(11f))
        }
        val preview = memo.content.replace(Regex("\\s*\\n+\\s*"), " ")

        val time = TextView(this).apply {
            text = relativeTime(memo.createTimeMillis)
            setTextColor(0x8AFFFFFF.toInt())
            textSize = 10.5f
            includeFontPadding = false
        }
        item.addView(
            time,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val body = TextView(this).apply {
            text = preview
            setTextColor(Color.WHITE)
            textSize = 15f
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
            setLineSpacing(dp(2f).toFloat(), 1f)
            includeFontPadding = false
        }
        item.addView(
            body,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4f) }
        )

        if (isTruncated(preview, body)) {
            item.isClickable = true
            item.isFocusable = true
            item.background = RippleDrawable(ColorStateList.valueOf(0x26FFFFFF), null, null)
            item.setOnClickListener { openDetail(memo) }
        }
        return item
    }

    private fun isTruncated(text: String, body: TextView): Boolean {
        val width = contentWidth
        if (width <= 0) return false
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, body.paint, width)
            .setLineSpacing(dp(2f).toFloat(), 1f)
            .setMaxLines(3)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        val lastLine = layout.lineCount - 1
        return lastLine >= 0 && layout.getEllipsisCount(lastLine) > 0
    }

    private fun openDetail(memo: Memo) {
        returningFromDetail = true
        startActivity(
            Intent(this, DetailActivity::class.java)
                .putExtra(DetailActivity.EXTRA_TEXT, memo.content)
                .putExtra(DetailActivity.EXTRA_TIME, memo.createTimeMillis)
        )
    }

    private fun showStatus(message: String?, retry: Boolean = false) {
        if (message == null) {
            statusView.visibility = View.GONE
            statusView.setOnClickListener(null)
            statusView.isClickable = false
            return
        }
        statusView.text = message
        statusView.visibility = View.VISIBLE
        statusView.isClickable = retry
        statusView.setOnClickListener { if (retry) load() }
    }
}
