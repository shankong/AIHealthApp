// 图片等静态资源的基地址。
// 原来指向远程 http://159.75.169.224:1235，现在换成本地后端。
//
// 注意这里【不带】/api：这个地址是用来拼后端返回的 filePath 的，
// filePath 形如 "/upload/xxx.png"，拼出来是 http://localhost:8080/upload/xxx.png。
// 所以后端提供文件访问时，路径要落在 /upload/** 上，而不是 /api/upload/**。
export const fileBaseUrl = "http://localhost:8080";
