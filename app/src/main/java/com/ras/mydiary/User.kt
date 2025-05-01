data class User(
    val uid: String = "",
    val username: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "", // "user", "therapist", "admin"
    val oneSignalId: String = "",
    val profileImageUrl: String = ""
)