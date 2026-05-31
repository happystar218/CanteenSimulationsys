package com.example.canteensimulation.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_window")
public class CanteenWindow {
    @TableId
    private Integer id;

    @NotBlank(message = "窗口名称不能为空")
    private String name;

    // Bug 21 修复: 修正命名和注释 -- avgServiceTimeSeconds 表示"平均服务时间(秒)"
    // 逻辑上是 1/N 概率每秒完成服务，因此值越大 = 服务越慢
    @NotNull(message = "平均服务时间不能为空")
    @Min(value = 1, message = "平均服务时间必须大于0")
    private Integer avgSpeed; // 平均服务时间(秒)，值越大出餐越慢

    private Integer currentQueueCount;

    @Min(value = 0, message = "状态只能为0(关闭)或1(营业)")
    @Max(value = 1, message = "状态只能为0(关闭)或1(营业)")
    private Integer status; // 1-营业, 0-关闭

    @Min(value = 1, message = "楼层只能为1或2")
    @Max(value = 2, message = "楼层只能为1或2")
    private Integer floor;  // 1-一楼, 2-二楼

    @Min(value = 1, message = "吸引力范围为1-10")
    @Max(value = 10, message = "吸引力范围为1-10")
    private Integer attraction; // 窗口吸引力(1-10)，越高学生越愿意排队

    private LocalDateTime lastServeTime; // 最近一次出餐时间
}