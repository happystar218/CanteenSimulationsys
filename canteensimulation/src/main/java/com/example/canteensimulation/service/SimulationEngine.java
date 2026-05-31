package com.example.canteensimulation.service;

import com.example.canteensimulation.dto.SimConfigDTO;
import com.example.canteensimulation.entity.CanteenSeat;
import com.example.canteensimulation.entity.CanteenWindow;
import com.example.canteensimulation.entity.SimulationRecord;
import com.example.canteensimulation.mapper.SeatMapper;
import com.example.canteensimulation.mapper.SimulationRecordMapper;
import com.example.canteensimulation.mapper.WindowMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Service
public class SimulationEngine {

    @Autowired
    private WindowMapper windowMapper;
    @Autowired
    private SeatMapper seatMapper;
    @Autowired
    private SimulationRecordMapper recordMapper;

    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;
    private volatile SimConfigDTO currentConfig;
    private final Random random = new Random();

    // === 基础统计 ===
    private long totalServed = 0;
    private long totalWaitTimeSeconds = 0;
    private long tickCount = 0;
    private volatile int simulationDuration = 7200;
    private volatile String periodStart = "07:00";

    // === 追踪指标 ===
    private long entryCount = 0;
    private int currentMinuteEntryCount = 0;
    private long currentMinute = -1;
    private final Map<String, List<Long>> flowHistory = new LinkedHashMap<>();
    private int currentOccupiedSeats = 0;
    private int maxOccupiedSeats = 0;
    private long seatOccupationSum = 0;
    private long seatSampleCount = 0;
    private long totalSeatCount = -1;
    private long simulationStartTime = 0;

    // === 每窗口指标 ===
    private final Map<Integer, Long> windowQueueSum = new HashMap<>();
    private final Map<Integer, Long> windowQueueSamples = new HashMap<>();
    private final Map<Integer, Integer> windowMaxQueue = new HashMap<>();

    private static class EatingStudent {
        int seatId;
        long finishTime;
    }
    private final Queue<EatingStudent> eatingStudents = new ConcurrentLinkedQueue<>();

    // ============ 公开控制 ============

    public void start(SimConfigDTO config) {
        this.currentConfig = config;
        resetCounters();
        this.tickCount = 0;
        this.simulationDuration = config.getSimulationDuration() != null ? config.getSimulationDuration() : 7200;
        this.periodStart = config.getPeriodStart() != null ? config.getPeriodStart() : "07:00";
        this.isPaused = false;
        this.isRunning = true;
        this.simulationStartTime = System.currentTimeMillis() / 1000;
    }

    public void stop() {
        this.isRunning = false;
        this.isPaused = false;

        // 自动保存仿真结果到数据库
        saveCurrentResults();

        List<CanteenWindow> windows = windowMapper.selectList(null);
        for (CanteenWindow w : windows) {
            w.setCurrentQueueCount(0);
            windowMapper.updateById(w);
        }
        List<CanteenSeat> seats = seatMapper.selectList(null);
        for (CanteenSeat s : seats) {
            s.setIsOccupied(0);
            seatMapper.updateById(s);
        }
        eatingStudents.clear();
        this.currentOccupiedSeats = 0;
    }

    /**
     * 自动保存当前仿真结果到数据库
     */
    private void saveCurrentResults() {
        try {
            Map<String, Object> results = getResults();
            ObjectMapper om = new ObjectMapper();

            SimulationRecord record = new SimulationRecord();
            record.setConfigJson(om.writeValueAsString(results));
            record.setAvgWaitTime(((Number) results.get("avgWaitTime")).intValue());
            record.setTotalServed((Long) results.get("totalServed"));
            record.setEntryCount((Long) results.get("entryCount"));
            record.setSeatUtilization((Double) results.get("seatUtilization"));
            record.setCongestionLevel((String) results.get("congestionLevel"));
            record.setElapsedSeconds(((Number) results.get("elapsedSeconds")).intValue());
            record.setMaxSeatUtilization((Double) results.get("maxSeatUtilization"));
            record.setPeriodStart((String) results.get("periodStart"));
            record.setPeriodDuration((Integer) results.get("simulationDuration"));
            record.setPerWindowData(om.writeValueAsString(results.get("perWindowData")));
            record.setCreatedAt(LocalDateTime.now());

            recordMapper.insert(record);
            System.out.println("仿真结果已自动保存，ID=" + record.getId());
        } catch (Exception e) {
            System.err.println("自动保存仿真结果失败: " + e.getMessage());
        }
    }

    public void pause() { this.isPaused = true; }
    public void resume() { this.isPaused = false; }

    public boolean isRunning() { return isRunning; }
    public boolean isPaused() { return isPaused; }
    public long getTickCount() { return tickCount; }
    public int getSimulationDuration() { return simulationDuration; }
    public String getPeriodStart() { return periodStart; }

    public void reset() {
        resetCounters();
        this.tickCount = 0;
    }

    private void resetCounters() {
        this.totalServed = 0;
        this.totalWaitTimeSeconds = 0;
        this.entryCount = 0;
        this.currentMinuteEntryCount = 0;
        this.currentMinute = -1;
        this.flowHistory.clear();
        this.currentOccupiedSeats = 0;
        this.maxOccupiedSeats = 0;
        this.seatOccupationSum = 0;
        this.seatSampleCount = 0;
        this.windowQueueSum.clear();
        this.windowQueueSamples.clear();
        this.windowMaxQueue.clear();
        if (totalSeatCount < 0) {
            totalSeatCount = seatMapper.selectCount(null);
        }
    }

    // ============ 核心仿真 ============

    @Transactional
    @Scheduled(fixedRate = 1000)
    public void runStep() {
        if (!isRunning || currentConfig == null || isPaused) return;

        // 自动结束
        if (tickCount >= simulationDuration) {
            stop();
            return;
        }

        int multiplier = currentConfig.getSpeedMultiplier() != null
                ? currentConfig.getSpeedMultiplier() : 1;

        for (int tick = 0; tick < multiplier; tick++) {
            if (tickCount >= simulationDuration) break;
            doOneStep();
            tickCount++;
        }
    }

    private void doOneStep() {
        long currentTime = System.currentTimeMillis() / 1000 + tickCount;
        List<CanteenWindow> allWindows = windowMapper.selectList(null);

        int arrivalRate = currentConfig.getArrivalRate() != null ? currentConfig.getArrivalRate() : 30;
        int floorPref = currentConfig.getFloorPreference() != null ? currentConfig.getFloorPreference() : 50;
        String distType = currentConfig.getDistributionType() != null ? currentConfig.getDistributionType() : "uniform";

        // 1. 学生到达
        int studentCount = calculateArrivals(arrivalRate, distType, tickCount);
        for (int i = 0; i < studentCount; i++) {
            int targetFloor = (random.nextInt(100) < floorPref) ? 1 : 2;
            List<CanteenWindow> floorWindows = allWindows.stream()
                    .filter(w -> w.getFloor() == targetFloor && w.getStatus() == 1)
                    .collect(Collectors.toList());
            if (!floorWindows.isEmpty()) {
                CanteenWindow best = selectBestWindow(floorWindows);
                best.setCurrentQueueCount(best.getCurrentQueueCount() + 1);
            }
        }
        entryCount += studentCount;
        trackPerMinuteFlow(tickCount, studentCount);

        // 2. 窗口出餐 + 采样每窗口指标
        for (CanteenWindow w : allWindows) {
            int q = w.getCurrentQueueCount() != null ? w.getCurrentQueueCount() : 0;

            // 采样每窗口排队数据
            windowQueueSum.merge(w.getId(), (long) q, Long::sum);
            windowQueueSamples.merge(w.getId(), 1L, Long::sum);
            windowMaxQueue.merge(w.getId(), q, Math::max);

            if (w.getStatus() == 1 && q > 0) {
                Integer avgSpeed = w.getAvgSpeed();
                if (avgSpeed != null && avgSpeed > 0 && random.nextInt(avgSpeed) == 0) {
                    w.setCurrentQueueCount(q - 1);
                    totalServed++;
                    totalWaitTimeSeconds += (long) q * avgSpeed;
                    w.setLastServeTime(LocalDateTime.now());
                    findAndOccupySeat(w.getFloor(), currentTime);
                }
            }
            windowMapper.updateById(w);
        }

        // 3. 用餐结束
        while (!eatingStudents.isEmpty() && eatingStudents.peek().finishTime <= currentTime) {
            EatingStudent student = eatingStudents.poll();
            CanteenSeat seat = seatMapper.selectById(student.seatId);
            if (seat != null) {
                seat.setIsOccupied(0);
                seatMapper.updateById(seat);
                currentOccupiedSeats--;
            }
        }

        // 4. 采样
        seatOccupationSum += currentOccupiedSeats;
        seatSampleCount++;
        if (currentOccupiedSeats > maxOccupiedSeats) {
            maxOccupiedSeats = currentOccupiedSeats;
        }
    }

    // ============ 窗口选择 ============

    private CanteenWindow selectBestWindow(List<CanteenWindow> windows) {
        return windows.stream().min((a, b) -> {
            int attractA = a.getAttraction() != null ? a.getAttraction() : 5;
            int attractB = b.getAttraction() != null ? b.getAttraction() : 5;
            double scoreA = a.getCurrentQueueCount() - (attractA - 5) * 1.0;
            double scoreB = b.getCurrentQueueCount() - (attractB - 5) * 1.0;
            return Double.compare(scoreA, scoreB);
        }).orElse(windows.get(0));
    }

    // ============ 到达分布 ============

    private int calculateArrivals(int arrivalRate, String distType, long simTime) {
        double studentsPerSecond;
        switch (distType != null ? distType : "uniform") {
            case "normal": {
                double phaseSeconds = simTime % 7200;
                double gaussianFactor = Math.exp(
                        -Math.pow(phaseSeconds - 3600, 2) / (2.0 * 1800 * 1800));
                studentsPerSecond = arrivalRate * (0.05 + 0.95 * gaussianFactor) / 60.0;
                break;
            }
            case "poisson":
            default:
                studentsPerSecond = arrivalRate / 60.0;
                break;
        }
        int count = (int) studentsPerSecond;
        if (random.nextDouble() < studentsPerSecond - count) count++;
        return count;
    }

    // ============ 流量追踪 ============

    private void trackPerMinuteFlow(long simTime, int studentCount) {
        long currentMin = simTime / 60;
        if (this.currentMinute < 0) this.currentMinute = currentMin;
        if (currentMin != this.currentMinute) {
            flowHistory.put(String.valueOf(this.currentMinute),
                    Collections.singletonList((long) this.currentMinuteEntryCount));
            this.currentMinuteEntryCount = 0;
            this.currentMinute = currentMin;
            if (flowHistory.size() > 20) {
                flowHistory.remove(flowHistory.keySet().iterator().next());
            }
        }
        this.currentMinuteEntryCount += studentCount;
    }

    // ============ 座位分配 ============

    private void findAndOccupySeat(int floor, long currentTime) {
        QueryWrapper<CanteenSeat> qw = new QueryWrapper<>();
        qw.eq("floor", floor).eq("is_occupied", 0);
        List<CanteenSeat> freeSeats = seatMapper.selectList(qw);
        if (!freeSeats.isEmpty()) {
            CanteenSeat seat = freeSeats.get(random.nextInt(freeSeats.size()));
            LambdaUpdateWrapper<CanteenSeat> uw = new LambdaUpdateWrapper<>();
            uw.eq(CanteenSeat::getId, seat.getId())
              .eq(CanteenSeat::getIsOccupied, 0)
              .set(CanteenSeat::getIsOccupied, 1);
            if (seatMapper.update(null, uw) == 0) return;
            currentOccupiedSeats++;
            EatingStudent s = new EatingStudent();
            s.seatId = seat.getId();
            s.finishTime = currentTime + 900 + random.nextInt(300);
            eatingStudents.add(s);
        }
    }

    // ============ 结果查询（增强版） ============

    public Map<String, Object> getResults() {
        Map<String, Object> res = new LinkedHashMap<>();

        double seatUtilization = seatSampleCount == 0 ? 0
                : (double) seatOccupationSum / seatSampleCount / totalSeatCount * 100;
        double maxSeatUtil = totalSeatCount == 0 ? 0
                : (double) maxOccupiedSeats / totalSeatCount * 100;

        res.put("avgWaitTime", totalServed == 0 ? 0 : totalWaitTimeSeconds / totalServed);
        res.put("totalServed", totalServed);
        res.put("entryCount", entryCount);
        res.put("currentMinuteCount", currentMinuteEntryCount);
        res.put("seatUtilization", Math.round(seatUtilization * 10.0) / 10.0);
        res.put("maxSeatUtilization", Math.round(maxSeatUtil * 10.0) / 10.0);
        res.put("currentOccupiedSeats", currentOccupiedSeats);
        res.put("totalSeats", totalSeatCount);
        res.put("tickCount", tickCount);
        res.put("simulationDuration", simulationDuration);
        res.put("periodStart", periodStart);

        String congestionLevel;
        if (seatUtilization < 30) congestionLevel = "空闲";
        else if (seatUtilization < 60) congestionLevel = "适中";
        else if (seatUtilization < 85) congestionLevel = "拥挤";
        else congestionLevel = "严重拥堵";
        res.put("congestionLevel", congestionLevel);

        // 每分钟流量
        List<Map<String, Object>> flowData = new ArrayList<>();
        for (Map.Entry<String, List<Long>> e : flowHistory.entrySet()) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("minute", e.getKey()); p.put("count", e.getValue().get(0));
            flowData.add(p);
        }
        Map<String, Object> live = new LinkedHashMap<>();
        live.put("minute", "live"); live.put("count", currentMinuteEntryCount);
        flowData.add(live);
        res.put("flowHistory", flowData);

        // 每窗口指标
        List<CanteenWindow> allWindows = windowMapper.selectList(null);
        List<Map<String, Object>> perWindow = new ArrayList<>();
        for (CanteenWindow w : allWindows) {
            Map<String, Object> pw = new LinkedHashMap<>();
            long samples = windowQueueSamples.getOrDefault(w.getId(), 0L);
            long sum = windowQueueSum.getOrDefault(w.getId(), 0L);
            int maxQ = windowMaxQueue.getOrDefault(w.getId(), 0);
            int speed = w.getAvgSpeed() != null ? w.getAvgSpeed() : 30;
            pw.put("windowName", w.getName());
            pw.put("floor", w.getFloor());
            pw.put("status", w.getStatus());
            pw.put("attraction", w.getAttraction() != null ? w.getAttraction() : 5);
            pw.put("avgQueue", samples == 0 ? 0 : Math.round((double) sum / samples * 10.0) / 10.0);
            pw.put("maxQueue", maxQ);
            pw.put("avgWait", samples == 0 ? 0 : (long) ((double) sum / samples * speed));
            pw.put("maxWait", (long) maxQ * speed);
            perWindow.add(pw);
        }
        res.put("perWindowData", perWindow);

        // 分析建议
        res.put("analysis", generateAnalysis(perWindow, seatUtilization));

        long elapsed = simulationStartTime > 0
                ? System.currentTimeMillis() / 1000 - simulationStartTime : 0;
        res.put("elapsedSeconds", elapsed);

        return res;
    }

    private List<String> generateAnalysis(List<Map<String, Object>> perWindow, double seatUtil) {
        List<String> advice = new ArrayList<>();

        if (seatUtil > 85) advice.add("⚠ 座位严重拥堵（平均利用率" + Math.round(seatUtil) + "%），建议增设座位或缩短用餐时长");
        else if (seatUtil > 60) advice.add("⚡ 座位较为紧张（平均利用率" + Math.round(seatUtil) + "%），高峰期可能不够用");
        else advice.add("✅ 座位利用率正常（" + Math.round(seatUtil) + "%），承载能力充足");

        if (maxOccupiedSeats > totalSeatCount * 0.9)
            advice.add("⚠ 高峰时刻座位几乎全满（" + maxOccupiedSeats + "/" + totalSeatCount + "），存在拥堵风险");

        for (Map<String, Object> pw : perWindow) {
            String name = (String) pw.get("windowName");
            double avgQ = ((Number) pw.get("avgQueue")).doubleValue();
            int maxQ = (Integer) pw.get("maxQueue");
            int attraction = (Integer) pw.get("attraction");

            if (maxQ > 40)
                advice.add("🔴 窗口「" + name + "」最高排队达" + maxQ + "人，强烈建议增设同类窗口或提高打饭速度");
            else if (avgQ > 20)
                advice.add("🟡 窗口「" + name + "」平均排队" + String.format("%.1f", avgQ) + "人，高峰期存在瓶颈");

            if (attraction >= 8 && avgQ > 15)
                advice.add("💡 窗口「" + name + "」吸引力高(" + attraction + ")且排队较长，可考虑在该窗口附近增设分流窗口");
        }

        // 楼层分析
        double f1avg = perWindow.stream().filter(p -> (Integer) p.get("floor") == 1 && (Integer) p.get("status") == 1)
                .mapToDouble(p -> ((Number) p.get("avgQueue")).doubleValue()).average().orElse(0);
        double f2avg = perWindow.stream().filter(p -> (Integer) p.get("floor") == 2 && (Integer) p.get("status") == 1)
                .mapToDouble(p -> ((Number) p.get("avgQueue")).doubleValue()).average().orElse(0);
        if (f1avg > f2avg * 1.5)
            advice.add("📊 一楼平均排队(" + String.format("%.1f", f1avg) + "人)显著高于二楼(" + String.format("%.1f", f2avg) + "人)，建议引导分流");

        if (advice.size() <= 1)
            advice.add("✅ 食堂整体运行良好，当前参数下可满足学生用餐需求");

        return advice;
    }
}
