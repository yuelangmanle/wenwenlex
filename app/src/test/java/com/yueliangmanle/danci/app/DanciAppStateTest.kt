package com.yueliangmanle.danci.app

import com.yueliangmanle.danci.core.study.StudyLaunchMode
import com.yueliangmanle.danci.core.study.studyRoute
import com.yueliangmanle.danci.core.study.studyRoutePattern
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DanciAppStateTest {

    @Test
    fun studyTopLevelRoute_matchesModeAwareStudyRoutes() {
        assertTrue(isStudyTopLevelRoute(TopLevelDestination.STUDY.name))
        assertTrue(isStudyTopLevelRoute(studyRoutePattern()))
        assertTrue(isStudyTopLevelRoute(studyRoute(StudyLaunchMode.NEW_WORDS)))
        assertTrue(isStudyTopLevelRoute("word_detail/1"))
        assertTrue(isStudyTopLevelRoute("quiz/1"))
        assertFalse(isStudyTopLevelRoute(TopLevelDestination.HOME.name))
    }
}
