package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.result.PageResult;
import com.crm.entity.OperLog;
import com.crm.mapper.OperLogMapper;
import com.crm.query.OperLogQuery;
import com.crm.service.OperLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crm.utils.AddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 操作日志记录 服务实现类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@Service
@Slf4j
public class OperLogServiceImpl extends ServiceImpl<OperLogMapper, OperLog> implements OperLogService {

    @Override
    public void recordOperLog(OperLog operLog) {
            operLog.setOperLocation(AddressUtils.getRealAddressByIP(operLog.getOperIp()));
            operLog.setOperTime(LocalDateTime.now());
            this.save(operLog);
    }

    @Override
    public PageResult<OperLog> getPage(OperLogQuery operLogQuery) {
        Page<OperLog> page = new Page<>(operLogQuery.getPage(), operLogQuery.getLimit());
        LambdaQueryWrapper<OperLog> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(StringUtils.isNotBlank(operLogQuery.getOperName()), OperLog::getOperName, operLogQuery.getOperName());
        wrapper.eq(StringUtils.isNotBlank(operLogQuery.getOperUrl()), OperLog::getOperUrl, operLogQuery.getOperUrl());

        if (operLogQuery.getOperTime() != null && !operLogQuery.getOperTime().isEmpty()){
            wrapper.between(OperLog::getOperTime,operLogQuery.getOperTime().get(0),operLogQuery.getOperTime().get(1));
        }

        wrapper.orderByDesc(OperLog::getOperTime);
        Page<OperLog> pageData = baseMapper.selectPage( page,wrapper);
        return new PageResult<>(pageData.getRecords(),pageData.getTotal());
    }
}
