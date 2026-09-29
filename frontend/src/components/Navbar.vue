<script setup>
  import {Operation} from "@element-plus/icons-vue";
  import { ArrowDown } from '@element-plus/icons-vue'
  import { useAdminStore } from '@/stores/admin.js'

  const handleCommand = (command) => {
    // command
    if (command === 'logout'){

    }
  }

  const handleCollapse = () => {
    // collapse
    useAdminStore().toggleCollapse()
  }
</script>

<template>
  <div class="navbar">
    <div class="flex-box">
      <el-button @click="handleCollapse">
        <el-icon><Operation /></el-icon>
      </el-button>
      <p class="page-title">导航栏</p>
    </div>
    <div class="flex-box">
      <el-dropdown @command="handleCommand">
        <span class="el-dropdown-link">
          admin
          <el-icon class="el-icon--right">
            <arrow-down />
          </el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style scoped lang="scss">
  .navbar {
      background-color: #fff;
      height: 100%;
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0 20px;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
      border-bottom: 1px solid #ddd;
      .example-showcase .el-dropdown-link {
        cursor: pointer;
        color: var(--el-color-primary);
        display: flex;
        align-items: center;
      }
      .flex-box {
        border: 0;
        display: flex;
        align-items: center;
      }
      .page-title {
        margin-left: 20px;
        font-size: 26px;
        font-weight: bold;
        color: #1f2937;
      }
  }
</style>

<style lang="scss">
  .el-header {
    padding: 0;
  }
</style>