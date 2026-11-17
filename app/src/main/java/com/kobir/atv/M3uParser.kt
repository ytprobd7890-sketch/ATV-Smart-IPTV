package com.kobir.atv

object M3uParser {
    fun parse(text: String): List<Channel> {
        val result = mutableListOf<Channel>()
        var name = ""
        var logo: String? = null
        var group = "All channels"
        var number = 0
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                val attrs = Regex("([\\w-]+)=[\"']([^\"']*)[\"']")
                    .findAll(line).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                name = line.substringAfterLast(",", "").trim().ifBlank { "Channel ${number + 1}" }
                logo = attrs["tvg-logo"]
                group = attrs["group-title"] ?: "All channels"
                number = attrs["tvg-chno"]?.toIntOrNull() ?: (number + 1)
            } else if (line.isNotBlank() && !line.startsWith("#") &&
                (line.startsWith("http://") || line.startsWith("https://") || line.startsWith("rtsp://"))) {
                result += Channel(number.coerceAtLeast(result.size + 1), name.ifBlank { "Channel ${result.size + 1}" }, line, logo, group)
                name = ""; logo = null; group = "All channels"
            }
        }
        return result
    }
}
