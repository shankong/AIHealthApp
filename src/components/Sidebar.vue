<script setup>
  import router from "@/router/index.js";

  const handleOpen = () => {}
  const handleClose = () => {}
  const selectMenu = (key) => {
    console.log(key)
    const currentRoute = router.options.routes[0]
    console.log(currentRoute)
    router.push(`${currentRoute.path}/${key.index}`)
  }
</script>

<template>
  <el-aside width="256px">
    <el-menu
        default-active="2"
        class="menu-style"
        @open="handleOpen"
        @close="handleClose"
    >
      <div class="brand">
        <h1 class="brand-title">心理健康AI助手</h1>
        <p class="brand-description">后台管理</p>
      </div>
      <el-menu-item @click="selectMenu" v-for="item in router.options.routes[0].children" :key="item.path" :index="item.path">
        <el-icon> <component :is="item.meta.icon"/> </el-icon>
        <span>{{item.meta.title}}</span>
      </el-menu-item>
    </el-menu>
  </el-aside>
</template>

<style scoped lang="scss">
  .menu-style{
    height: 100%;
    .brand{
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      padding: 20px;
      border-bottom: 1px solid #ddd;
      .brand-title{
        font-size: 20px;
        font-weight: bold;
      }
      .brand-description{
        margin-top: 10px;
        font-size: 12px;
        color: #999;
      }
    }
  }
</style>