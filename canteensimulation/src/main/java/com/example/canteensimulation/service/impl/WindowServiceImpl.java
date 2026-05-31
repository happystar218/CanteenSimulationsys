package com.example.canteensimulation.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.canteensimulation.entity.CanteenWindow;
import com.example.canteensimulation.mapper.WindowMapper;
import com.example.canteensimulation.service.IWindowService;
import org.springframework.stereotype.Service;

@Service
public class WindowServiceImpl extends ServiceImpl<WindowMapper, CanteenWindow> implements IWindowService {
    // 继承 ServiceImpl 后，你就拥有了 save, remove, update, get 等所有内置方法
}