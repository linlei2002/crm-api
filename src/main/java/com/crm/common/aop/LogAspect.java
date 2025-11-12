package com.crm.common.aop;

import com.alibaba.fastjson2.JSON;
import com.crm.common.filter.PropertyPreExcludeFilter;
import com.crm.entity.Manager;
import com.crm.entity.OperLog;
import com.crm.enums.BusinessStatus;
import com.crm.security.user.ManagerDetail;
import com.crm.security.user.SecurityUser;
import com.crm.service.OperLogService;
import com.crm.utils.IpUtils;
import com.crm.utils.ServletUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NamedThreadLocal;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.Map;

@Aspect
@Component
public class LogAspect {

    public static final String[] EXCLUDE_PARAM_NAMES = {"password", "oldPassword", "newPassword", "confirmPassword"};

    public static final Logger log = LoggerFactory.getLogger(LogAspect.class.getName());

    private static final ThreadLocal<Long> TIME_THREADLOCAL = new NamedThreadLocal<>("Cost time");

    @Resource
    private OperLogService operLogService;

    private PropertyPreExcludeFilter excludePropertyPreFilter(String[] excludeParamNames){
        return new PropertyPreExcludeFilter().addExcludes(ArrayUtils.addAll(excludeParamNames, EXCLUDE_PARAM_NAMES));
    }

    @Before(value = "@annotation(controllerLog)")
    public void doBefore(JoinPoint joinPoint, Log controllerLog) throws Throwable {
        TIME_THREADLOCAL.set(System.currentTimeMillis());
    }

    @AfterReturning(pointcut = "@annotation(controllerLog)",returning = "jsonResult")
    public void doAfterReturning(Object jsonResult, JoinPoint joinPoint, Log controllerLog) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    @AfterThrowing(pointcut = "@annotation(controllerLog)",throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e,Object jsonResult) {
        try {
            ManagerDetail loginManager = SecurityUser.getManager();
            OperLog operLog = new OperLog();
            operLog.setStatus(BusinessStatus.SUCCESS.ordinal());

            String ip = IpUtils.getIpAddr();
            operLog.setOperIp(ip);
            operLog.setOperUrl(StringUtils.substring(ServletUtils.getRequest().getRequestURI(),0,255));

            if (loginManager != null){
                operLog.setOperName(loginManager.getAccount());
                operLog.setManagerId(loginManager.getId().toString());
            }

            if (e != null){
                operLog.setStatus(BusinessStatus.FAIL.ordinal());
                operLog.setErrorMsg(StringUtils.substring(e.getMessage(), 0, 2000));
            }

            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");

            operLog.setRequestMethod(ServletUtils.getRequest().getMethod());

            getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);

            operLog.setCostTime(System.currentTimeMillis() - TIME_THREADLOCAL.get());

            operLogService.recordOperLog(operLog);

        } catch (Exception ex) {
            log.error("操作日志记录发生异常，详细堆栈如下：", ex.getMessage());
        } finally {
            TIME_THREADLOCAL.remove();
        }
    }

    public void getControllerMethodDescription(JoinPoint joinPoint, Log log, OperLog operLog, Object jsonResult) throws Exception {
        operLog.setTitle(log.title());
        operLog.setOperType(log.businessType().ordinal());
        if (log.isSaveRequestData()){
            setRequestValue(joinPoint, operLog, log.excludeParamNames());
        }

        if (log.isSaveResponseData()){
            operLog.setJsonResult(StringUtils.substring(JSON.toJSONString(jsonResult, excludePropertyPreFilter(EXCLUDE_PARAM_NAMES)), 0, 2000));
        }
    }

    private void setRequestValue(JoinPoint joinPoint, OperLog operLog, String[] excludeParamNames) throws Exception {
        Map<?, ?> paramsMap = ServletUtils.getParamMap(ServletUtils.getRequest());
        String requestMethod = operLog.getRequestMethod();
        if (paramsMap.isEmpty()
                && (HttpMethod.PUT.name().equals(requestMethod) || HttpMethod.POST.name().equals(requestMethod))) {
            String params = argsArrayToString(joinPoint.getArgs(), excludeParamNames);
            operLog.setOperParam(StringUtils.substring(params, 0, 2000));
        } else {
            operLog.setOperParam(StringUtils.substring(JSON.toJSONString(paramsMap, excludePropertyPreFilter(excludeParamNames)), 0, 2000));
        }
    }

    private String argsArrayToString(Object[] paramsArray, String[] excludeParamNames) {
        StringBuilder params = new StringBuilder();
        if (paramsArray != null && paramsArray.length > 0) {
            for (Object o : paramsArray) {
                if (ObjectUtils.isNotEmpty(o) && !isFilterObject(o)) {
                    try {
                        String jsonObj = JSON.toJSONString(o, excludePropertyPreFilter(excludeParamNames));
                        params.append(jsonObj).append(" ");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return params.toString().trim();
    }

    @SuppressWarnings("rawtypes")
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        // 检查是否是数组
        if (clazz.isArray()) {
            // 判断数组的元素类型是否是MultipartFile或其子类
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            // 只要集合中有一个元素是MultipartFile，就整个过滤掉
            Collection collection = (Collection) o;
            for (Object value : collection) {
                return value instanceof MultipartFile;
            }
        } else if (Map.class.isAssignableFrom(clazz)) {
            // 只要Map中有一个值是MultipartFile，就整个过滤掉
            Map map = (Map) o;
            for (Object value : map.entrySet()) {
                Map.Entry entry = (Map.Entry) value;
                return entry.getValue() instanceof MultipartFile;
            }
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse
                || o instanceof BindingResult;
    }


}
