package com.pelicinehd

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor

class PeliCineHDProvider : MainAPI() { // All providers must be an instance of MainAPI
    override var mainUrl = "https://pelicinehd.com/" 
    override var name = "PeliCineHD"
    override val supportedTypes = setOf(TvType.Movie)

    override var lang = "en"

    // Enable this when your provider has a main page
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        Pair("${mainUrl}peliculas/", "Películas"),
        Pair("${mainUrl}series/", "Series"),
        Pair("${mainUrl}estreno/", "Estrenos")
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}page/$page/"
        val document = app.get(url).document

        val home = document.select("article.post").mapNotNull { element ->
            val title = element.selectFirst("h2.entry-title")?.text()
            val href = element.selectFirst("h2.entry-title a")?.attr("href")
            val posterUrl = element.selectFirst("figure img")?.attr("src")

            if (title != null && href != null) {
                newMovieSearchResponse(title, href, TvType.Movie) {
                    this.posterUrl = posterUrl
                }
            } else {
                null
            }
        }

        return newHomePageResponse(request.name, home)
    }

    // This function gets called when you search for something
    override suspend fun search(query: String): List<SearchResponse> {
        val url = "${mainUrl}?s=$query"
        val document = app.get(url).document

        return document.select("article.post").mapNotNull { element ->
            val title = element.selectFirst("h2.entry-title")?.text()
            val href = element.selectFirst("h2.entry-title a")?.attr("href")
            val posterUrl = element.selectFirst("figure img")?.attr("src")

            if (title != null && href != null) {
                newMovieSearchResponse(title, href, TvType.Movie) {
                    this.posterUrl = posterUrl
                }
            } else {
                null
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text() ?: ""
        val posterUrl = document.selectFirst("figure img")?.attr("src")
        val plot = document.selectFirst(".entry-content p")?.text()
        val year = document.selectFirst(".year")?.text()?.toIntOrNull()

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = posterUrl
            this.plot = plot
            this.year = year
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        // Buscar iframes de reproductores
        document.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.contains("trembed") || src.contains("trid")) {
                val iframeDoc = app.get(src).document
                val finalSrc = iframeDoc.selectFirst("iframe")?.attr("src")
                if (finalSrc != null) {
                    loadExtractor(finalSrc, data, subtitleCallback, callback)
                }
            } else {
                loadExtractor(src, data, subtitleCallback, callback)
            }
        }
        
        return true
    }
}