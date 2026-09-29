<script setup>
import { ref, reactive, computed, nextTick, watch } from "vue";
import { ElMessage } from "element-plus";
import { uploadFile, createArticle, updateArticle, getArticleDetail } from "@/api/admin";
import { fileBaseUrl } from "@/config/index";
import RichTextEditor from "@/components/RichTextEditor.vue";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  categories: {
    type: Array,
    default: () => [],
  },
  // 编辑时父组件把文章数据传进来，新增时传 null
  // 注意属性名要和父组件的 :article="..." 一致，之前两边写的名字对不上，
  // 导致父组件传的数据这里压根收不到
  article: {
    type: Object,
    default: null,
  },
});

// 有 id 说明是在编辑已有文章，没有 id 就是新增
const isEdit = computed(() => !!props.article?.id);

const emit = defineEmits(["update:modelValue", "success"]);

const dialogVisible = computed({
  get() {
    return props.modelValue;
  },
  set(val) {
    emit("update:modelValue", val);
  },
});

const rules = {
  title: [{ required: true, message: "请输入文章标题", trigger: "blur" }],
  categoryId: [{ required: true, message: "请选择分类", trigger: "change" }],
  status: [{ required: true, message: "请输入文章内容", trigger: "blur" }],
  content: [{ max: 5000, message: "文章内容最多5000字", trigger: "blur" }],
};

// 表单实例和提交按钮的 loading 状态。
// 提前声明是因为下面的重置方法里要用到 formRef
const formRef = ref();
const loading = ref(false);

// 表单数据
const formData = reactive({
  id: "", // 有 id 表示编辑，没有就是新增
  title: "",
  content: "",
  coverImage: "",
  categoryId: 1,
  summary: "",
  tags: "",
  status: 0, // 0 草稿、1 已发布、2 已下线，编辑时要原样带回去
  tagArray: [],
});

const commonTags = [
  "情绪管理",
  "焦虑",
  "压力管理",
  "时间管理",
  "自我提升",
  "人际关系",
  "职业发展",
  "健康生活",
  "理财知识",
  "科技新闻",
];

//上传图片
const imageUrl = ref("");
const beforeUpload = (file) => {
  //上传图片进行校验
  const isImage = (file) => file.type.startsWith("image/");
  const isLt5M = (file) => file.size / 1024 / 1024 < 5;

  if (!isImage(file)) {
    ElMessage.error("上传图片只能是图片格式!");
    return false;
  }

  if (!isLt5M(file)) {
    ElMessage.error("上传图片大小不能超过 5MB!");
    return false;
  }
  return true;
};

//处理上传图片请求
const handleUploadRequest = async ({ file }) => {
  //生成UUID作为图片名
  const businessId = crypto.randomUUID();

  const fileRes = await uploadFile(file, {
    businessId: businessId,
  });
  //console.log(fileRes)

  // 处理上传成功后的图片URL
  // 地址必须用后端返回的 filePath（它才是文件真正的存储位置，带后缀）
  // 不能拿自己生成的 businessId 去拼，那样端口号会和 UUID 粘在一起，地址是错的
  // fileBaseUrl 结尾没有斜杠，所以这里要保证拼接处有且只有一个斜杠：
  // 后端返回 "/upload/a.png" 就直接用；返回 "upload/a.png" 就补一个
  const filePath = fileRes.filePath?.startsWith("/")
    ? fileRes.filePath
    : `/${fileRes.filePath}`;

  imageUrl.value = `${fileBaseUrl}${filePath}`;
  formData.coverImage = fileRes.filePath;
};

// 移除封面图片
const removeCover = () => {
  imageUrl.value = "";
  formData.coverImage = "";
};

// 把后端返回的相对路径拼成完整地址，保证中间只有一个斜杠
const normalizePath = (path) => (path.startsWith("/") ? path : `/${path}`);

// 编辑时：把文章数据回填进表单
const fillForm = (article) => {
  formData.id = article.id ?? "";
  formData.title = article.title ?? "";
  formData.content = article.content ?? "";
  formData.summary = article.summary ?? "";
  formData.categoryId = article.categoryId ?? 1;
  formData.status = article.status ?? 0;
  formData.coverImage = article.coverImage ?? "";

  // 标签在数据库里存的是逗号分隔的字符串，而多选框需要的是数组，所以要转一下
  formData.tagArray = article.tags
    ? article.tags.split(",").filter(Boolean)
    : [];

  // 已有封面要把地址拼出来，否则编辑时看不到原来那张图
  imageUrl.value = formData.coverImage
    ? `${fileBaseUrl}${normalizePath(formData.coverImage)}`
    : "";
};

// 新增时：把表单清空。
// el-dialog 默认不销毁内容，不清空的话，编辑完再点"新增"
// 会看到上一次留下的数据，容易误提交
const resetForm = () => {
  formData.id = "";
  formData.title = "";
  formData.content = "";
  formData.summary = "";
  formData.categoryId = 1;
  formData.status = 0;
  formData.coverImage = "";
  formData.tagArray = [];
  imageUrl.value = "";
  // 清掉上一次校验留下的红色提示文字
  formRef.value?.clearValidate();
};

const handleClose = () => {
  resetForm();
};

// 弹窗打开时决定这次是"编辑"还是"新增"：
// 父组件是先拿到文章详情、再把弹窗打开的，
// 所以这里读取 props.article 时数据已经准备好了
watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return;
    if (isEdit.value) {
      fillForm(props.article);
    } else {
      resetForm();
    }
  },
);

// 富文本区域
// 注意：编辑器的 change 事件直接抛出 HTML 字符串，不是对象，别写成 data.html
// 其实已经有 v-model 在同步内容了，这里只是留个钩子方便以后做字数统计之类的
const handleContentChange = (html) => {
  // console.log(html, "富文本内容");
  formData.content = html;
};
const editorInstance = ref(null);
const handleEditorCreated = (editor) => {
  editorInstance.value = editor;
  // 初始化富文本内容
  if (formData.content && editor) {
    nextTick(() => {
      editor.setHtml(formData.content);
    });
  }
};

const btnPreview = ref(false);

// 提交表单
const handleSubmit = async () => {
  // validate() 校验不通过时会 reject，用 catch 兜住转成 false，避免报未处理的错误
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    const submitData = {
      ...formData,
      // 提交时把标签数组拼回逗号分隔的字符串，和后端存储格式一致
      tags: formData.tagArray.join(","),
    };
    delete submitData.tagArray;

    // 有 id 就是编辑，走更新接口；没有 id 就是新增
    if (isEdit.value) {
      await updateArticle(submitData);
      ElMessage.success("修改成功");
    } else {
      await createArticle(submitData);
      ElMessage.success("新增成功");
    }

    // 通知父组件刷新列表，然后关闭弹窗
    emit("success");
    dialogVisible.value = false;
  } catch (e) {
    ElMessage.error("保存失败，请重试");
  } finally {
    // 不管成功失败都要把按钮的 loading 状态去掉，否则按钮会一直转圈
    loading.value = false;
  }
};
</script>

<template>
  <el-dialog
    :title="isEdit ? '编辑文章' : '新增文章'"
    v-model="dialogVisible"
    width="50%"
    @close="handleClose"
  >
    <el-form :model="formData" :rules="rules" ref="formRef" label-width="120px">
      <el-form-item label="文章标题" prop="title">
        <el-input
          v-model="formData.title"
          placeholder="请输入文章标题"
          maxlength="200"
          show-word-limit
          clearable
        ></el-input>
      </el-form-item>
      <el-form-item label="所属分类" prop="categoryId">
        <el-select v-model="formData.categoryId" placeholder="请选择分类">
          <el-option
            v-for="item in props.categories"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          ></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="文章摘要" prop="summary">
        <el-input
          type="textarea"
          v-model="formData.summary"
          placeholder="请输入文章摘要"
          maxlength="1000"
          show-word-limit
          :rows="4"
        ></el-input>
      </el-form-item>
      <el-form-item label="文章标签" prop="content">
        <el-select
          v-model="formData.tagArray"
          placeholder="请输入文章标签"
          multiple
          filterable
          allow-create
          style="width: 100%"
        >
          <el-option
            v-for="tag in commonTags"
            :key="tag"
            :label="tag"
            :value="tag"
          ></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="封面图片">
        <div class="cover-upload">
          <el-upload
            class="cover-upload"
            action="#"
            :before-upload="beforeUpload"
            :http-request="handleUploadRequest"
            :show-file-list="false"
            accept="image/*"
          >
            <div v-if="!imageUrl" class="cover-placeholder">
              <p>点击上传封面图片</p>
            </div>
            <img v-else :src="imageUrl" alt="封面图片" class="cover-image" />
          </el-upload>
          <div v-if="imageUrl" class="cover-remove">
            <el-button type="danger" size="mini" @click="removeCover"
              >移除封面</el-button
            >
          </div>
        </div>
      </el-form-item>
      <el-form-item label="文章内容" prop="content">
        <RichTextEditor
          v-model="formData.content"
          placeholder="请输入文章内容，支持富文本格式\n\n可以使用加粗、斜体、列表、标题等格式来丰富文章内容。"
          :maxCharCount="5000"
          @change="handleContentChange"
          @created="handleEditorCreated"
          min-height="400px"
        />
      </el-form-item>
    </el-form>
    <div v-if="btnPreview">
      <h3>文章预览</h3>
      <div v-html="formData.content"></div>
    </div>
    <template #footer>
      <el-button @click="btnPreview = !btnPreview">{{
        btnPreview ? "隐藏预览" : "预览效果"
      }}</el-button>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="loading">
        {{ isEdit ? "保存修改" : "创建文章" }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.cover-placeholder {
  width: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 120px;
  font-size: 14px;
  flex-direction: column;
  color: #8b949e;
  background-color: #f6f8fa;
}

// 封面图片的显示尺寸，要和上面占位框保持一致
// object-fit: cover 表示按比例裁剪填满，不会把图片拉变形
.cover-image {
  width: 200px;
  height: 120px;
  object-fit: cover;
  display: block;
}
</style>
