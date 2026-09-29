<script setup>

</script>

<template>
  <div class="auth-layout">
    <div class="left-section">
      <div class="content">
        <h2 class="title">心理AI助手</h2>
        <p class="text">
          AIPsychology 是一个基于人工智能的心理咨询平台，通过AI技术，为用户提供心理咨询建议，帮助用户解决心理问题。
        </p>
      </div>
    </div>
    <div class="right-section">
      <router-view></router-view>
    </div>
  </div>

</template>

<style scoped lang="scss">
.auth-layout {
  display: flex;
  min-height: 100vh;
}

.left-section {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: rgb(84, 140, 125);

  .content {
    text-align: center;
    padding: 40px;
    color: #fff;
  }

  .title {
    font-size: 36px;
    font-weight: bold;
    margin-bottom: 20px;
    color: #fff;
  }

  .text {
    font-size: 16px;
    line-height: 1.8;
    max-width: 400px;
    color: rgba(255, 255, 255, 0.85);
  }
}

.right-section {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #fff;
}
</style>