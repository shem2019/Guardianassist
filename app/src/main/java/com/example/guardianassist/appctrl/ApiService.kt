package com.example.guardianassist.appctrl

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
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

data class StoreResponse(
    val status: String,
    val message: String?
)


data class NextUserIdResponse(val next_user_id: Int)
data class RegisterUserResponse(val status: String, val message: String, val user_id: Int, val token: String)
data class UserResponse(val user_id: Int, val real_name: String, val username: String)
data class UserLoginRequest(val username: String, val password: String)
data class FacialDataRequest(
    val token: String,
    val embedding: List<Float>
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
    var subscription_status: String
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

data class LoginRequest(
    val username: String,
    val password: String
)



data class UserActivationRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("is_active") val isActive: Boolean
)


data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("status") val status: String,  // ✅ Added "status"
    @SerializedName("message") val message: String,
    @SerializedName("token") val token: String?)

data class User(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("real_name") val realName: String,
    @SerializedName("username") val username: String,
    @SerializedName("is_active") val isActive: Boolean
)
data class UserListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("users") val users: List<User>)



data class UpdatePasswordRequest(
    @SerializedName("user_id") val userId: Int,  // ✅ Ensure correct parameter name
    @SerializedName("new_password") val newPassword: String  // ✅ Ensure correct parameter name
)

data class UpdatePasswordResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)
data class RegisterUserRequest(
    @SerializedName("real_name") val realName: String,
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("org_id") val orgId: Int,  // ✅ Ensure org_id is included
    @SerializedName("is_active") val isActive: Boolean
)
data class UserLoginResponse(
    @SerializedName("success") val success: Boolean,  // ✅ Check if API uses "success" instead of "status"
    @SerializedName("token") val token: String?,
    @SerializedName("message") val message: String?)

data class AdminLoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("token") val token: String?,
    @SerializedName("admin_id") val adminId: Int = -1,  // ✅ Default -1 if missing
    @SerializedName("org_id") val orgId: Int = -1,  // ✅ Default -1 if missing
    @SerializedName("site_id") val siteId: Int = -1,  // ✅ Default -1 if missing
    @SerializedName("admin_level") val adminLevel: String = "Unknown",  // ✅ Default "Unknown"
    @SerializedName("site_access") val siteAccess: List<Int> = emptyList()  // ✅ Default empty list
)


data class BookOnRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("site_id") val siteId: Int,
    @SerializedName("org_id") val orgId: Int,
    @SerializedName("clock_in_tag") val clockInTag: String
)
data class BookOnResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("clock_in_time") val clockInTime: String?
)

data class UserDetailsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: UserData?
)

data class UserData(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("real_name") val realName: String,
    @SerializedName("org_id") val orgId: Int,
    @SerializedName("site_id") val siteId: Int,  // ✅ Added site_id
    @SerializedName("org_name") val orgName: String?,
    @SerializedName("site_name") val siteName: String?
)



data class BookOnStatusResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("is_booked_on") val isBookedOn: Boolean,
    @SerializedName("site_id") val siteId: Int?,
    @SerializedName("clock_in_tag") val clockInTag: String?,
    @SerializedName("clock_in_time") val clockInTime: String?  // ✅ Added missing field
)

data class SaveHourlyCheckRequest(
    val token: String,
    val site_id: Int,
    val org_id: Int,  // ✅ Added organization ID
    val real_name: String,  // ✅ Added real name
    val personal_safety: Boolean,
    val site_secure: Boolean,
    val equipment_functional: Boolean,
    val comments: String?
)

data class IncidentReportRequest(
    @SerializedName("token") val token: RequestBody,
    @SerializedName("real_name") val realName: RequestBody,
    @SerializedName("org_id") val orgId: RequestBody,
    @SerializedName("site_id") val siteId: RequestBody,
    @SerializedName("incident_type") val incidentType: RequestBody,
    @SerializedName("custom_incident") val customIncident: RequestBody?,
    @SerializedName("incident_description") val incidentDescription: RequestBody,
    @SerializedName("corrective_action") val correctiveAction: RequestBody,
    @SerializedName("severity") val severity: RequestBody,
    @SerializedName("incident_image") val incidentImage: MultipartBody.Part?,
    @SerializedName("corrective_image") val correctiveImage: MultipartBody.Part?
)

data class IncidentReportResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("incident_image_path") val incidentImagePath: String?,
    @SerializedName("corrective_image_path") val correctiveImagePath: String?
)


data class ClockInRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("site_id") val siteId: Int,
    @SerializedName("org_id") val orgId: Int,
    @SerializedName("clock_in_tag") val clockInTag: String
)

data class ClockInResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?
)
data class ClockOutRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("site_id") val siteId: Int,
    @SerializedName("org_id") val orgId: Int,
    @SerializedName("clock_out_tag") val clockOutTag: String
)


data class ClockOutResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?
)

data class PatrolLogRequest(
    @SerializedName("user_id") val userId: Int,  // ✅ User performing the patrol
    @SerializedName("site_id") val siteId: Int,  // ✅ Site where patrol happens
    @SerializedName("org_id") val orgId: Int,    // ✅ Organization ID
    @SerializedName("tag_name") val tagName: String  // ✅ NFC tag name scanned
)
data class PatrolRecordsRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("book_on_time") val bookOnTime: String
)

data class PatrolRecord(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("patrol_time") val patrolTime: String
)

data class PatrolResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("patrols") val patrols: List<PatrolRecord>?
)
//
data class SiteNamesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("sites") val sites: List<SiteNames>
)

data class SiteNames(
    @SerializedName("site_id") val siteId: Int,
    @SerializedName("site_name") val siteName: String
)

//
data class AdminClockInResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("clock_ins") val clockIns: List<ClockInRecord>
)

data class ClockInRecord(
    @SerializedName("site_name") val siteName: String,
    @SerializedName("real_name") val realName: String,
    @SerializedName("clock_in_time") val clockInTime: String,
    @SerializedName("is_on_site") val isOnSite: Int
)
//clock out
data class AdminClockOutResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("clock_outs") val clockOuts: List<ClockOutRecord>
)

data class ClockOutRecord(
    @SerializedName("site_name") val siteName: String,
    @SerializedName("real_name") val realName: String,
    @SerializedName("clock_out_time") val clockOutTime: String
)
//
data class HourlyCheckResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("hourly_checks") val hourlyChecks: List<HourlyCheckRecord>
)

data class HourlyCheckRecord(
    @SerializedName("site_name") val siteName: String,
    @SerializedName("real_name") val realName: String,
    @SerializedName("personal_safety") val personalSafety: Int,
    @SerializedName("site_secure") val siteSecure: Int,
    @SerializedName("equipment_functional") val equipmentFunctional: Int,
    @SerializedName("comments") val comments: String?,
    @SerializedName("check_time") val checkTime: String
)
data class AdminPatrol(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("site_id") val siteId: Int,
    @SerializedName("org_id") val orgId: Int,
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("patrol_time") val patrolTime: String
)

data class AdminPatrolResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("patrols") val patrols: List<AdminSitePatrol>
)

data class AdminSitePatrol(
    @SerializedName("site_name") val siteName: String,
    @SerializedName("patrols") val patrolRecords: List<AdminPatrolRecord>
)

data class AdminPatrolRecord(
    @SerializedName("real_name") val realName: String,
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("patrol_date") val patrolDate: String,
    @SerializedName("patrol_time") val patrolTime: String
)
data class GroupedPatrols(
    val siteName: String,
    val guards: Map<String, Map<String, Int>> // Guard -> Patrol Area -> Visit Count
)
data class AdminSiteRequest(
    @SerializedName("site_ids") val siteIds: List<Int>
)

/// book sessions

// Request payloads
data class BookSessionRequest(
    val user_id: Int,
    val org_id: Int,
    val site_id: Int,
    val tag: String
)

// Generic success/failure wrapper
data class ApiResponse(
    val success: Boolean,
    val message: String
)

// For status checks (optional full history)
data class SessionRecord(
    val id: Int,
    val user_id: Int,
    val site_id: Int,
    val clock_in_tag: String,
    val clock_in_time: String,
    val clock_out_tag: String?,
    val clock_out_time: String?,
    val site_name: String?,
)
data class SessionHistoryResponse(
    val success: Boolean,
    val sessions: List<SessionRecord>
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

    @Headers("Content-Type: application/json")
    @POST("admin_signup.php") // This is the API endpoint on your server
    fun signupAdmin(@Body signupRequest: SignupRequest): Call<SignupResponse>


    @GET("get_next_user_id.php")
    fun getNextUserId(): Call<NextUserIdResponse>

    @GET("fetch_users.php")
    fun fetchUsers(@Query("search") searchTerm: String): Call<List<UserResponse>>


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
    @POST("save_hourly_check.php") // Replace with your actual endpoint
    fun saveHourlyCheck(@Body request: SaveHourlyCheckRequest): Call<Void>


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
    @POST("register_user.php")
    fun registerUser(@Body request: RegisterUserRequest): Call<LoginResponse>

    @POST("user_login.php")
    fun loginUser(@Body request: LoginRequest): Call<LoginResponse>


    @GET("get_users.php")
    fun getUsers(@Query("org_id") orgId: Int): Call<UserListResponse>

    @POST("update_user_status.php")
    fun updateUserStatus(@Body request: UserActivationRequest): Call<Void>



    @POST("update_password.php")
    fun updatePassword(@Body request: UpdatePasswordRequest): Call<UpdatePasswordResponse>

    @GET("get_user_details.php")
    fun getUserDetails(@Header("Authorization") token: String): Call<UserDetailsResponse>  // ✅ Ensure token is sent

    @POST("admin_login.php")
    fun loginAdmin(@Body request: LoginRequest): Call<AdminLoginResponse>

    @POST("book_on.php")
    fun bookOn(@Header("Authorization") token: String, @Body request: BookOnRequest): Call<BookOnResponse>

    @GET("check_book_on_status.php")
    fun checkBookOnStatus(
        @Header("Authorization") token: String,
        @Query("user_id") userId: String,
        @Query("site_id") siteId: Int,
        @Query("date") date: String
    ): Call<BookOnStatusResponse>

    @Multipart
    @POST("incidentreport.php")
    fun reportIncident(
        @Part("token") token: RequestBody,
        @Part("real_name") realName: RequestBody,
        @Part("org_id") orgId: RequestBody,
        @Part("site_id") siteId: RequestBody,
        @Part("incident_type") incidentType: RequestBody,
        @Part("custom_incident") customIncident: RequestBody?,
        @Part("incident_description") incidentDescription: RequestBody,
        @Part("corrective_action") correctiveAction: RequestBody,
        @Part("severity") severity: RequestBody,
        @Part incidentImage: MultipartBody.Part?,
        @Part correctiveImage: MultipartBody.Part?
    ): Call<IncidentReportResponse>

    @POST("book_on.php")
    fun bookOn(
        @Header("Authorization") token: String,
        @Body request: ClockInRequest
    ): Call<ClockInResponse>

    @POST("clock_out.php")
    fun clockOut(@Body request: ClockOutRequest): Call<ClockOutResponse>

    // Save Patrol Log
    @POST("save_patrol_log.php")
    fun savePatrolLog(@Body request: PatrolLogRequest): Call<BasicResponse>


    @POST("get_filtered_patrol_records.php")
    fun getPatrolRecords(
        @Query("user_id") userId: Int,

        @Query("book_on_time") bookOnTime: String
    ): Call<PatrolResponse>
    @POST("get_patrol_records.php")
    fun getPatrolRecords(
        @Body request: PatrolRecordsRequest
    ): Call<PatrolResponse>

    @POST("get_admin_sites_names.php")
    fun getSiteNames(@Body request: SiteRequest): Call<SiteNamesResponse>
    @POST("get_clock_in_records.php")
    fun getClockInRecords(@Body request: SiteRequest): Call<AdminClockInResponse>
    //admin clock out
    @POST("get_clock_out_records.php")
    fun getClockOutRecords(@Body request: SiteRequest): Call<AdminClockOutResponse>
    //admin Hourly Check
    @POST("get_hourly_checks.php")
    fun getHourlyChecks(@Body request: SiteRequest): Call<HourlyCheckResponse>

    //
    @POST("get_patrols.php")
    fun getPatrols(@Body request: AdminSiteRequest): Call<AdminPatrolResponse>

    // book sessions
    @POST("clock_in.php")
    fun clockIn(
        @Header("Authorization") token: String,
        @Body req: BookSessionRequest
    ): Call<ApiResponse>

    @POST("clock_out_update.php")
    fun clockOut(
        @Header("Authorization") token: String,
        @Body req: BookSessionRequest
    ): Call<ApiResponse>

    @GET("session_history.php")
    fun getSessionHistory(
        @Header("Authorization") token: String,
        @Query("user_id") userId: Int,
        @Query("site_id") siteId: Int,
        @Query("date") date: String
    ): Call<SessionHistoryResponse>


}

