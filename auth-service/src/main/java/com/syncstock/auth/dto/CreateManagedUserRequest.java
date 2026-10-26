package com.syncstock.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class CreateManagedUserRequest {

    @NotBlank(message = "Vui lòng nhập tên đăng nhập")
    @Size(min = 3, max = 50, message = "Tên đăng nhập từ 3 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 6, max = 100, message = "Mật khẩu cần ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng chọn vai trò")
    @Pattern(regexp = "^(ThuKho|KeToan)$", message = "Chỉ được phép tạo tài khoản với vai trò ThuKho hoặc KeToan")
    private String role;

    @JsonProperty("isActive")
    @Builder.Default
    private Boolean isActive = true;

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        this.isActive = active;
    }
}
