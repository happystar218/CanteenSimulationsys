package com.example.canteensimulation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SimConfigDTO {
    @NotNull(message = "到达率不能为空")
    @Min(value = 1, message = "到达率至少为1人/分钟")
    @Max(value = 500, message = "到达率最多为500人/分钟")
    private Integer arrivalRate;       // 每分钟到达人数

    // Bug 17: distributionType 待实现 -- 当前仿真引擎始终使用均匀分布
    // 后续可在此实现 normal(正态分布) 逻辑（使用 Random.nextGaussian()）
    private String distributionType;   // uniform 或 normal

    @NotNull(message = "楼层偏好不能为空")
    @Min(value = 0, message = "楼层偏好范围为0-100")
    @Max(value = 100, message = "楼层偏好范围为0-100")
    private Integer floorPreference;   // 留在一楼的概率 (0-100)

    @Min(value = 1, message = "速度倍率最小为1")
    @Max(value = 120, message = "速度倍率最大为120")
    private Integer speedMultiplier = 1; // 仿真速度倍率(1x-120x)

    @NotNull(message = "仿真时长不能为空")
    @Min(value = 60, message = "仿真时长至少60秒")
    private Integer simulationDuration = 7200; // 仿真总时长(秒)，默认2小时

    private String periodStart = "07:00"; // 时段起始时间，如 "07:00", "11:00", "17:00"
}