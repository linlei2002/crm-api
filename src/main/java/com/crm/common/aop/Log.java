package com.crm.common.aop;

import com.crm.enums.BusinessType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    //模块名称
    String title() default "";

    //接口功能
    BusinessType businessType() default BusinessType.OTHER;

    //是否保存请求参数
    boolean isSaveRequestData() default true;

    //是否保存响应参数
    boolean isSaveResponseData() default true;

    //排除指定的敏感参数
    String[] excludeParamNames() default {};
}
