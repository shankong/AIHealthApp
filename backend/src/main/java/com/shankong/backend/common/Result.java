package com.shankong.backend.common;

import lombok.Data;

@Data 
public class Result <T> {
    private String code;
    private String msg;
    private T data;

    public static <T> Result <T> success() {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());
        return result;
    } 

    public static <T> Result <T> success(T data) {
        Result<T> result = new Result<>();
        // 这两行不能省：前端 requst.js 里判断的是 String(data.code) === '200'，
        // 只 setData 不 setCode 的话前端拿到 code=null，会把成功当失败，
        // 弹一句"请求失败"然后把 Promise reject 掉。
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());
        result.setData(data);
        return result;
    }

    public static <T> Result <T> error() {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.ERROR.getCode());
        result.setMsg(ResultCode.ERROR.getMsg());
        return result;
    } 

    public static <T> Result <T> error(String code, String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }
}
