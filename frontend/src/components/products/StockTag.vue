<template>
  <el-tag v-if="stock === 0" type="info" size="small" effect="dark">已售罄</el-tag>
  <div v-else-if="stock < threshold" style="color: #f56c6c; display: flex; align-items: center;">
    <el-icon style="margin-right: 4px"><Warning /></el-icon>{{ stock }}
  </div>
  <span v-else>{{ stock }}</span>
</template>

<script setup>
import { defineProps } from 'vue'
import { Warning } from '@element-plus/icons-vue'

const props = defineProps({
  stock: Number,
  threshold: { type: Number, default: 10 }
})
</script>

<style scoped>
div { font-weight: bold; }
</style>