package com.example.canteensimulation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.canteensimulation.entity.CanteenSeat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SeatMapper extends BaseMapper<CanteenSeat> {
}