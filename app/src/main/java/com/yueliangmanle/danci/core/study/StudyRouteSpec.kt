package com.yueliangmanle.danci.core.study

private const val STUDY_ROUTE_BASE = "study"
private const val STUDY_MODE_QUERY_PREFIX = "$STUDY_ROUTE_BASE?mode="

fun studyRoute(mode: StudyLaunchMode): String = "$STUDY_ROUTE_BASE?mode=${mode.storageValue}"

fun studyRoutePattern(): String = "$STUDY_ROUTE_BASE?mode={mode}"

fun isModeAwareStudyRoute(route: String): Boolean = route.startsWith(STUDY_MODE_QUERY_PREFIX)

fun parseStudyLaunchMode(mode: String?): StudyLaunchMode? {
    return StudyLaunchMode.entries.firstOrNull { it.storageValue == mode }
}
