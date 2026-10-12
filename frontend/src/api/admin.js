import service from "@/utils/requst.js";

export function login(data) {
  return service.post("/user/login", data);
}

export function categoryTree() {
  return service.get("/knowledge/category/tree");
}

export function articlePage(paras) {
  return service.get("/knowledge/article/page", { params: paras });
}

export function uploadFile(file, businessInfo) {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("businessType", "ARTICLE");
  formData.append("businessId", businessInfo.businessId);
  // businessField 交给调用方决定：封面传 cover，正文插图传 content。
  // 原来写死成 cover，富文本正文里插的图也会被登记成封面
  formData.append("businessField", businessInfo.businessField || "cover");
  return service.post("/file/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}

export function createArticle(data) {
  return service.post("/knowledge/article", data);
}

// 更新文章。id 放在请求体里一起提交
// 如果后端用的是 PUT /knowledge/article/{id} 这种带路径参数的写法，
// 把这里改成 service.put(`/knowledge/article/${data.id}`, data) 即可
export function updateArticle(data) {
  return service.put(`/knowledge/article/${data.id}`, data);
}

export function getArticleDetail(id) {
  return service.get(`/knowledge/article/${id}`);
}

export function changeArticleStatus(id, data) {
    return service.put(`/knowledge/article/${id}/status`, data);
}