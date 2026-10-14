package com.marshall.pyerite.sdeModule.update

data class SdeRemotePackage(
    val meta: SdeReleaseMeta,
    val tagName: String,
    val iconPartCount: Int = 0,
)
