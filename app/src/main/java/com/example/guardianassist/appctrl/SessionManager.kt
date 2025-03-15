package com.example.guardianassist.appctrl

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

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

        private const val SITE_ID_KEY = "site_id"
        private const val SITE_NAME_KEY = "site_name"
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
            apply()
        }
    }
}
