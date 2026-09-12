package com.lunchorder.service

import com.lunchorder.domain.DayEvaluations
import com.lunchorder.domain.EvaluationRecord
import com.lunchorder.domain.User
import com.lunchorder.exception.BusinessException
import com.lunchorder.store.DataStore
import com.lunchorder.util.TimeUtil
import com.lunchorder.web.dto.EvaluationsResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * 当天点餐评价（D-012）：
 * - 需当天已登记点餐才能评价（否则 2002）；评价不受点餐窗口限制；
 * - 每用户每天仅保留最后一次评价（重复提交=修改）。
 */
@Service
class EvaluationService(
    private val store: DataStore,
    private val clock: Clock,
) {

    fun myEvaluation(loginName: String, now: ZonedDateTime): EvaluationRecord? =
        store.readEvaluations(now.toLocalDate())?.evaluations?.find { it.loginName == loginName }

    fun upsert(user: User, rating: Int, comment: String?, now: ZonedDateTime): EvaluationRecord {
        if (rating !in 1..5) throw BusinessException(1004, "评分需为 1~5 星")
        val text = (comment ?: "").trim()
        if (text.length > 100) throw BusinessException(1004, "评语不能超过 100 字")

        val date = now.toLocalDate()
        return store.withLock {
            val hasOrder = store.readDay(date)?.orders?.any { it.loginName == user.loginName } ?: false
            if (!hasOrder) throw BusinessException(2002, "请先登记今天的点餐，再进行评价", HttpStatus.NOT_FOUND)

            val day = store.readEvaluations(date) ?: DayEvaluations(date.toString())
            val record = EvaluationRecord(
                loginName = user.loginName,
                displayName = user.displayName,
                rating = rating,
                comment = text,
                ratedAt = TimeUtil.format(now),
            )
            val idx = day.evaluations.indexOfFirst { it.loginName == user.loginName }
            if (idx >= 0) day.evaluations[idx] = record else day.evaluations.add(record)
            store.writeEvaluations(day)
            record
        }
    }

    /** 中台按天查看（D-012）：每用户仅保留最后一次评价，按评价时间升序 */
    fun adminList(date: LocalDate): EvaluationsResponse {
        val evaluations = (store.readEvaluations(date)?.evaluations ?: mutableListOf()).toList()
        val avg = if (evaluations.isEmpty()) 0.0
        else Math.round(evaluations.sumOf { it.rating } * 10.0 / evaluations.size) / 10.0
        return EvaluationsResponse(date = date.toString(), count = evaluations.size, avgRating = avg, evaluations = evaluations)
    }
}
