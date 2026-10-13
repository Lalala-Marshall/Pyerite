package com.marshall.pyerite.infra.notification

/** User choice for the app-wide notification switch. [FOLLOW_PERMISSION] means the user has not changed it. */
enum class NotificationMasterChoice {
    FOLLOW_PERMISSION,
    ENABLED,
    DISABLED,
    ;

    companion object {
        fun fromStoredName(name: String?): NotificationMasterChoice =
            entries.firstOrNull { it.name == name } ?: FOLLOW_PERMISSION
    }
}
