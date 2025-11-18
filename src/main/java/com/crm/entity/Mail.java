package com.crm.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class Mail {
    @NotEmpty
    @NotBlank
    @Email
    private String to;

    @NotBlank
    @NotEmpty
    //主题
    private String subject;

    @NotBlank
    @NotEmpty
    private String content;
}
