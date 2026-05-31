package com.example.canteensimulation.controller;

import com.example.canteensimulation.common.Result;
import com.example.canteensimulation.entity.SimulationRecord;
import com.example.canteensimulation.mapper.SimulationRecordMapper;
import com.example.canteensimulation.service.SimulationEngine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/simulation")
public class SimulationRecordController {

    @Autowired
    private SimulationEngine simulationEngine;

    @Autowired
    private SimulationRecordMapper recordMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 保存当前仿真结果
     */
    @PostMapping("/save")
    public Result<SimulationRecord> saveResult() {
        Map<String, Object> results = simulationEngine.getResults();

        SimulationRecord record = new SimulationRecord();
        try {
            record.setConfigJson(objectMapper.writeValueAsString(results));
        } catch (JsonProcessingException e) {
            record.setConfigJson("{}");
        }
        record.setAvgWaitTime(((Number) results.get("avgWaitTime")).intValue());
        record.setTotalServed((Long) results.get("totalServed"));
        record.setEntryCount((Long) results.get("entryCount"));
        record.setSeatUtilization((Double) results.get("seatUtilization"));
        record.setCongestionLevel((String) results.get("congestionLevel"));
        record.setElapsedSeconds(((Number) results.get("elapsedSeconds")).intValue());
        record.setMaxSeatUtilization((Double) results.get("maxSeatUtilization"));
        record.setPeriodStart((String) results.get("periodStart"));
        record.setPeriodDuration((Integer) results.get("simulationDuration"));
        // 每窗口数据序列化为JSON
        try {
            record.setPerWindowData(objectMapper.writeValueAsString(results.get("perWindowData")));
        } catch (JsonProcessingException e) {
            record.setPerWindowData("[]");
        }
        record.setCreatedAt(LocalDateTime.now());

        recordMapper.insert(record);
        return Result.success(record);
    }

    /**
     * 查询所有历史记录（按时间倒序）
     */
    @GetMapping("/records")
    public Result<List<SimulationRecord>> getRecords() {
        List<SimulationRecord> records = recordMapper.selectList(null);
        records.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return Result.success(records);
    }

    /**
     * 删除指定记录
     */
    @DeleteMapping("/records/{id}")
    public Result<String> deleteRecord(@PathVariable Long id) {
        int affected = recordMapper.deleteById(id);
        return affected > 0 ? Result.success("删除成功") : Result.error("记录不存在");
    }
}
