package com.example.canteensimulation.controller;

import com.example.canteensimulation.common.Result;
import com.example.canteensimulation.dto.SimConfigDTO;
import com.example.canteensimulation.entity.CanteenWindow;
import com.example.canteensimulation.service.IWindowService;
import com.example.canteensimulation.service.SimulationEngine;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/window")
@CrossOrigin // 处理跨域问题，允许前端 5173 端口访问后端 8080 端口
public class WindowController {

    @Autowired
    private IWindowService windowService;

    @Autowired
    private SimulationEngine simulationEngine;

    /**
     * 查询所有档口信息
     * 用于前端表格展示及实时数据刷新
     */
    @GetMapping("/list")
    public Result<List<CanteenWindow>> getWindowList() {
        List<CanteenWindow> list = windowService.list();
        return Result.success(list);
    }

    /**
     * 新增档口
     * Bug 15 修复: 添加 @Valid 校验请求体，防止非法数据入库
     */
    @PostMapping("/add")
    public Result<String> addWindow(@Valid @RequestBody CanteenWindow canteenWindow) {
        boolean saved = windowService.save(canteenWindow);
        return saved ? Result.success("添加成功") : Result.error("添加失败");
    }

    /**
     * Step 5: 修改档口信息
     */
    @PutMapping("/update/{id}")
    public Result<String> updateWindow(@PathVariable Integer id, @Valid @RequestBody CanteenWindow canteenWindow) {
        canteenWindow.setId(id);
        boolean updated = windowService.updateById(canteenWindow);
        return updated ? Result.success("修改成功") : Result.error("修改失败");
    }

    /**
     * Step 5: 删除档口
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteWindow(@PathVariable Integer id) {
        boolean removed = windowService.removeById(id);
        return removed ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 启动或更新仿真配置
     * @param config 包含到达强度、分布类型、楼层偏好的配置对象
     */
    @PostMapping("/simulation/start")
    public Result<String> startSim(@Valid @RequestBody SimConfigDTO config) {
        simulationEngine.start(config);
        return Result.success("仿真引擎已启动");
    }

    /**
     * 停止仿真
     */
    @PostMapping("/simulation/stop")
    public Result<String> stopSim() {
        simulationEngine.stop();
        return Result.success("仿真引擎已停止");
    }

    /**
     * 暂停仿真（保留当前状态）
     */
    @PostMapping("/simulation/pause")
    public Result<String> pauseSim() {
        simulationEngine.pause();
        return Result.success("仿真已暂停");
    }

    /**
     * 继续仿真
     */
    @PostMapping("/simulation/resume")
    public Result<String> resumeSim() {
        simulationEngine.resume();
        return Result.success("仿真已继续");
    }

    /**
     * 获取仿真运行状态
     */
    @GetMapping("/simulation/status")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("running", simulationEngine.isRunning());
        status.put("paused", simulationEngine.isPaused());
        status.put("tickCount", simulationEngine.getTickCount());
        status.put("duration", simulationEngine.getSimulationDuration());
        status.put("periodStart", simulationEngine.getPeriodStart());
        return Result.success(status);
    }

    /**
     * 重置仿真统计数据（保留队列和座位状态）
     */
    @PostMapping("/simulation/reset")
    public Result<String> resetSim() {
        simulationEngine.reset();
        return Result.success("统计数据已重置");
    }

    /**
     * Bug 7 修复: 获取仿真统计结果（平均等待时间、总服务人数）
     * 前端可调用此接口展示仿真运行数据
     */
    @GetMapping("/simulation/results")
    public Result<Map<String, Object>> getResults() {
        return Result.success(simulationEngine.getResults());
    }
}