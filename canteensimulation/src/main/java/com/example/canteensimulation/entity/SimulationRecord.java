package com.example.canteensimulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_simulation_record")
public class SimulationRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String configJson;
    private Integer avgWaitTime;
    private Long totalServed;
    private Long entryCount;
    private Double seatUtilization;
    private String congestionLevel;
    private Integer elapsedSeconds;
    private String perWindowData;        // JSON: 每窗口统计 [{windowName, avgQueue, maxQueue, avgWait, maxWait}]
    private Double maxSeatUtilization;   // 最高座位利用率(%)
    private String periodStart;          // 仿真时段起始
    private Integer periodDuration;      // 仿真时段时长(秒)
    private LocalDateTime createdAt;
}
