package com.yueliangmanle.danci.core.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyRouteSpecTest {

    @Test
    fun studyRouteSpec_buildsModeAwareRoute() {
        assertEquals("study?mode=new_words", studyRoute(StudyLaunchMode.NEW_WORDS))
        assertEquals("study?mode=review", studyRoute(StudyLaunchMode.REVIEW))
        assertEquals("study?mode=recent_mistakes", studyRoute(StudyLaunchMode.RECENT_MISTAKES))
    }

    @Test
    fun studyRouteSpec_matchesModeAwareStudyRoutes() {
        assertTrue(isModeAwareStudyRoute(studyRoutePattern()))
        assertTrue(isModeAwareStudyRoute(studyRoute(StudyLaunchMode.NEW_WORDS)))
        assertFalse(isModeAwareStudyRoute("STUDY"))
        assertFalse(isModeAwareStudyRoute("word_detail/1"))
    }

    @Test
    fun studyRouteSpec_parsesModeArgument() {
        assertEquals(StudyLaunchMode.NEW_WORDS, parseStudyLaunchMode("new_words"))
        assertEquals(StudyLaunchMode.REVIEW, parseStudyLaunchMode("review"))
        assertEquals(StudyLaunchMode.RECENT_MISTAKES, parseStudyLaunchMode("recent_mistakes"))
        assertEquals(null, parseStudyLaunchMode("unknown"))
        assertEquals(null, parseStudyLaunchMode(null))
    }
}
