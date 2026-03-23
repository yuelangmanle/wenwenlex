package com.yueliangmanle.danci.core.study

import org.junit.Assert.assertEquals
import org.junit.Test

class StudyRouteSpecTest {

    @Test
    fun studyRouteSpec_buildsModeAwareRoute() {
        assertEquals("study?mode=new_words", studyRoute(StudyLaunchMode.NEW_WORDS))
        assertEquals("study?mode=review", studyRoute(StudyLaunchMode.REVIEW))
        assertEquals("study?mode=recent_mistakes", studyRoute(StudyLaunchMode.RECENT_MISTAKES))
    }
}
