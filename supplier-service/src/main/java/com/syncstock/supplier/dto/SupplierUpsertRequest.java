package com.syncstock.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierUpsertRequest {

    @NotBlank(message = "Tên nhà cung cấp là bắt buộc")
    @Size(max = 200, message = "Tên nhà cung cấp không được vượt quá 200 ký tự")
    private String name;

    @Size(max = 50, message = "Tên người liên hệ không được vượt quá 50 ký tự")
    private String contactPerson;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^(0\\d{9}|\\+84\\d{9})$", message = "Số điện thoại không đúng định dạng (vd: 0901234567 hoặc +84901234567)")
    private String phone;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
    private String email;

    @Size(max = 300, message = "Địa chỉ không được vượt quá 300 ký tự")
    private String address;

    @Builder.Default
    private Boolean isActive = true;
}
