package com.syncstock.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {

    @NotBlank(message = "Vui lòng chọn vai trò")
    @Pattern(regexp = "^(ThuKho|KeToan)$", message = "Chỉ được phép chuyển sang vai trò ThuKho hoặc KeToan")
    private String role;
}
