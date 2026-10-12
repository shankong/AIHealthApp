<script setup>
// ==========================================================
// 富文本编辑器组件（基于 wangEditor 5）
//
// 用法：
//   <RichTextEditor v-model="formData.content" placeholder="请输入内容" />
//
// v-model 绑定的是一个 HTML 字符串，比如 <p>你好</p>
// ==========================================================

// wangEditor 自己的样式，必须引入，否则工具栏和编辑区全是乱的
import "@wangeditor/editor/dist/css/style.css";
import { onBeforeUnmount, ref, shallowRef, watch } from "vue";
// Editor 是编辑区，Toolbar 是工具栏
import { Editor, Toolbar } from "@wangeditor/editor-for-vue";
import { ElMessage } from "element-plus";
import { uploadFile } from "@/api/admin";
import { fileBaseUrl } from "@/config/index";

// ---------- 默认工具栏菜单 ----------
// 下面是 wangEditor 内置菜单的 key，是约定好的字符串。
// 写错了不会报错，只会导致那个按钮不显示，所以别凭记忆改。
// 特别注意：无序列表是 bulletedList、有序列表是 numberedList，不是 bullets
const DEFAULT_TOOLBAR_KEYS = [
  "headerSelect", // 标题级别下拉
  "blockquote", // 引用
  "|", // 一根竖线，表示分隔符
  "bold", // 粗体
  "underline", // 下划线
  "italic", // 斜体
  "through", // 删除线
  "code", // 行内代码
  "clearStyle", // 清除格式
  "|",
  "color", // 文字颜色
  "bgColor", // 文字背景色
  "fontSize", // 字号
  "lineHeight", // 行高
  "|",
  "bulletedList", // 无序列表
  "numberedList", // 有序列表
  "todo", // 待办清单
  "|",
  "insertLink", // 插入链接
  "uploadImage", // 上传本地图片
  "insertTable", // 插入表格
  "codeBlock", // 代码块
  "|",
  "undo", // 撤销
  "redo", // 重做
  "fullScreen", // 全屏
];

// ---------- 组件参数 ----------
const props = defineProps({
  // 编辑器内容，HTML 字符串，配合 v-model 使用
  modelValue: {
    type: String,
    default: "",
  },
  // 内容为空时显示的灰色提示文字
  placeholder: {
    type: String,
    default: "请输入内容...",
  },
  // 最大字符数，填 0 表示不限制
  maxCharCount: {
    type: Number,
    default: 0,
  },
  // 编辑区最小高度，避免内容少的时候框被压得很扁
  minHeight: {
    type: String,
    default: "300px",
  },
  // 编辑器模式：default（完整模式）或 simple（简洁模式）
  mode: {
    type: String,
    default: "default",
  },
  // 工具栏按钮。父组件想定制就传，不传（空数组）就用上面那套默认的
  //
  // 注意这里不能写成 default: () => DEFAULT_TOOLBAR_KEYS。
  // defineProps 会被编译器提升到模块作用域执行，那时本地变量还没初始化，
  // 编译会直接报错：defineProps() cannot reference locally declared variables
  toolbarKeys: {
    type: Array,
    default: () => [],
  },
});

// ---------- 对外抛出的事件 ----------
const emit = defineEmits([
  "update:modelValue", // v-model 专用
  "change", // 内容发生变化
  "created", // 编辑器创建完成，会把 editor 实例传出去
]);

// ---------- 编辑器实例 ----------
// 官方要求必须用 shallowRef，不能用 ref。
// 原因：editor 对象内部结构非常深，用 ref 会让 Vue 递归地把它变成响应式，
// 既拖慢性能，又可能因为内部对象被代理而引发一堆奇怪的问题。
const editorRef = shallowRef();

// 编辑器当前内容，交给下面的 <Editor v-model> 维护
const valueHtml = ref(props.modelValue || "");

// 工具栏配置
const toolbarConfig = {
  // 父组件传了就用它的，没传就用组件内置的默认菜单
  toolbarKeys: props.toolbarKeys.length > 0 ? props.toolbarKeys : DEFAULT_TOOLBAR_KEYS,
};

// ---------- 图片上传 ----------
// wangEditor 默认会把图片传到它自己的演示服务器，我们用不了，
// 所以改成走项目已有的 /file/upload 接口。
//
// customUpload 的参数：
//   file     用户选中的文件
//   insertFn 回调，插入图片时调用：insertFn(图片地址, alt文字, 跳转链接)
const customUploadImage = async (file, insertFn) => {
  try {
    // 后端需要一个 businessId，生成 UUID 作为本次上传的业务标识
    const businessId = crypto.randomUUID();
    // 正文插图要标成 content，否则会被当成封面
    const res = await uploadFile(file, { businessId, businessField: "content" });

    // 防御一下：万一后端没返回 filePath，早点报错，
    // 否则会拼出 http://xxx/undefined 这种地址，图片静默不显示
    if (!res?.filePath) {
      ElMessage.error("图片上传成功，但没拿到图片地址");
      return;
    }

    // fileBaseUrl 结尾没有斜杠，这里保证拼接处有且只有一个斜杠
    const filePath = res.filePath.startsWith("/")
      ? res.filePath
      : `/${res.filePath}`;

    insertFn(`${fileBaseUrl}${filePath}`, file.name, "");
  } catch (e) {
    // 不加 try/catch 的话，上传失败界面上没有任何提示，只能靠猜
    ElMessage.error("图片上传失败，请重试");
  }
};

// ---------- 编辑器配置 ----------
const editorConfig = {
  placeholder: props.placeholder,
  // 只有大于 0 时才限制字数，0 表示不限
  maxLength: props.maxCharCount > 0 ? props.maxCharCount : undefined,
  // 到达字数上限时提示一下。
  // 不加这个提示的话，用户会发现"打了字但没显示"，很困惑
  onMaxLength: () => {
    if (props.maxCharCount > 0) {
      ElMessage.warning(`内容不能超过 ${props.maxCharCount} 个字`);
    }
  },
  MENU_CONF: {
    uploadImage: {
      customUpload: customUploadImage,
    },
  },
};

// ---------- 回调 ----------
// 编辑器创建完成后触发。
// 必须把实例存进 editorRef，否则上面 <Toolbar :editor="editorRef"> 拿不到实例，
// 工具栏会是一片空白
const handleCreated = (editor) => {
  editorRef.value = editor;
  emit("created", editor);
};

// 内容变化时同步给父组件
const handleChange = (editor) => {
  const html = editor.getHtml();
  // 内容没变就不往外抛，否则父组件收到后又传回来，来回触发会形成死循环
  if (html === props.modelValue) return;
  emit("update:modelValue", html);
  emit("change", html);
};

// 父组件从外部改内容时（比如打开编辑弹窗回填文章），同步进编辑器
watch(
  () => props.modelValue,
  (val) => {
    if (val === valueHtml.value) return;
    valueHtml.value = val || "";
  }
);

// 组件销毁时必须手动销毁编辑器。
// 不销毁的话，它注册的 DOM 事件和定时器不会释放，属于内存泄漏
onBeforeUnmount(() => {
  const editor = editorRef.value;
  if (editor == null) return;
  editor.destroy();
});
</script>

<template>
  <!-- minHeight 通过 CSS 变量传给样式，下面样式中再取用 -->
  <div class="rich-text-editor" :style="{ '--editor-min-height': minHeight }">
    <Toolbar
      class="editor-toolbar"
      :editor="editorRef"
      :defaultConfig="toolbarConfig"
      :mode="mode"
    />
    <Editor
      class="editor-content"
      v-model="valueHtml"
      :defaultConfig="editorConfig"
      :mode="mode"
      @onCreated="handleCreated"
      @onChange="handleChange"
    />
  </div>
</template>

<style scoped lang="scss">
.rich-text-editor {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;

  .editor-toolbar {
    border-bottom: 1px solid #dcdfe6;
  }

  // 编辑区的最小高度，值来自组件的 minHeight 属性
  :deep(.w-e-text-container) {
    min-height: var(--editor-min-height);
  }

  // wangEditor 的下拉弹层（颜色选择器、字号选择器等）默认层级较低，
  // 组件放在 el-dialog 里时会被遮住，这里把层级抬高
  :deep(.w-e-drop-panel),
  :deep(.w-e-modal) {
    z-index: 3000;
  }
}
</style>
