package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.exception.ServerException;
import com.crm.common.result.PageResult;
import com.crm.convert.CustomerConvert;
import com.crm.entity.Customer;
import com.crm.entity.SysManager;
import com.crm.mapper.CustomerMapper;
import com.crm.query.CustomerQuery;
import com.crm.query.CustomerTrendQuery;
import com.crm.query.IdQuery;
import com.crm.security.user.SecurityUser;
import com.crm.service.CustomerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crm.utils.DateUtils;
import com.crm.utils.ExcelUtils;
import com.crm.vo.CustomerTrendVO;
import com.crm.vo.CustomerVO;
import com.fhs.common.utils.StringUtil;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.query.MPJLambdaQueryWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.swing.text.DateFormatter;
import java.security.Security;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.crm.utils.DateUtils.getHourData;
import static com.crm.utils.DateUtils.getMonthInRange;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@Service
@Slf4j
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    @Override
    public PageResult<CustomerVO> getPage(CustomerQuery query) {
        Page<CustomerVO> page = new Page<>(query.getPage(), query.getLimit());
        MPJLambdaWrapper< Customer> wrapper = selection(query);
        Page<CustomerVO> result = baseMapper.selectJoinPage(page, CustomerVO.class, wrapper);
        return new PageResult<>(result.getRecords(),result.getTotal());
    }

    @Override
    public void exportCustomer(CustomerQuery query, HttpServletResponse response) {
        MPJLambdaWrapper<Customer> wrap = selection(query);
        List<CustomerVO> list = baseMapper.selectJoinList(CustomerVO.class, wrap);
        ExcelUtils.writeExcel(response,list,"客户列表","客户列表",CustomerVO.class);
    }

    @Override
    public void saveOrUpdate(CustomerVO customerVO) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>().eq(Customer::getPhone,customerVO.getPhone());
        if (customerVO.getId() == null){
            //1.判断手机号信息是否存在
            Customer customer = baseMapper.selectOne(wrapper);
            if (customer != null){
                throw new ServerException("该手机号客户已经存在,请勿重复添加客户信息");
            }

            Customer convertCustomer = CustomerConvert.INSTANCE.convert(customerVO);
            //2.获取新增的管理员信息
            Integer managerId = SecurityUser.getManagerId();
            convertCustomer.setOwnerId(managerId);
            convertCustomer.setCreaterId(managerId);
            baseMapper.insert(convertCustomer);
        }else {
            wrapper.ne(Customer::getId,customerVO.getId());
            Customer customer = baseMapper.selectOne(wrapper);
            if (customer != null){
                throw new ServerException("该手机号客户已经存在,请勿重复添加客户信息");
            }
            Customer convertCustomer = CustomerConvert.INSTANCE.convert(customerVO);
            baseMapper.updateById(convertCustomer);
        }
    }

    @Override
    public void customerToPublicPool(IdQuery idQuery) {
        Customer customer = baseMapper.selectById(idQuery.getId());
        log.info("客户信息:{}",customer);
        if (customer == null){
            throw new ServerException("客户不存在,无法转入公海");
        }
        customer.setIsPublic(1);
        customer.setOwnerId(null);
        baseMapper.updateById(customer);
    }

    @Override
    public void removeCustomer(List<Integer> ids) {
        removeByIds(ids);
    }

    @Override
    public void publicPoolToPrivate(IdQuery idQuery) {
        Customer customer = baseMapper.selectById(idQuery.getId());
        log.info("客户信息:{}",customer);
        if (customer == null){
            throw new ServerException("客户不存在,无法转回个人");
        }
        customer.setIsPublic(0);
        Integer ownerId = SecurityUser.getManagerId();
        customer.setOwnerId(ownerId);
        baseMapper.updateById(customer);
    }

    private  MPJLambdaWrapper<Customer> selection(CustomerQuery query){
        MPJLambdaWrapper<Customer> wrapper = new MPJLambdaWrapper<>();

        wrapper.selectAll(Customer.class)
                .selectAs("o", SysManager::getAccount,CustomerVO::getOwnerName)
                .selectAs("c",SysManager::getAccount,CustomerVO::getCreatorName)
                .leftJoin(SysManager.class,"o", SysManager::getId, Customer::getOwnerId)
                .leftJoin(SysManager.class,"c", SysManager::getId, Customer::getCreaterId);

        if (StringUtils.isNotBlank(query.getName())){
            wrapper.like(Customer::getName,query.getName());
        }
        if (StringUtils.isNotBlank(query.getPhone())){
            wrapper.like(Customer::getPhone,query.getPhone());
        }
        if (query.getLevel() != null){
            wrapper.eq(Customer::getLevel,query.getLevel());
        }
        if (query.getSource() != null){
            wrapper.eq(Customer::getSource,query.getSource());
        }
        if (query.getFollowStatus() != null){
            wrapper.eq(Customer::getFollowStatus,query.getFollowStatus());
        }
        if (query.getIsPublic() != null){
            wrapper.eq(Customer::getIsPublic,query.getIsPublic());
        }
        wrapper.orderByDesc(Customer::getCreateTime);

        return wrapper;
    }

    @Override
    public Map<String, List> getCustomerTrendData(CustomerTrendQuery query) {
        List<String> timeList = new ArrayList<>();
        List<Integer> countList = new ArrayList<>();
        List<CustomerTrendVO> tradeStatistics;

        if ("day".equals(query.getTransactionType())){
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime truncateNow = now.truncatedTo(ChronoUnit.SECONDS);
            LocalDateTime startTime = now.withHour(0).withMinute(0).withSecond(0).truncatedTo(ChronoUnit.SECONDS);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            ArrayList<String> timeRange = new ArrayList<>();
            timeRange.add(formatter.format(startTime));
            timeRange.add(formatter.format(truncateNow));
            query.setTimeRange(timeRange);

            timeList = getHourData(timeList);
            tradeStatistics = baseMapper.getTradeStatistics(query);
        } else if ("monthrange".equals(query.getTransactionType())) {
            query.setTimeFormat("'%Y-%m'");
            timeList = getMonthInRange(query.getTimeRange().get(0),query.getTimeRange().get(1));
            tradeStatistics = baseMapper.getTradeStatisticsByDay(query);
        } else {
            query.setTimeFormat("'%Y-%m-%d'");
            log.info("时间参数:{}",query.getTimeRange());
            timeList = DateUtils.getDatesInRange(query.getTimeRange().get(0),query.getTimeRange().get(1));
            tradeStatistics = baseMapper.getTradeStatisticsByDay(query);
        }

        List<CustomerTrendVO> finalTradeStatistics = tradeStatistics;
        timeList.forEach(item -> {
            CustomerTrendVO statisticsVO = finalTradeStatistics.stream().filter(vo -> {
                if ("day".equals(query.getTransactionType())){
                    return item.substring(0,2).equals(vo.getTradeTime().substring(0,2));
                }else {
                    return item.equals(vo.getTradeTime());
                }
            })
                    .findFirst()
                    .orElse(null);

            if (statisticsVO != null){
                countList.add(statisticsVO.getTradeCount());
            }else {
                countList.add(0);
            }
        });

        Map<String, List> result = new HashMap<>();
        result.put("timeList",timeList);
        result.put("countList",countList);
        return result;
    }
}
