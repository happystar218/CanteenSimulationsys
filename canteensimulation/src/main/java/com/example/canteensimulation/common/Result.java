package com.example.canteensimulation.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;    // 状态码：200-成功，500-失败
    private String message;  // 提示信息
    private T data;          // 具体数据

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        result.setData(null);  // Bug 20 修复: 显式设置 data 为 null，与 success() 保持一致
        return result;
    }
}