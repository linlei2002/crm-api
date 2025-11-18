package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.exception.ServerException;
import com.crm.common.result.PageResult;
import com.crm.convert.ContractConvert;
import com.crm.entity.*;
import com.crm.mapper.ContractMapper;
import com.crm.mapper.ContractProductMapper;
import com.crm.mapper.CustomerMapper;
import com.crm.mapper.ProductMapper;
import com.crm.query.ContractQuery;
import com.crm.query.ContractTrendQuery;
import com.crm.query.IdQuery;
import com.crm.security.user.SecurityUser;
import com.crm.service.ContractService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crm.vo.ContractTrendVO;
import com.crm.vo.ContractVO;
import com.crm.vo.ProductVO;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.query.MPJLambdaQueryWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.crm.utils.NumberUtils.generateContractNumber;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements ContractService {
    private final ContractProductMapper contractProductMapper;
    private final ProductMapper productMapper;
    private final CustomerMapper customerMapper;

    @Resource
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public PageResult<ContractVO> getPage(ContractQuery query) {
        Page<ContractVO> page = new Page<>();
        MPJLambdaWrapper<Contract> wrapper = new MPJLambdaWrapper<>();
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(Contract::getName, query.getName());
        }

        if (query.getCustomerId() != null) {
            wrapper.eq(Contract::getCustomerId, query.getCustomerId());
        }

        if (StringUtils.isNotBlank(query.getNumber())) {
            wrapper.like(Contract::getNumber, query.getNumber());
        }

        if (query.getStatus() != null) {
            wrapper.eq(Contract::getStatus, query.getStatus());
        }

        wrapper.orderByDesc(Contract::getCreateTime);
        Integer managerId = SecurityUser.getManagerId();
        wrapper.selectAll(Contract.class)
                .selectAs(Customer::getName,ContractVO::getCustomerName)
                .leftJoin(Customer.class,Customer::getId,Contract::getCustomerId)
                .eq(Contract::getOwnerId,managerId);
        Page<ContractVO> result = baseMapper.selectJoinPage(page, ContractVO.class, wrapper);
        if (!result.getRecords().isEmpty()){
            result.getRecords().forEach(contractVO -> {
                contractProductMapper.selectList(new MPJLambdaWrapper<ContractProduct>().eq(ContractProduct::getId,contractVO.getId()));
                contractVO.setProducts(ContractConvert.INSTANCE.convertToProductVOList(contractProductMapper.selectList(new MPJLambdaWrapper<ContractProduct>().eq(ContractProduct::getCId,contractVO.getId()))));
            });
        }
        return new PageResult<>(result.getRecords(),result.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(ContractVO contractVO) {
        boolean isNew = contractVO.getId() == null;
        if (isNew && baseMapper.exists(new LambdaQueryWrapper<Contract>().eq(Contract::getName, contractVO.getName()))){
            throw new ServerException("合同名称已存在,请勿重复添加");
        }

        Contract contract = ContractConvert.INSTANCE.convert(contractVO);
        contract.setCreaterId(SecurityUser.getManagerId());
        contract.setOwnerId(SecurityUser.getManagerId());

        if (isNew) {
            log.info(generateContractNumber());
            contract.setNumber(generateContractNumber());
            baseMapper.insert(contract);
        }else {
            Contract oldContract = baseMapper.selectById(contractVO.getId());
            if (oldContract == null) {
                throw new ServerException("合同不存在");
            }
            if (oldContract.getStatus() == 1){
                throw new ServerException("合同正在审核,请勿操作");
            }
            baseMapper.updateById(contract);
        }

        handleContractProducts(contract.getId(),contractVO.getProducts());
    }

    @Override
    public void returnApproval(IdQuery query) {
        Contract contract = baseMapper.selectById(query.getId());
        if (contract == null){
            throw new ServerException("合同不存在");
        }
        if (contract.getStatus() == 2){
            throw new ServerException("该合同审核已通过,请勿重复操作");
        }
        contract.setStatus(3);
        baseMapper.updateById(contract);
    }

    @Override
    public void successApproval(IdQuery query) {
        Contract contract = baseMapper.selectById(query.getId());
        if (contract == null){
            throw new ServerException("合同不存在");
        }
        if (contract.getStatus() == 2 || contract.getStatus() == 3){
            throw new ServerException("该合同审核已通过,或被退回");
        }
        contract.setStatus(2);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        contract.setUpdateTime( now);
        sendMail(query);
        baseMapper.updateById(contract);
    }

    @Override
    public void deleteApproval(IdQuery query) {
        Contract contract = baseMapper.selectById(query.getId());
        if (contract == null){
            throw new ServerException("合同不存在");
        }
        if (contract.getStatus() == 2){
            contract.setStatus(1);
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            contract.setUpdateTime( now);
            baseMapper.updateById(contract);
        }else {
            throw new ServerException("该合同审核未通过或退回,请勿重复操作");
        }
    }

    @Override
    public void startApproval(IdQuery query) {
        Contract contract = baseMapper.selectById(query.getId());
        if (contract == null){
            throw new ServerException("合同不存在");
        }
        if (contract.getStatus() != 0){
            throw new ServerException("该合同审核已通过，请勿重复提交");
        }
        contract.setStatus(1);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        contract.setUpdateTime( now);
        baseMapper.updateById(contract);
    }

    @Override
    public Map<String, List> getContractStatistics(ContractTrendQuery query) {
        log.info("getContractStatistics: {}", query.getTransactionType());
        List<BigDecimal> totalAmount = new ArrayList<>();
        List<BigDecimal> receivedAmount = new ArrayList<>();
        List<String> contractName = new ArrayList<>();
        if("default".equals(query.getTransactionType())){
            List<ContractTrendVO> contractTrendVOList = baseMapper.getTotalContractStatistics();
            for (ContractTrendVO contractTrendVO : contractTrendVOList) {
                totalAmount.add(contractTrendVO.getTotalAmount());
                receivedAmount.add(contractTrendVO.getReceivedAmount());
                contractName.add(contractTrendVO.getContractName());
            }
        }else {
            List<ContractTrendVO> contractTrendVOList = baseMapper.getTradeStatisticsByDay(query);
            for (ContractTrendVO contractTrendVO : contractTrendVOList) {
                totalAmount.add(contractTrendVO.getTotalAmount());
                receivedAmount.add(contractTrendVO.getReceivedAmount());
                contractName.add(contractTrendVO.getContractName());
            }
        }

        HashMap<String, List> result = new HashMap<>();
        result.put("receivedAmount",receivedAmount);
        result.put("totalAmount",totalAmount);
        result.put("contractName",contractName);

        return result;
    }

    @Override
    public String sendMail(IdQuery query) {
        Contract contract = baseMapper.selectById(query.getId());
        Integer customerId = contract.getCustomerId();
        Customer customer = customerMapper.selectById(customerId);
        String emailTo = customer.getEmail();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(emailTo);
        message.setSubject("合同申请");
        message.setText("合同审核成功");
        try {
            javaMailSender.send(message);
            return "合同审核成功";
        } catch (Exception e){
            return "邮件发送失败";
        }
    }

    private void handleContractProducts(Integer contractId, List<ProductVO> newProductList) {
        if (newProductList == null){
            return;
        }
        List<ContractProduct> oldProducts = contractProductMapper.selectList(new LambdaQueryWrapper<ContractProduct>().eq(ContractProduct::getCId, contractId));
        List<ProductVO> addProductList = newProductList.stream().filter(item -> oldProducts.stream().noneMatch(op -> op.getPId().equals(item.getId()))).toList();

        for (ProductVO productVO : addProductList) {
            Product product = checkProductStock(productVO.getId(), productVO.getCount());
            decreaseStock(product, productVO.getCount());
            contractProductMapper.insert(buildContractProduct(contractId, product, productVO.getCount()));
        }

        List<ProductVO> changeProducts = newProductList.stream().filter(item -> oldProducts.stream().anyMatch(op -> op.getPId().equals(item.getId()) && !op.getCount().equals(item.getCount()))).toList();

        for (ProductVO productVO : changeProducts) {
            ContractProduct contractProduct = oldProducts.stream().filter(item -> item.getPId().equals(productVO.getId())).findFirst().orElseThrow();
            Product product = checkProductStock(productVO.getId(), 0);
            int diff = productVO.getCount() - contractProduct.getCount();
            if (diff > 0) {
                decreaseStock(product, diff);
            } else {
                increaseStock(product, -diff);
            }
//            更新商品合同信息
            contractProduct.setCount(productVO.getCount());
            contractProduct.setPrice(product.getPrice());
            contractProduct.setTotalPrice(product.getPrice().multiply(new BigDecimal(productVO.getCount())));
            contractProductMapper.updateById(contractProduct);
        }

        List<ContractProduct> removedProduct = oldProducts.stream().filter(item -> newProductList.stream().noneMatch(np -> np.getId().equals(item.getPId()))).toList();
        for (ContractProduct contractProduct : removedProduct) {
            Product product = productMapper.selectById(contractProduct.getPId());
            if (product != null) {
                increaseStock(product, contractProduct.getCount());
            }
            contractProductMapper.deleteById(contractProduct);
        }
    }

    private ContractProduct buildContractProduct(Integer contractId, Product product, int count) {
        ContractProduct contractProduct = new ContractProduct();
        contractProduct.setCId(contractId);
        contractProduct.setPId(product.getId());
        contractProduct.setPName(product.getName());
        contractProduct.setPrice(product.getPrice());
        contractProduct.setCount(count);
        contractProduct.setTotalPrice(product.getPrice().multiply(new BigDecimal(count)));
        return contractProduct;
    }

    private Product checkProductStock(Integer productId, int count) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new ServerException("商品不存在");
        }

        if (product.getStock() < count) {
            throw new ServerException("商品库存不足");
        }
        return product;
    }

    private void increaseStock(Product product, int count) {
        product.setStock(product.getStock() + count);
        product.setSales(product.getSales() - count);
        productMapper.updateById(product);
    }

    private void decreaseStock(Product product, int count) {
        product.setStock(product.getStock() - count);
        product.setSales(product.getSales() + count);
        productMapper.updateById(product);
    }
}
