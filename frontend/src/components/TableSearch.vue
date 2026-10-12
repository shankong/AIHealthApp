<script setup>
  import {ref, reactive, computed} from "vue";

  const props = defineProps({
    formItem: {
      type: Array,
      // 原来写的是 () => {}，返回 undefined 而不是数组，
      // 父组件一旦不传 formItem，下面的 map 会直接报错
      default: () => []
    }
  })
  const emit = defineEmits(['search'])
  // 不要直接改 props 里的 item：
  // 一是改 props 本身就不规范，二是"在 computed 里写自己依赖的响应式数据"
  // 会让计算属性反复失效。改成返回一批新对象，各自带上该占的栅格宽度。
  const formItemAttrs = computed(() =>
    props.formItem.map(item => ({
      ...item,
      col: { xs: 24, sm: 12, md: 8, lg: 6, xl: 4 }
    }))
  )
  const ruleFormRef = ref()
  const formData = reactive({})
  const isComp = (comp) => {
    return {
      input: 'elInput',
      select: 'elSelect'
    }[comp]
  }
  const handleSearch = () => {
    //console.log(formData)
    emit('search', formData)
  }
  const handleReset = (formEl) => {
    if(!formEl) return
    formEl.resetFields()
    emit('search', formData)
  }
</script>

<template>
  <el-form ref="ruleFormRef" :model="formData">
    <el-row :gutter="24">
      <template  v-for="item in formItemAttrs" :key="item.props">
        <el-col v-bind="item.col">
          <el-form-item :label="item.label" :prop="item.prop">
            <component v-model="formData[item.prop]" :is="isComp(item.comp)" :placeholder="item.placeholder">
              <template v-if="item.comp === 'select'">
                <el-option label="全部" value="" />
                <el-option
                    v-for="option in item.options"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value" />
              </template>
            </component>
          </el-form-item>
        </el-col>
      </template>
    </el-row>

    <el-row>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset(ruleFormRef)">重置</el-button>
    </el-row>
  </el-form>
</template>

<style scoped lang="scss">

</style>