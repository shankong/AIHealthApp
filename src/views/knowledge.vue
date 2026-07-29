<script setup>
  import PageHead from "@/components/PageHead.vue";
  import TableSearch from "@/components/TableSearch.vue";

  const formItem = [
    { comp : 'input', label: '文章标题', prop: 'title', placeholder: '请输入文章标题'},
    { comp : 'select', label: '分类', prop: 'categoryId', placeholder: '请选择分类', options: [
        { label: '分类1', value: 1},
        { label: '分类2', value: 2}
      ]
    }

  ]

  const handleSearch = (formData) => {
    console.log(formData, "查询参数")
  }
</script>

<template>
  <div>
    <PageHead title="知识文章">
      <template #buttons>
        <el-button type="primary">新增</el-button>
      </template>
    </PageHead>
    <TableSearch :formItem="formItem" @search="handleSearch"/>
  </div>
</template>

<style scoped lang="scss">

</style>