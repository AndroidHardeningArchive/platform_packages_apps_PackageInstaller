/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.permissioncontroller.tests.mocking.role.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.permissioncontroller.role.model.RoleParserInitializer
import com.android.role.controller.model.RoleParser
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoleParserTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun setupBeforeClass() {
            RoleParserInitializer.initialize()
        }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiAutomation = instrumentation.uiAutomation
    private val targetContext = instrumentation.targetContext

    @Test
    fun testParseRolesWithValidation() {
        // We may need to call privileged APIs to determine availability of things.
        uiAutomation.adoptShellPermissionIdentity()
        try {
            RoleParser(targetContext, true).parse()
        } finally {
            uiAutomation.dropShellPermissionIdentity()
        }
    }
}
