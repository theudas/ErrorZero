package com.errorzero.app.review

import com.errorzero.app.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewScheduleTest {
    private val now = System.currentTimeMillis()

    @Test
    fun firstReviewIsNextDay() {
        assertEquals(1L, Dates.daysFromToday(ReviewSchedule.first(now)))
    }

    @Test
    fun masteredAdvancesThroughIntervals() {
        var stage = 0
        for (i in 1 until ReviewSchedule.ROUNDS) {
            val r = ReviewSchedule.afterReview(stage, mastered = true, now = now)
            assertEquals(i, r.stage)
            assertEquals(ReviewSchedule.INTERVALS[i].toLong(), Dates.daysFromToday(r.next))
            assertFalse(r.justCompleted)
            stage = r.stage
        }
        val last = ReviewSchedule.afterReview(stage, mastered = true, now = now)
        assertEquals(ReviewSchedule.ROUNDS, last.stage)
        assertTrue(last.justCompleted)
        assertEquals(30L, Dates.daysFromToday(last.next))

        // 全部完成后继续掌握，不再重复提示归档
        val again = ReviewSchedule.afterReview(last.stage, mastered = true, now = now)
        assertFalse(again.justCompleted)
        assertEquals(30L, Dates.daysFromToday(again.next))
    }

    @Test
    fun notMasteredResetsToFirstRound() {
        val r = ReviewSchedule.afterReview(4, mastered = false, now = now)
        assertEquals(0, r.stage)
        assertEquals(1L, Dates.daysFromToday(r.next))
    }

    @Test
    fun dueLabels() {
        assertEquals("今日重做", Dates.dueLabel(Dates.startOfToday()))
        assertEquals("逾期 2 天", Dates.dueLabel(Dates.startOfDayPlus(now, -2)))
        assertEquals("明天", Dates.dueLabel(Dates.startOfDayPlus(now, 1)))
    }
}
