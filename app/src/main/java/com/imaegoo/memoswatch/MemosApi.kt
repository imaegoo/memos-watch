package com.imaegoo.memoswatch

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.time.Instant
import java.util.concurrent.Executors

data class Memo(
    val id: String,
    val content: String,
    val createTimeMillis: Long
)

sealed class FetchResult {
    data class Success(val memos: List<Memo>) : FetchResult()
    data class Failure(val message: String) : FetchResult()
}

object MemosApi {

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "memos-net").apply { isDaemon = true }
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    private class ApiException(message: String) : IOException(message)

    fun fetchRecent(limit: Int, callback: (FetchResult) -> Unit) {
        executor.execute {
            val result = try {
                FetchResult.Success(request(limit))
            } catch (t: Throwable) {
                FetchResult.Failure(describe(t))
            }
            mainHandler.post { callback(result) }
        }
    }

    private fun request(limit: Int): List<Memo> {
        val base = BuildConfig.MEMOS_BASE_URL.trimEnd('/')
        val url = URL("$base/api/v1/memos?pageSize=$limit")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Authorization", "Bearer ${BuildConfig.MEMOS_TOKEN}")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw ApiException(
                    when (code) {
                        401 -> "Token 无效或已过期"
                        403 -> "没有访问权限"
                        404 -> "接口地址不存在"
                        else -> "服务器错误 $code"
                    }
                )
            }

            val array = JSONObject(body).optJSONArray("memos") ?: return emptyList()
            val memos = ArrayList<Memo>(array.length())
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val content = Markdown.toPlainText(item.optString("content"))
                if (content.isBlank()) continue
                memos += Memo(
                    id = item.optString("name"),
                    content = content,
                    createTimeMillis = parseTime(item.optString("createTime"))
                )
            }
            return memos.sortedByDescending { it.createTimeMillis }.take(limit)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseTime(value: String): Long = try {
        Instant.parse(value).toEpochMilli()
    } catch (_: Exception) {
        0L
    }

    private fun describe(t: Throwable): String = when (t) {
        is ApiException -> t.message ?: "请求失败"
        is UnknownHostException -> "无法连接服务器"
        is SocketTimeoutException -> "连接超时"
        else -> "网络错误"
    }
}
