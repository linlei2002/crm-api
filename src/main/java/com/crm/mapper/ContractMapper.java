package com.crm.mapper;

import com.crm.entity.Contract;
import com.crm.query.ContractTrendQuery;
import com.crm.vo.ContractTrendVO;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
public interface ContractMapper extends MPJBaseMapper<Contract> {

    List<ContractTrendVO> getTotalContractStatistics();

    /**
     * 获取合同统计数据
     * @param query 通过query获取时间范围，作为sql查询条件
     * @return List<ContractTrendVO>
     */
    List<ContractTrendVO> getTradeStatisticsByDay(@Param("query") ContractTrendQuery query);


}
