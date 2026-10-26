package com.syncstock.auth.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @NotBlank
    @Pattern(regexp = "^(Admin|ThuKho|KeToan)$", message = "Vai trò chỉ có thể là Admin, ThuKho hoặc KeToan")
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private String role = "ThuKho";

    @Column(name = "is_active", nullable = false)
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
