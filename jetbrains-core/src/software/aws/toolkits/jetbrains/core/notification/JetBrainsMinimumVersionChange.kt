// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core.notification

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import software.aws.toolkits.resources.message

class JetBrainsMinimumVersionChange : NoticeType {
    override val id: String = "JetBrainsMinimumVersion_192"
    private val noticeContents = NoticeContents(
        message("notice.title.jetbrains.minimum.version.2019.2"),
        message(
            "notice.message.jetbrains.minimum.version.2019.2",
            ApplicationInfo.getInstance().fullVersion,
            ApplicationNamesInfo.getInstance().fullProductName
        )
    )

    override fun getSuppressNotificationValue(): String = ApplicationInfo.getInstance().fullVersion

    override fun isNotificationSuppressed(previousSuppressNotificationValue: String?): Boolean {
        previousSuppressNotificationValue?.let {
            return previousSuppressNotificationValue == getSuppressNotificationValue()
        }
        return false
    }

    override fun isNotificationRequired(): Boolean {
        val appInfo = ApplicationInfo.getInstance()
        val majorVersion = appInfo.majorVersion.toIntOrNull()
        val minorVersion = appInfo.minorVersion.toFloatOrNull()

        majorVersion?.let {
            minorVersion?.let {
                return majorVersion < 2019 || (majorVersion == 2019 && minorVersion < 2)
            }
        }

        return true
    }

    override fun getNoticeContents(): NoticeContents = noticeContents
}
