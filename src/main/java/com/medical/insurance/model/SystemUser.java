package com.medical.insurance.model;

public final class SystemUser {

    private final long userId;
    private final String username;
    private final String passwordHash;
    private final String realName;
    private final String roleCode;
    private final boolean enabled;

    public SystemUser(long userId, String username, String passwordHash, String realName, String roleCode, boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.realName = realName;
        this.roleCode = roleCode;
        this.enabled = enabled;
    }

    public long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRealName() { return realName; }
    public String getRoleCode() { return roleCode; }
    public boolean isEnabled() { return enabled; }
}
