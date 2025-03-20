package de.peekandpoke.aktor.frontend

data class WebAppConfig(
    val orgId: String = "",
    val lang: String = "en",
    val fallbackLang: String = "en",
    val title: String = "The Base WebApp | Local",
    val environment: String = "dev",
    val wwwBaseUrl: String = "http://www.aktor.local:8081",
    val apiBaseUrl: String = "http://api.aktor.local:8081",
    val insightsDetailsBaseUrl: String = "http://admin.aktor.local:8081/_/insights/details/",
    val gaContainer: String = "",
    val gaTrackerName: String = "webapp",
) {
    @Suppress("unused")
    @JsName("withOrgId")
    fun withOrgId(orgId: String) = copy(orgId = orgId)

    @Suppress("unused")
    @JsName("withLang")
    fun withLang(lang: String) = copy(lang = lang)

    @Suppress("unused")
    @JsName("withFallbackLang")
    fun withFallbackLang(fallbackLang: String) = copy(fallbackLang = fallbackLang)

    @Suppress("unused")
    @JsName("withTitle")
    fun withTitle(title: String) = copy(title = title)

    @Suppress("unused")
    @JsName("withEnvironment")
    fun withEnvironment(environment: String) = copy(environment = environment)

    @Suppress("unused")
    @JsName("withWwwBaseUrl")
    fun withWwwBaseUrl(url: String) = copy(wwwBaseUrl = url)

    @Suppress("unused")
    @JsName("withApiBaseUrl")
    fun withApiBaseUrl(url: String) = copy(apiBaseUrl = url)

    @Suppress("unused")
    @JsName("withInsightsDetailsBaseUrl")
    fun withInsightsDetailsBaseUrl(url: String) = copy(insightsDetailsBaseUrl = url)

    @Suppress("unused")
    @JsName("withGaContainer")
    fun withGaContainer(gaContainer: String) = copy(gaContainer = gaContainer)

    @Suppress("unused")
    @JsName("withGaTrackerName")
    fun withGaTrackerName(gaTrackerName: String) = copy(gaTrackerName = gaTrackerName)

    // Getters

    @Suppress("unused")
    @JsName("isLive")
    val isLive = environment == "live"

    @Suppress("unused")
    val isNotLive get() = !isLive
}
