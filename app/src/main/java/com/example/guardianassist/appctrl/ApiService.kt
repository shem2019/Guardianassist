package com.example.guardianassist.appctrl

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

// Data model for sending signup request
data class SignupRequest(
    val username: String,
    val password: String
)

// Data model for handling signup response
data class SignupResponse(
    val status: String,
    val message: String,
    val token: String? // Token is returned only on success
)

data class LoginRequest(
    val username: String,
    val password: String
)

// Data model for login response
data class LoginResponse(
    val status: String,
    val token: String?,  // Token will be returned on success
    val message: String? // Error message in case of failure
)
data class StoreResponse(
    val status: String,
    val message: String?
)


data class NextUserIdResponse(val next_user_id: Int)
data class RegisterUserRequest(val real_name: String, val username: String, val password: String)
data class RegisterUserResponse(val status: String, val message: String, val user_id: Int, val token: String)
data class UserResponse(val user_id: Int, val real_name: String, val username: String)
data class UpdatePasswordRequest(val user_id: Int, val new_password: String)
data class UpdatePasswordResponse(val status: String, val message: String)
data class UserLoginRequest(val username: String, val password: String)
data class UserLoginResponse(val status: String, val token: String?, val message: String?)
data class FacialDataRequest(
    val token: String,
    val embedding: List<Float>
)
data class UserDetailsResponse(
    val status: String,
    val data: UserDetailsData?
)

data class UserDetailsData(
    val real_name: String,
    val username: String
)
data class SaveEventRequest(
    val event_type: String,
    val site_name: String,
    val real_name: String,
    val timestamp: String
)
data class SaveLogRequest(
    val event_type: String
)
data class SaveHourlyCheckRequest(
    val token: String, // References `token` column in `users`
    val site_id: Int, // References `site_id` column in `sites`
    val personal_safety: Boolean,
    val site_secure: Boolean,
    val equipment_functional: Boolean,
    val comments: String? // Optional comments field
)

data class IncidentReportRequest(
    val token: RequestBody,
    val incidentType: RequestBody,
    val customIncident: RequestBody?,
    val incidentDescription: RequestBody,
    val correctiveAction: RequestBody,
    val severity: RequestBody,
    val incidentImage: MultipartBody.Part?,
    val correctiveImage: MultipartBody.Part?
)
data class SitesResponse(
    val success: Boolean,
    val sites: List<Site>
)

// Represents a single NFC tag
data class Tag(
    val id: Int,
    val site_id: Int,
    val name: String,
    val type: String, // "Start", "Intermediate", "End"
    val latitude: Double,
    val longitude: Double
)

// Response wrapper for fetching tags
data class TagResponse(
    val success: Boolean,
    val tags: List<Tag>
)

// Data model for patrol route
data class PatrolRoutePayload(
    val site_id: Int,
    val checkpoints: List<PatrolCheckpoint>
)

// Represents a single checkpoint in a patrol route
data class PatrolCheckpoint(
    val id: Int,
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double
)

// Response wrapper for patrol route
data class PatrolRouteResponse(
    val success: Boolean,
    val route: List<PatrolCheckpoint>
    )
data class AccessResponse(val access: String, val reason: String?)

data class Organization(
    val org_id: Int,
    val org_name: String,
    val email: String,
    val subscription_status: String
)
data class OrganizationResponse(val success: Boolean, val organizations: List<Organization>)
data class SiteResponse(val success: Boolean, val sites: List<Site>)

data class Site(
    val site_id: Int,
    val org_id: Int,
    val site_name: String,
    val site_address: String,
    val latitude: Double,
    val longitude: Double,
    val subscription_status: String
)

data class SiteStatusUpdate(
    val site_id: Int,
    val subscription_status: String
)


data class BasicResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String
)

data class NfcTagResponse(
    val success: Boolean,
    val message: String?,
    val tags: List<NfcTag>
)


data class NfcTag(
    val tag_id: Int,

    @SerializedName("tag_name")
    val tagName: String, // ✅ Ensures tag_name is included

    val site_id: Int,

    @SerializedName("tag_type")
    val tagType: String, // ✅ Correct field name (was tag_type)

    val latitude: Double,
    val longitude: Double,

    @SerializedName("is_active")
    val isActive: Boolean
)

interface ApiService {
    //logs
    @POST("savelog.php")
    fun saveLog(
        @Header("Authorization") token: String,
        @Body logRequest: SaveLogRequest
    ): Call<Void>
    //events
    @POST("event")
    fun saveEvent(
        @Header("Authorization") token: String,
        @Body request: SaveEventRequest
    ): Call<Void>

    //
    @GET("userdatafetch.php")
    fun getUserDetails(@Header("Authorization") token: String): Call<UserDetailsResponse>


    @Headers("Content-Type: application/json")
    @POST("admin_signup.php") // This is the API endpoint on your server
    fun signupAdmin(@Body signupRequest: SignupRequest): Call<SignupResponse>
    //admin login
    @Headers("Content-Type: application/json")
    @POST("admin_login.php")  // This is the API endpoint on your server
    fun loginAdmin(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @GET("get_next_user_id.php")
    fun getNextUserId(): Call<NextUserIdResponse>

    @POST("register_user.php")
    fun registerUser(@Body request: RegisterUserRequest): Call<RegisterUserResponse>

    @GET("fetch_users.php")
    fun fetchUsers(@Query("search") searchTerm: String): Call<List<UserResponse>>

    @POST("update_password.php")
    fun updatePassword(@Body request: UpdatePasswordRequest): Call<UpdatePasswordResponse>
    @POST("user_login.php")
    fun loginUser(@Body request: UserLoginRequest): Call<UserLoginResponse>

    //
    @Multipart
    @POST("uniform.php")
    fun uploadImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("token") tokenPart: RequestBody
    ): Call<Void>
// hourly
    @POST("posthourly.php") // Replace with your actual endpoint
    fun saveHourlyCheck(@Body request: SaveHourlyCheckRequest): Call<Void>

    @Multipart
    @POST("incidentreport.php")
    fun reportIncident(
        @Part("token") token: RequestBody,
        @Part("incident_type") incidentType: RequestBody, // Match backend parameter
        @Part("custom_incident") customIncident: RequestBody?, // Match backend parameter
        @Part("incident_description") incidentDescription: RequestBody, // Match backend parameter
        @Part("corrective_action") correctiveAction: RequestBody, // Match backend parameter
        @Part("severity") severity: RequestBody,
        @Part incidentImage: MultipartBody.Part?,
        @Part correctiveImage: MultipartBody.Part?
    ): Call<Void>

    // 🚀 Add a new tag (NFC checkpoint)
    @POST("add_tag.php")
    fun addTag(@Body tag: Tag): Call<Void>

    // 🚀 Fetch tags for a site
    @GET("fetch_tags.php")
    fun fetchTags(@Query("site_id") siteId: Int): Call<TagResponse>

    // 🚀 Save patrol route
    @POST("add_patrol_route.php")
    fun savePatrolRoute(@Body payload: PatrolRoutePayload): Call<Void>

    // 🚀 Fetch patrol route for a site
    @GET("fetch_patrol_route.php")
    fun getPatrolRoute(@Query("site_id") siteId: Int): Call<PatrolRouteResponse>

    @FormUrlEncoded
    @POST("check_access.php")
    fun checkAccess(@Field("user_id") userId: Int): Call<AccessResponse>
    @GET("get_organizations.php")
    fun getOrganizations(): Call<OrganizationResponse>

    @POST("add_organization.php")
    fun addOrganization(@Body organization: Organization): Call<BasicResponse>
    @GET("get_sites.php")
    fun getSites(@Query("org_id") orgId: Int): Call<SiteResponse>

    @POST("add_site.php")
    fun addSite(@Body site: Site): Call<BasicResponse>

    @POST("update_site_status.php")
    fun updateSiteStatus(@Body request: SiteStatusUpdate): Call<BasicResponse>

    @GET("get_nfc_tags.php")
    fun getNfcTags(@Query("site_id") siteId: Int): Call<NfcTagResponse>

    @POST("add_nfc_tag.php")
    fun addNfcTag(@Body nfcTag: NfcTag): Call<BasicResponse>

    //@POST("activate_nfc_tags.php")
  //  fun activateNfcTags(@Body request: ActivateTagsRequest): Call<BasicResponse>

}

