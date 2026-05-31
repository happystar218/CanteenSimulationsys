package com.example.canteensimulation.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_seat")
public class CanteenSeat {
    @TableId
    private Integer id;
    private Integer rowIndex;
    private Integer colIndex;
    private Integer isOccupied; // 0-空闲, 1-占用
    private Integer floor;
}