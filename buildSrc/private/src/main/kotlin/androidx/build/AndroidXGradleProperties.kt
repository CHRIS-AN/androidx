/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.build

import androidx.build.dependencyTracker.AffectedModuleDetector
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Setting this property makes Test tasks succeed even if there
 * are some failing tests. Useful when running tests in CI where build
 * passes test results as XML to test reporter.
 */
const val TEST_FAILURES_DO_NOT_FAIL_TEST_TASK = "androidx.ignoreTestFailures"

/**
 * Setting this property to false makes test tasks not display detailed output to stdout.
 */
const val DISPLAY_TEST_OUTPUT = "androidx.displayTestOutput"

/**
 * Setting this property changes "url" property in publishing maven artifact metadata
 */
const val ALTERNATIVE_PROJECT_URL = "androidx.alternativeProjectUrl"

/**
 * Check that version extra meets the specified rules
 * (version is in format major.minor.patch-extra)
 */
const val VERSION_EXTRA_CHECK_ENABLED = "androidx.versionExtraCheckEnabled"

/**
 * Validate the project structure against Jetpack guidelines
 */
const val VALIDATE_PROJECT_STRUCTURE = "androidx.validateProjectStructure"

/**
 * Setting this property enables multiplatform builds of Compose
 */
const val ENABLE_COMPOSE_COMPILER_METRICS = "androidx.enableComposeCompilerMetrics"

/**
 * Setting this property enables multiplatform builds of Compose
 */
const val ENABLE_COMPOSE_COMPILER_REPORTS = "androidx.enableComposeCompilerReports"

/**
 * Returns whether the project should generate documentation.
 */
const val ENABLE_DOCUMENTATION = "androidx.enableDocumentation"

/**
 * Adjusts the set of projects participating in this build.
 * See settings.gradle for more information
 */
const val PROJECT_SUBSET = "androidx.projects"

/**
 * Setting this property puts a summary of the relevant failure messages into standard error
 */
const val SUMMARIZE_STANDARD_ERROR = "androidx.summarizeStderr"

/**
 * Setting this property indicates that a build is being performed to check for forward
 * compatibility.
 */
const val USE_MAX_DEP_VERSIONS = "androidx.useMaxDepVersions"

/**
 * Setting this property enables writing versioned API files
 */
const val WRITE_VERSIONED_API_FILES = "androidx.writeVersionedApiFiles"

/**
 * Build id used to pull SNAPSHOT versions to substitute project dependencies in Playground projects
 */
const val PLAYGROUND_SNAPSHOT_BUILD_ID = "androidx.playground.snapshotBuildId"

/**
 * Build Id used to pull SNAPSHOT version of Metalava for Playground projects
 */
const val PLAYGROUND_METALAVA_BUILD_ID = "androidx.playground.metalavaBuildId"

/**
 * Build Id used to pull SNAPSHOT version of Dokka for Playground projects
 */
const val PLAYGROUND_DOKKA_BUILD_ID = "androidx.playground.dokkaBuildId"

/**
 * Filepath to the java agent of YourKit for profiling
 * If this value is set, profiling via YourKit will automatically be enabled
 */
const val PROFILE_YOURKIT_AGENT_PATH = "androidx.profile.yourkitAgentPath"

/**
 * Specifies to validate that the build doesn't generate any unrecognized messages
 * This prevents developers from inadvertently adding new warnings to the build output
 */
const val VALIDATE_NO_UNRECOGNIZED_MESSAGES = "androidx.validateNoUnrecognizedMessages"

/**
 * Specifies to run the build twice and validate that the second build doesn't run more
 * tasks than expected.
 */
const val VERIFY_UP_TO_DATE = "androidx.verifyUpToDate"

/**
 * If true, we are building in GitHub and should enable build features related to KMP.
 * If false, we are in AOSP, where not all KMP features are enabled.
 */
const val KMP_GITHUB_BUILD = "androidx.github.build"

/**
 * If true, don't require lint-checks project to exist.  This should only be set in
 * integration tests, to allow them to save time by not configuring extra projects.
 */
const val ALLOW_MISSING_LINT_CHECKS_PROJECT = "androidx.allow.missing.lint"

/**
 * If set to a uri, this is the location that will be used to download `xcodegen` when running
 * Darwin benchmarks.
 */
const val XCODEGEN_DOWNLOAD_URI = "androidx.benchmark.darwin.xcodeGenDownloadUri"

/**
 * If true, don't restrict usage of compileSdk property.
 */
const val ALLOW_CUSTOM_COMPILE_SDK = "androidx.allowCustomCompileSdk"

val ALL_ANDROIDX_PROPERTIES = setOf(
    ALTERNATIVE_PROJECT_URL,
    VERSION_EXTRA_CHECK_ENABLED,
    VALIDATE_PROJECT_STRUCTURE,
    COMPOSE_MPP_ENABLED,
    ENABLE_COMPOSE_COMPILER_METRICS,
    ENABLE_COMPOSE_COMPILER_REPORTS,
    DISPLAY_TEST_OUTPUT,
    ENABLE_DOCUMENTATION,
    PROJECT_SUBSET,
    STUDIO_TYPE,
    SUMMARIZE_STANDARD_ERROR,
    USE_MAX_DEP_VERSIONS,
    TEST_FAILURES_DO_NOT_FAIL_TEST_TASK,
    VALIDATE_NO_UNRECOGNIZED_MESSAGES,
    VERIFY_UP_TO_DATE,
    WRITE_VERSIONED_API_FILES,
    AffectedModuleDetector.ENABLE_ARG,
    AffectedModuleDetector.BASE_COMMIT_ARG,
    PLAYGROUND_SNAPSHOT_BUILD_ID,
    PLAYGROUND_METALAVA_BUILD_ID,
    PLAYGROUND_DOKKA_BUILD_ID,
    PROFILE_YOURKIT_AGENT_PATH,
    KMP_GITHUB_BUILD,
    ENABLED_KMP_TARGET_PLATFORMS,
    ALLOW_MISSING_LINT_CHECKS_PROJECT,
    XCODEGEN_DOWNLOAD_URI,
    ALLOW_CUSTOM_COMPILE_SDK
)

/**
 * Returns alternative project url that will be used as "url" property
 * in publishing maven artifact metadata.
 *
 * Returns null if there is no alternative project url.
 */
fun Project.getAlternativeProjectUrl(): String? =
    project.findProperty(ALTERNATIVE_PROJECT_URL) as? String

/**
 * Check that version extra meets the specified rules
 * (version is in format major.minor.patch-extra)
 */
fun Project.isVersionExtraCheckEnabled(): Boolean =
    findBooleanProperty(VERSION_EXTRA_CHECK_ENABLED) ?: true

/**
 * Validate the project structure against Jetpack guidelines
 */
fun Project.isValidateProjectStructureEnabled(): Boolean =
    findBooleanProperty(VALIDATE_PROJECT_STRUCTURE) ?: true

/**
 * Validates that all properties passed by the user of the form "-Pandroidx.*" are not misspelled
 */
fun Project.validateAllAndroidxArgumentsAreRecognized() {
    for (propertyName in project.properties.keys) {
        if (propertyName.startsWith("androidx")) {
            if (!ALL_ANDROIDX_PROPERTIES.contains(propertyName)) {
                val message = "Unrecognized Androidx property '$propertyName'.\n" +
                    "\n" +
                    "Is this a misspelling? All recognized Androidx properties:\n" +
                    ALL_ANDROIDX_PROPERTIES.joinToString("\n") + "\n" +
                    "\n" +
                    "See AndroidXGradleProperties.kt if you need to add this property to " +
                    "the list of known properties."
                throw GradleException(message)
            }
        }
    }
}

/**
 * Returns whether tests in the project should display output.
 * Build server scripts generally set displayTestOutput to false so that their failing test
 * results aren't considered build failures, and instead pass their test failures on via build
 * artifacts to be tracked and displayed on test dashboards in a different format
 */
fun Project.isDisplayTestOutput(): Boolean = findBooleanProperty(DISPLAY_TEST_OUTPUT) ?: true

/**
 * Returns whether the project should write versioned API files, e.g. `1.1.0-alpha01.txt`.
 * <p>
 * When set to `true`, the `updateApi` task will write the current API surface to both `current.txt`
 * and `<version>.txt`. When set to `false`, only `current.txt` will be written. The default value
 * is `true`.
 */
fun Project.isWriteVersionedApiFilesEnabled(): Boolean =
    findBooleanProperty(WRITE_VERSIONED_API_FILES) ?: true

/**
 * Returns whether the project should generate documentation.
 */
fun Project.isDocumentationEnabled(): Boolean {
    if (System.getenv().containsKey("ANDROIDX_PROJECTS")) {
        val projects = System.getenv()["ANDROIDX_PROJECTS"] as String
        if (projects != "ALL") return false
    }
    return (project.findProperty(ENABLE_DOCUMENTATION) as? String)?.toBoolean() ?: true
}

/**
 * Returns whether the build is for checking forward compatibility across projects
 */
fun Project.usingMaxDepVersions(): Boolean {
    return project.hasProperty(USE_MAX_DEP_VERSIONS)
}

/**
 * Returns whether this is an integration test that is allowing lint checks to be skipped to
 * save configuration time.
 */
fun Project.allowMissingLintProject() =
    findBooleanProperty(ALLOW_MISSING_LINT_CHECKS_PROJECT) ?: false

/**
 * Whether libraries are allowed to customize the value of the compileSdk property.
 */
fun Project.isCustomCompileSdkAllowed(): Boolean =
    findBooleanProperty(ALLOW_CUSTOM_COMPILE_SDK) ?: true

fun Project.findBooleanProperty(propName: String) = (findProperty(propName) as? String)?.toBoolean()