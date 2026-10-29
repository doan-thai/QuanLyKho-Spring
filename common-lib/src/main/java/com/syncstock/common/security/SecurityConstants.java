package com.syncstock.common.security;

import java.util.Set;

public final class SecurityConstants {

    private SecurityConstants() {
        // Utility class
    }

    public static final String DEFAULT_ISSUER = "QuanLyKho.AuthService";
    public static final String DEFAULT_AUDIENCE = "QuanLyKho.Client";
    public static final long DEFAULT_EXPIRATION_MS = 60 * 60 * 1000L; // 60 minutes
    /** Độ dài tối thiểu (byte) cho JWT secret. */
    public static final int MIN_SECRET_LENGTH = 32;

    public static final String COOKIE_NAME = "access_token";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    // Role names (matching 100% case-sensitive claims without ROLE_ prefix)
    public static final String ROLE_ADMIN = "Admin";
    public static final String ROLE_THUKHO = "ThuKho";
    public static final String ROLE_KETOAN = "KeToan";

    public static final Set<String> ALL_ROLES = Set.of(ROLE_ADMIN, ROLE_THUKHO, ROLE_KETOAN);
    public static final Set<String> MANAGED_ROLES = Set.of(ROLE_THUKHO, ROLE_KETOAN);

    // Claims keys
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_UNIQUE_NAME = "unique_name";
    public static final String CLAIM_ROLE = "role";
}
