<script setup>
  const props = defineProps({
    title: {
      type: String,
      default: '知识文章'
    }
  })
</script>

<template>
  <div class="page-head">
    <p class="page-title">{{ props.title }}</p>
    <div class="action-btn">
      <slot name="buttons"></slot>
    </div>
  </div>
</template>

<style scoped lang="scss">
  .page-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .page-title {
      font-size: 24px;
      color: #2d3748;
    }
  }
</style>