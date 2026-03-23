package com.yueliangmanle.danci.core.study

private const val STUDY_ROUTE_BASE = "study"

fun studyRoute(mode: StudyLaunchMode): String = "$STUDY_ROUTE_BASE?mode=${mode.storageValue}"

fun studyRoutePattern(): String = "$STUDY_ROUTE_BASE?mode={mode}"
