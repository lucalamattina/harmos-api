package ar.edu.itba.harmos.models

enum class AppUserRole(val roleName: String) {
    DOCTOR("DOCTOR"),
    ADMINISTRATOR("ADMINISTRATOR");

    companion object {
        fun fromRoleName(roleName: String): AppUserRole? = values().find { it.roleName.equals(roleName, ignoreCase = true) }
    }
}