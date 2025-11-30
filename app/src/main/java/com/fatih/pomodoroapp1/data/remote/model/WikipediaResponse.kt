package com.fatih.pomodoroapp1.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Wikimedia Feed API Response
 */
data class OnThisDayResponse(
    @SerializedName("selected")
    val selected: List<EventItem>? = null,

    @SerializedName("births")
    val births: List<EventItem>? = null,

    @SerializedName("deaths")
    val deaths: List<EventItem>? = null,

    @SerializedName("events")
    val events: List<EventItem>? = null,

    @SerializedName("holidays")
    val holidays: List<EventItem>? = null
)

data class EventItem(
    @SerializedName("text")
    val text: String? = null,

    @SerializedName("year")
    val year: Int? = null,

    @SerializedName("pages")
    val pages: List<WikiPage>? = null
)

data class WikiPage(
    @SerializedName("title")
    val title: String? = null,

    @SerializedName("extract")
    val extract: String? = null
)
