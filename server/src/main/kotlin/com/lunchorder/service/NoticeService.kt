package com.lunchorder.service

import com.lunchorder.domain.NoticeItem
import com.lunchorder.domain.NoticesFile
import com.lunchorder.exception.BusinessException
import com.lunchorder.store.DataStore
import com.lunchorder.util.TimeUtil
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Clock

/**
 * 重要通知（D-015）：
 * - 管理员可发布多条、可删除；所有登录用户可查看；
 * - 展示按 id 倒序（最新在前），客户端折叠时只显示最新一条。
 */
@Service
class NoticeService(
    private val store: DataStore,
    private val clock: Clock,
) {

    fun list(): List<NoticeItem> = store.readNotices().notices.sortedByDescending { it.id }

    fun create(title: String, content: String, createdBy: String): NoticeItem {
        val t = title.trim()
        if (t.isEmpty() || t.length > 50) throw BusinessException(1004, "通知标题需为 1~50 字")
        val c = content.trim()
        if (c.isEmpty() || c.length > 500) throw BusinessException(1004, "通知内容需为 1~500 字")

        return store.withLock {
            val file = store.readNotices()
            val id = (file.notices.maxOfOrNull { it.id } ?: 0L) + 1
            val notice = NoticeItem(
                id = id,
                title = t,
                content = c,
                createdAt = TimeUtil.nowString(clock),
                createdBy = createdBy,
            )
            file.notices.add(notice)
            store.writeNotices(file)
            notice
        }
    }

    fun delete(id: Long) {
        store.withLock {
            val file = store.readNotices()
            val removed = file.notices.removeIf { it.id == id }
            if (!removed) throw BusinessException(2002, "通知不存在或已删除", HttpStatus.NOT_FOUND)
            store.writeNotices(file)
        }
    }
}
