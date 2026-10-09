package com.errorzero.app.review

import com.errorzero.app.util.Dates

/**
 * 基于艾宾浩斯遗忘曲线的重做间隔。
 * 录入后 1 天第一次重做；每次"已掌握"进入下一轮，"仍不会"回到第一轮。
 * 六轮全部掌握后视为完成，之后每 30 天提醒一次，直到归档。
 */
object ReviewSchedule {
    val INTERVALS = intArrayOf(1, 2, 4, 7, 15, 30)
    val ROUNDS = INTERVALS.size

    fun first(now: Long): Long = Dates.startOfDayPlus(now, INTERVALS[0])

    data class Outcome(val stage: Int, val next: Long, val justCompleted: Boolean)

    fun afterReview(stage: Int, mastered: Boolean, now: Long): Outcome {
        if (!mastered) return Outcome(0, Dates.startOfDayPlus(now, INTERVALS[0]), false)
        val newStage = stage + 1
        return if (newStage >= ROUNDS) {
            Outcome(ROUNDS, Dates.startOfDayPlus(now, INTERVALS.last()), stage == ROUNDS - 1)
        } else {
            Outcome(newStage, Dates.startOfDayPlus(now, INTERVALS[newStage]), false)
        }
    }

    fun progressLabel(stage: Int): String =
        if (stage >= ROUNDS) "已完成 $ROUNDS 轮" else "第 ${stage + 1} / $ROUNDS 轮"
}
