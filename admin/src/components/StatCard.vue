<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  label: string
  value: number | string
  icon: Component
  /** 主题色（PALETTE 之一） */
  color: string
}>()
</script>

<template>
  <el-card shadow="hover" class="stat-card">
    <div class="stat-card__inner">
      <div class="stat-card__icon" :style="{ color, backgroundColor: `${color}1a` }">
        <el-icon :size="26">
          <component :is="icon" />
        </el-icon>
      </div>
      <div class="stat-card__meta">
        <div class="stat-card__value" :style="{ color }">{{ value }}</div>
        <div class="stat-card__label">{{ label }}</div>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.stat-card {
  height: 100%;
}

.stat-card__inner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 6px 4px;
}

.stat-card__icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card__value {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.stat-card__label {
  margin-top: 2px;
  font-size: 13px;
  color: #909399;
}
</style>
