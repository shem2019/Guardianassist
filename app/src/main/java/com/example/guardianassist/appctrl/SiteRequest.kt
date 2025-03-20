package com.example.guardianassist.appctrl

import com.google.gson.annotations.SerializedName

data class SiteRequest(
    @SerializedName("site_ids") val siteIds: List<Int>
)
