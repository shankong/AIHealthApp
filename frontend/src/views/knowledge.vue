<script setup>
import { ref, onMounted, reactive } from "vue";
import PageHead from "@/components/PageHead.vue";
import TableSearch from "@/components/TableSearch.vue";
import { categoryTree, articlePage, getArticleDetail, changeArticleStatus } from "@/api/admin";
import { Timer } from "@element-plus/icons-vue";
import ArticleDialog from "@/components/ArticleDialog.vue";
import { ElMessageBox } from "element-plus";

const formItem = [
  {
    comp: "input",
    label: "文章标题",
    prop: "title",
    placeholder: "请输入文章标题",
  },
  {
    comp: "select",
    label: "分类",
    prop: "categoryId",
    placeholder: "请选择分类",
  },
  {
    comp: "select",
    label: "状态",
    prop: "status",
    placeholder: "请选择状态",
    options: [
      {
        label: "草稿",
        value: "0",
      },
      {
        label: "已发布",
        value: "1",
      },
      {
        label: "已下线",
        value: "2",
      },
    ],
  },
];

//分页参数
const pagination = reactive({
  currentPage: 1,
  size: 10,
  total: 0,
});
const handleSearch = async (formData) => {
  console.log(formData, "查询参数");

  const paras = {
    ...pagination,
    ...formData,
  };

  const { records, total } = await articlePage(paras);
  tableData.value = records;
  pagination.total = total;
};

const handleChange = (page) => {
  pagination.currentPage = page;
  handleSearch();
};

// 分类映射
const categoryMap = reactive([]);
//分类列表
const categories = ref([]);
//列表数据
const tableData = ref([]);
//弹窗控制
const dialogVisible = ref(false);

onMounted(async () => {
  const res = await categoryTree();
  const list = res.data || res;

  if (Array.isArray(list)) {
    categories.value = list.map((item) => {
      categoryMap[item.id] = item.categoryName;
      return {
        label: item.categoryName,
        value: item.id,
      };
    });
    formItem[1].options = categories.value;
  }

  console.log(categories.value);

  //获取列表
  await handleSearch();
});

// 新增或编辑保存成功后，重新拉一次列表，让数据立刻更新
const handleSuccess = () => {
  handleSearch();
};

// 编辑文章按钮实现
const currentArticle = ref(null);
const handleEdit = (row) => {
  if (!row.id) {
    // 新增文章
    currentArticle.value = null;
    dialogVisible.value = true;
  } else {
    // 编辑文章
    getArticleDetail(row.id).then((res) => {
      currentArticle.value = res;
      dialogVisible.value = true;
    });
  }
};

// 发布文章按钮实现
const handlePublish = (row) => {
  ElMessageBox.confirm(`确认发布文章${row.title}吗？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  }).then(() => {
    updateArticle({
      id: row.id,
      status: 1,
    }).then(() => {
      ElMessage.success("发布成功");
      handleSearch();
    });
  });
};

// 下线文章按钮实现
const handleOffline = (row) => {
  
};
</script>

<template>
  <div>
    <PageHead title="知识文章">
      <template #buttons>
        <el-button @click="handleEdit({})" type="primary">新增</el-button>
      </template>
    </PageHead>
    <TableSearch :formItem="formItem" @search="handleSearch" />
    <el-table :data="tableData" style="width: 100%; margin-top: 25px">
      <el-table-column label="文章标题" width="230" fixed="left">
        <template #default="scope">
          <div style="display: flex; align-items: center">
            <el-icon><timer /></el-icon>
            <span style="margin-left: 10px">{{ scope.row.title }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="分类" width="200">
        <template #default="scope">
          <div style="display: flex; align-items: center">
            <el-icon><timer /></el-icon>
            <span style="margin-left: 10px">{{
              categoryMap[scope.row.categoryId]
            }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="authorName" label="作者" width="150" />
      <el-table-column prop="readCount" label="阅读量" width="150" />
      <el-table-column prop="updateAt" label="发布时间" width="350" />

      <el-table-column label="操作" width="250" fixed="right">
        <template #default="scope">
          <el-button @click="handleEdit(scope.row)" text type="primary"
            >编辑</el-button
          >
          <el-button
            @click="handlePublish(scope.row)"
            v-if="scope.row.status === 0 || scope.row.status === 2"
            text
            type="success"
            >发布</el-button
          >
          <el-button
            @click="handleOffline(scope.row)"
            v-if="scope.row.status === 1"
            text
            type="warning"
            >下线</el-button
          >
          <el-button text type="danger">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="margin-top: 25px"
      :page-size="pagination.size"
      layout="prev, pager, next"
      :total="pagination.total"
      @change="handleChange"
    />
    <ArticleDialog
      v-model:modelValue="dialogVisible"
      :categories="categories"
      @success="handleSuccess"
      :article="currentArticle"
    />
  </div>
</template>

<style scoped lang="scss"></style>
