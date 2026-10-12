import axios from "axios"
import {ElMessage} from "element-plus"

//创建axios实例
const service = axios.create({
    baseURL: '/api',
    timeout: 5000,
})

//请求拦截器
service.interceptors.request.use(
    config => {
        //在发送请求之前做些什么
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['token'] = token
        }
        return config
    },
    error => {
        //对请求错误做些什么
        return Promise.reject(error)
    }
)

//响应拦截器
service.interceptors.response.use(
    (response) => {
        //对响应数据做些什么
        const { data, config } = response
        //处理业务状态码。
        // 外面包一层 String()：后端有的接口返回字符串 "200"，有的返回数字 200，
        // 只写 === '200' 的话，返回数字的那些请求会全部被判成失败
        const code = String(data?.code)

        //成功：只把真正的业务数据交出去
        if (code === '200') {
            return data.data
        }

        //登录态失效
        if (code === '-1') {
            ElMessage.error(data.msg || '登陆过期，请重新登陆')
            if (!config.url?.includes('login')) {
                localStorage.removeItem('token')
                localStorage.removeItem('userInfo')
                window.location.href = '/auth/login'
            }
        } else {
            //其他业务失败码：提示 + reject。
            // 原来这里会直接 return response，于是调用方拿到的不是数据而是整个响应对象，
            // 解构出来全是 undefined，界面上又没有任何报错，非常难排查
            ElMessage.error(data?.msg || '请求失败')
        }

        return Promise.reject(data)
    },
    (error) => {
        //对响应错误做些什么
        return Promise.reject(error)
    }
)

export default service






















