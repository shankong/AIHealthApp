<script setup>
  import {Back} from "@element-plus/icons-vue"
  import {reactive, ref} from "vue"
  import { login } from "@/api/admin"

  const ruleFormRef = ref()
  const formData = reactive({
    username: '',
    password: ''
  })
  const rules = {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 10, message: '长度在 6 到 10 个字符', trigger: 'blur' }
    ]
  }
  const submitForm = async (formEl) => {
    if (!formEl) return
    await formEl.validate((valid, fields) => {
      if (valid) {
        login(formData).then(data => {
          //判断token是否存在
          if (!data.token) {
            return console.error('登录失败')
          }
          //登陆成功，保存token和用户信息
          localStorage.setItem('token', data.token)
          localStorage.setItem('userInfo', JSON.stringify(data.userInfo))

        })
      }
    })
  }
</script>

<template>
  <div class="container">
    <div class="title">
      <div class="back-home">
        <el-icon><Back /></el-icon>
        <span>返回首页</span>
      </div>
      <div class="title-text">
        <h2>登陆您的账户</h2>
        <p>请输入您的登录信息</p>
      </div>
    </div>
    <el-form
        :model="formData"
        ref="ruleFormRef"
        :rules="rules"
        label-position="top"
        class="login-form">
      <el-form-item label="用户名和邮箱" prop="username">
        <el-input v-model="formData.username" size="large" placeholder="请输入用户名和邮箱"></el-input>
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="formData.password" size="large" placeholder="请输入密码" type="password" show-password></el-input>
      </el-form-item>
      <el-button class="login-btn" type="primary" size="large" @click="submitForm(ruleFormRef)">登录</el-button>
      <div class="footer">
        <p>还没有账户? <router-link to="/auth/register" class="router-link-color">注册</router-link></p>
      </div>
    </el-form>
  </div>
</template>

<style scoped lang="scss">
  .container {
    width: 384px;
    .title {
      .back-home{
        margin-bottom: 60px;
      }
      .title-text {
        text-align: center;
        h2 {
          font-size: 36px;
          margin-bottom: 10px;
        }
        p{
          font-size: 18px;
          color: #6b7280;
        }
      }
    }
    .login-form{
      margin-top: 30px;
      .login-btn{
        margin-top: 40px;
        width: 100%;
      }
      .footer{
        padding: 30px;
        text-align: center;
        .router-link-color{
          color: blue;
        }
      }
    }
  }
</style>