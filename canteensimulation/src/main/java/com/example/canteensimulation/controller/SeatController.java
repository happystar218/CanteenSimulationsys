package com.example.canteensimulation.controller;

import com.example.canteensimulation.common.Result;
import com.example.canteensimulation.entity.CanteenSeat;
import com.example.canteensimulation.mapper.SeatMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seat")
@CrossOrigin
public class SeatController {

    @Autowired
    private SeatMapper seatMapper;

    @GetMapping("/list")
    public Result<List<CanteenSeat>> getSeats(@RequestParam Integer floor) {
        QueryWrapper<CanteenSeat> query = new QueryWrapper<>();
        query.eq("floor", floor);
        return Result.success(seatMapper.selectList(query));
    }
}