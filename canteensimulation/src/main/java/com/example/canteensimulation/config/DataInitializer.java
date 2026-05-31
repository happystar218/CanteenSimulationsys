package com.example.canteensimulation.config;

import com.example.canteensimulation.entity.CanteenSeat;
import com.example.canteensimulation.entity.CanteenWindow;
import com.example.canteensimulation.mapper.SeatMapper;
import com.example.canteensimulation.mapper.WindowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化器：确保座位和窗口数据与北交大四食堂实际布局一致。
 * 仅在数据缺失时自动填充，已有数据则跳过。
 *
 * 调研数据：
 * 一楼：70张4人桌 = 280个座位 (14行×20列)
 * 二楼：58张4人桌 + 1张2人桌 = 234个座位 (12行×20列-6)
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private SeatMapper seatMapper;

    @Autowired
    private WindowMapper windowMapper;

    @Override
    public void run(String... args) {
        initSeats();
        initWindows();
    }

    private void initSeats() {
        Long seatCount = seatMapper.selectCount(null);
        if (seatCount >= 500) return; // 已有完整数据（514座）

        seatMapper.delete(null);

        // 一楼 280座 (rows 0-13, cols 0-19)
        for (int row = 0; row < 14; row++) {
            for (int col = 0; col < 20; col++) {
                insertSeat(row, col, 1);
            }
        }

        // 二楼 234座 (rows 0-11, cols 0-19, 去掉 row=11 col=14~19)
        for (int row = 0; row < 12; row++) {
            for (int col = 0; col < 20; col++) {
                if (row == 11 && col >= 14) continue;
                insertSeat(row, col, 2);
            }
        }

        System.out.println("座位数据初始化完成：一楼280座 + 二楼234座 = " +
                seatMapper.selectCount(null) + "座");
    }

    private void insertSeat(int row, int col, int floor) {
        CanteenSeat seat = new CanteenSeat();
        seat.setRowIndex(row);
        seat.setColIndex(col);
        seat.setIsOccupied(0);
        seat.setFloor(floor);
        seatMapper.insert(seat);
    }

    private void initWindows() {
        Long windowCount = windowMapper.selectCount(null);
        if (windowCount >= 16) return;

        windowMapper.delete(null);

        // 一楼 8 个窗口 (全部 attraction=5)
        insertWindow("面食窗口", 30, 1, 1, 5);
        insertWindow("特色菜窗口", 20, 1, 1, 5);
        insertWindow("家常菜窗口1", 25, 1, 1, 5);
        insertWindow("家常菜窗口2", 25, 1, 1, 5);
        insertWindow("减脂餐窗口", 30, 1, 1, 5);
        insertWindow("小份菜窗口1", 25, 1, 1, 5);
        insertWindow("小份菜窗口2", 25, 1, 1, 5);
        insertWindow("特色主食窗口", 20, 1, 1, 5);

        // 二楼 8 个窗口
        insertWindow("炒鸡窗口", 0, 0, 2, 5);
        insertWindow("广式风味窗口", 45, 1, 2, 6);
        insertWindow("秘制私房菜窗口", 40, 1, 2, 5);
        insertWindow("热卤拼饭窗口", 40, 1, 2, 5);
        insertWindow("粉面窗口", 180, 1, 2, 7);
        insertWindow("麻辣烫/香锅窗口", 240, 1, 2, 8);
        insertWindow("馄饨/瓦罐汤窗口", 180, 1, 2, 6);
        insertWindow("兰州拉面窗口", 80, 1, 2, 9);

        System.out.println("窗口数据初始化完成：共 " +
                windowMapper.selectCount(null) + " 个窗口");
    }

    private void insertWindow(String name, int avgSpeed, int status, int floor, int attraction) {
        CanteenWindow win = new CanteenWindow();
        win.setName(name);
        win.setAvgSpeed(avgSpeed);
        win.setCurrentQueueCount(0);
        win.setStatus(status);
        win.setFloor(floor);
        win.setAttraction(attraction);
        windowMapper.insert(win);
    }
}
