<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { api } from '../api'
import type { NoticeItem } from '../api/types'
import { userStore } from '../store/user'

/**
 * 重要通知横幅（D-015）：登录后展示在顶部；
 * 折叠时只显示最新一条，可展开查看全部。
 */
const notices = ref<NoticeItem[]>([])
const expanded = ref(false)
const visible = computed(() => userStore.loaded && notices.value.length > 0)
const latest = computed(() => notices.value[0] ?? null)

async function load() {
  if (!userStore.loaded) {
    notices.value = []
    return
  }
  try {
    notices.value = (await api.getNotices()).notices
  } catch {
    /* 通知拉取失败不打扰主流程 */
  }
}

onMounted(load)
watch(
  () => userStore.loaded,
  (loaded) => {
    if (loaded) void load()
    else notices.value = []
  },
)
</script>

<template>
  <div v-if="visible" class="notice-banner" data-test="notice-banner">
    <div class="notice-latest">
      <span class="notice-icon">📢</span>
      <div class="notice-body">
        <b class="notice-title">{{ latest?.title }}</b>
        <span class="notice-content">{{ latest?.content }}</span>
        <span class="notice-time">{{ latest?.createdAt.slice(0, 10) }}</span>
      </div>
      <el-button
        v-if="notices.length > 1"
        link
        type="primary"
        size="small"
        data-test="notice-toggle"
        @click="expanded = !expanded"
      >
        {{ expanded ? '收起' : `展开全部（${notices.length} 条）` }}
      </el-button>
    </div>
    <div v-if="expanded" class="notice-all" data-test="notice-all">
      <div v-for="n in notices" :key="n.id" class="notice-item">
        <div class="notice-item-head">
          <b>📢 {{ n.title }}</b>
          <span class="notice-time">{{ n.createdAt.slice(0, 10) }} · {{ n.createdBy }}</span>
        </div>
        <div class="notice-item-content">{{ n.content }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notice-banner {
  background: #fdf6ec;
  border-bottom: 1px solid #faecd8;
  padding: 8px 20px;
}
.notice-latest {
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 1100px;
  margin: 0 auto;
}
.notice-icon {
  font-size: 18px;
}
.notice-body {
  flex: 1;
  min-width: 0;
  color: #e6a23c;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.notice-title {
  flex-shrink: 0;
}
.notice-content {
  overflow: hidden;
  text-overflow: ellipsis;
}
.notice-time {
  color: #c8b28e;
  flex-shrink: 0;
  font-size: 12px;
}
.notice-all {
  max-width: 1100px;
  margin: 8px auto 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.notice-item {
  background: #fff;
  border: 1px solid #faecd8;
  border-radius: 8px;
  padding: 10px 12px;
}
.notice-item-head {
  display: flex;
  justify-content: space-between;
  color: #e6a23c;
  font-size: 13px;
  margin-bottom: 4px;
}
.notice-item-content {
  color: #606266;
  font-size: 13px;
  white-space: pre-wrap;
}
</style>
