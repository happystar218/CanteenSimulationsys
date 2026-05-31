package com.example.canteensimulation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.canteensimulation.entity.CanteenWindow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WindowMapper extends BaseMapper<CanteenWindow> {
}