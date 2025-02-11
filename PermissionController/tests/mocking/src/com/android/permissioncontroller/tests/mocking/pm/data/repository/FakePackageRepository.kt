/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.permissioncontroller.tests.mocking.pm.data.repository

import android.graphics.drawable.Drawable
import android.os.UserHandle
import com.android.permissioncontroller.pm.data.model.v31.PackageAttributionModel
import com.android.permissioncontroller.pm.data.model.v31.PackageInfoModel
import com.android.permissioncontroller.pm.data.repository.v31.PackageRepository

class FakePackageRepository(
    private val packages: Map<String, PackageInfoModel> = emptyMap(),
    private val packageAttributions: Map<String, PackageAttributionModel> = emptyMap(),
    private val packagesAndLabels: Map<String, String> = emptyMap(),
) : PackageRepository {
    override fun getPackageLabel(packageName: String, user: UserHandle): String {
        return packagesAndLabels[packageName] ?: packageName
    }

    override fun getBadgedPackageIcon(packageName: String, user: UserHandle): Drawable? {
        return null
    }

    override suspend fun getPackageInfo(
        packageName: String,
        user: UserHandle,
        flags: Int
    ): PackageInfoModel? {
        return packages[packageName]
    }

    override suspend fun getPackageAttributionInfo(
        packageName: String,
        user: UserHandle
    ): PackageAttributionModel? {
        return packageAttributions[packageName]
    }
}
