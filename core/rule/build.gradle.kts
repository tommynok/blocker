/*
 * Copyright 2023 Blocker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("blocker.android.library")
    id("blocker.android.library.jacoco")
    id("blocker.android.hilt")
    id("kotlinx-serialization")
    kotlin("kapt")
}

android {
    namespace = "com.merxury.blocker.core.rule"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.componentController)
    implementation(projects.core.datastore)
    implementation(projects.core.ifwApi)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.work.ktx)
    implementation(libs.hilt.android)
    implementation(libs.hilt.ext.work)
    implementation(libs.kotlinx.serialization.json)
    kapt(libs.hilt.compiler)
    kapt(libs.hilt.ext.compiler)
}