package com.imaegoo.memoswatch

/**
 * Converts memo markdown into compact plain text for tiny watch screens.
 */
object Markdown {

    fun toPlainText(src: String): String {
        var s = src

        // Code fences: keep the code, drop the fence lines and language hint.
        s = s.replace(Regex("(?m)^\\s*```.*$"), "")

        // Images first (before links), then links keep only their label.
        s = s.replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), "")
        s = s.replace(Regex("\\[([^\\]]*)]\\([^)]*\\)"), "$1")

        // HTML tags and entities.
        s = s.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        s = s.replace(Regex("<[^>]+>"), "")
        s = s.replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")

        // Line prefixes: headings, quotes, list bullets, ordered lists, rules.
        s = s.replace(Regex("(?m)^\\s{0,3}#{1,6}\\s+"), "")
        s = s.replace(Regex("(?m)^\\s{0,3}>\\s?"), "")
        s = s.replace(Regex("(?m)^\\s*[-*+]\\s+"), "")
        s = s.replace(Regex("(?m)^\\s*\\d+[.)]\\s+"), "")
        s = s.replace(Regex("(?m)^\\s*([-*_])(\\s*\\1){2,}\\s*$"), "")

        // Inline emphasis and code markers.
        s = s.replace(Regex("\\*\\*|__|~~|`"), "")
        s = s.replace(Regex("(?<!\\w)[*_](?=\\S)|(?<=\\S)[*_](?!\\w)"), "")

        // Collapse blank lines and trailing spaces.
        s = s.replace(Regex("[ \\t]+\\n"), "\n")
        s = s.replace(Regex("\\n{3,}"), "\n\n")
        return s.trim()
    }
}
