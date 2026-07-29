import service from "@/utils/requst.js"

export function login(data) {
    return service.post('/user/login', data)
}
