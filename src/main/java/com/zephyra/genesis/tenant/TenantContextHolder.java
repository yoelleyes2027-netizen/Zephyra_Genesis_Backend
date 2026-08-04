package com.zephyra.genesis.tenant;

public final class TenantContextHolder {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setMaster() {
        CURRENT_TENANT.set(null);
    }

    public static void setTenantDatabase(String tenantDatabase) {
        CURRENT_TENANT.set(tenantDatabase);
    }

    public static String getCurrentTenantDatabase() {
        return CURRENT_TENANT.get();
    }

    public static boolean isMaster() {
        return CURRENT_TENANT.get() == null || CURRENT_TENANT.get().isBlank();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}