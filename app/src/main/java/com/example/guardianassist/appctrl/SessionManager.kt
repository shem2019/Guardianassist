package com.example.guardianassist.appctrl

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)

    companion object {
        private const val ADMIN_TOKEN_KEY = "admin_token"
        private const val USER_TOKEN_KEY = "user_token"
        private const val REAL_NAME_KEY = "real_name"
        private const val USER_ID_KEY = "user_id"
        private const val ORG_ID_KEY = "org_id"
        private const val ORG_NAME_KEY = "org_name"
        private const val IS_ON_SITE_KEY = "is_on_site"
        private const val SITE_ID_KEY = "site_id"
        private const val SITE_NAME_KEY = "site_name"
        private const val BOOK_ON_TIME_KEY = "book_on_time"
        private const val ADMIN_ORG_ID_KEY = "admin_org_id"
        private const val ADMIN_SITE_ID_KEY = "admin_site_id"
        private const val ADMIN_LEVEL_KEY = "admin_level"
        private const val ADMIN_SITE_ACCESS_KEY = "admin_site_access" // List of site IDs
    }

    // Save admin token to SharedPreferences
    fun saveAdminToken(token: String) {
        prefs.edit().putString(ADMIN_TOKEN_KEY, token).apply()
    }

    // Retrieve admin token from SharedPreferences
    fun fetchAdminToken(): String? {
        return prefs.getString(ADMIN_TOKEN_KEY, null)
    }

    // Save user token to SharedPreferences
    fun saveUserToken(token: String) {
        prefs.edit().putString(USER_TOKEN_KEY, token).apply()
    }

    // Retrieve user token from SharedPreferences
    fun fetchUserToken(): String? {
        return prefs.getString(USER_TOKEN_KEY, null)
    }

    // Save real name to SharedPreferences
    fun saveRealName(realName: String) {
        prefs.edit().putString(REAL_NAME_KEY, realName).apply()
    }

    fun fetchRealName(): String? {
        return prefs.getString(REAL_NAME_KEY, null)
    }

    fun saveTagName(tagName: String) {
        val editor = prefs.edit()
        editor.putString("TAG_NAME_KEY", tagName)
        editor.apply()
    }
    fun saveUsername(username: String) {
        val editor = prefs.edit()
        editor.putString("USERNAME_KEY", username)
        editor.apply()
    }

    fun fetchUsername(): String? {
        return prefs.getString("USERNAME_KEY", null)
    }


    fun saveClockInTag(tagName: String) {
        val editor = prefs.edit()
        editor.putString("CLOCK_IN_TAG_KEY", tagName)
        editor.apply()
    }

    fun fetchClockInTag(): String? {
        return prefs.getString("CLOCK_IN_TAG_KEY", null)
    }



    fun saveUserSession(token: String, userId: Int, realName: String, orgId: Int, orgName: String, siteId: Int, siteName: String) {
        val editor = prefs.edit()
        editor.putString(USER_TOKEN_KEY, token)
        editor.putInt(USER_ID_KEY, userId)
        editor.putString(REAL_NAME_KEY, realName)
        editor.putInt(ORG_ID_KEY, orgId)
        editor.putString(ORG_NAME_KEY, orgName)
        editor.putInt(SITE_ID_KEY, siteId)
        editor.putString(SITE_NAME_KEY, siteName)
        editor.apply()
    }

    // Retrieve user details
    fun fetchUserId(): Int = prefs.getInt(USER_ID_KEY, -1)
    fun fetchOrgId(): Int = prefs.getInt(ORG_ID_KEY, -1)
    fun fetchOrgName(): String? = prefs.getString(ORG_NAME_KEY, null)

    fun saveSiteId(siteId: Int) {
        prefs.edit().putInt(SITE_ID_KEY, siteId).apply()
        Log.d("SessionManager", "Site ID saved: $siteId") // ✅ Debugging Log
    }

    fun fetchSiteId(): Int {
        val siteId = prefs.getInt(SITE_ID_KEY, -1)
        Log.d("SessionManager", "Fetched Site ID: $siteId") // ✅ Debugging Log
        return siteId
    }

    fun saveSiteName(siteName: String) {
        prefs.edit().putString(SITE_NAME_KEY, siteName).apply()
        Log.d("SessionManager", "Site Name saved: $siteName") // ✅ Debugging Log
    }

    fun fetchSiteName(): String? {
        val siteName = prefs.getString(SITE_NAME_KEY, null)
        Log.d("SessionManager", "Fetched Site Name: $siteName") // ✅ Debugging Log
        return siteName
    }
    fun saveOnSiteStatus(isOnSite: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(IS_ON_SITE_KEY, isOnSite)
        editor.apply()
    }

    fun fetchOnSiteStatus(): Boolean {
        return prefs.getBoolean(IS_ON_SITE_KEY, false)
    }
    fun saveBookOnTime(bookOnTime: String) {
        prefs.edit().putString(BOOK_ON_TIME_KEY, bookOnTime).apply()
    }

    fun saveIsOnSite(isOnSite: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean("is_on_site", isOnSite)
        editor.apply()
    }

    fun fetchIsOnSite(): Boolean {
        return prefs.getBoolean("is_on_site", false) // Default to false if not found
    }
    fun saveAdminPrivileges(orgId: Int?, siteId: Int?) {
        prefs.edit().apply {
            putInt(ADMIN_ORG_ID_KEY, orgId ?: -1)  // Default `-1` for unrestricted access
            putInt(ADMIN_SITE_ID_KEY, siteId ?: -1)
            apply()
        }
        Log.d("SessionManager", "✅ Admin Privileges Saved: OrgID=$orgId, SiteID=$siteId")
    }


    fun fetchBookOnTime(): String? = prefs.getString(BOOK_ON_TIME_KEY, "N/A")



    fun saveAdminPrivileges(orgId: Int?, siteId: Int?, adminLevel: String, siteAccess: List<Int>) {
        prefs.edit().apply {
            putInt(ADMIN_ORG_ID_KEY, orgId ?: -1)  // Default `-1` for unrestricted access
            putInt(ADMIN_SITE_ID_KEY, siteId ?: -1)
            putString(ADMIN_LEVEL_KEY, adminLevel)
            putString(ADMIN_SITE_ACCESS_KEY, Gson().toJson(siteAccess)) // Save list as JSON
            apply()
        }
        Log.d("SessionManager", "✅ Admin Privileges Saved: OrgID=$orgId, SiteID=$siteId, Level=$adminLevel, Sites=$siteAccess")
    }

    // ✅ Fetch Admin Org ID
    fun fetchAdminOrgId(): Int = prefs.getInt(ADMIN_ORG_ID_KEY, -1)

    // ✅ Fetch Admin Site ID
    fun fetchAdminSiteId(): Int = prefs.getInt(ADMIN_SITE_ID_KEY, -1)

    // ✅ Fetch Admin Level
    fun fetchAdminLevel(): String = prefs.getString(ADMIN_LEVEL_KEY, "Unknown") ?: "Unknown"

    // ✅ Fetch Accessible Site IDs
    fun fetchSiteAccess(): List<Int> {
        val json = prefs.getString(ADMIN_SITE_ACCESS_KEY, "[]") ?: "[]"
        return Gson().fromJson(json, object : TypeToken<List<Int>>() {}.type)
    }


    // Clear session (on logout)
    fun clearSession() {
        prefs.edit().apply {
            remove(ADMIN_TOKEN_KEY)
            remove(USER_TOKEN_KEY)
            remove(USER_ID_KEY)
            remove(REAL_NAME_KEY)
            remove(ORG_ID_KEY)
            remove(ORG_NAME_KEY)
            remove(SITE_ID_KEY)
            remove(SITE_NAME_KEY)
            remove(ADMIN_TOKEN_KEY)
            remove(ADMIN_ORG_ID_KEY)
            remove(ADMIN_SITE_ID_KEY)
            remove(ADMIN_LEVEL_KEY)
            remove(ADMIN_SITE_ACCESS_KEY)
            apply()
        }
        Log.d("SessionManager", "❌ Admin Session Cleared")
    }
}
