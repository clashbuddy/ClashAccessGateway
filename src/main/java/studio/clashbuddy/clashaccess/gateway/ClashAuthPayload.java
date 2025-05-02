package studio.clashbuddy.clashaccess.gateway;


public class ClashAuthPayload {
    private final String userId;
    private final String[] roles;
    private final String[] permissions;


    public ClashAuthPayload(String userId, String[] roles, String[] permissions) {
        this.userId = userId;
        this.roles = roles;
        this.permissions = permissions;
    }

    public String getUserId() {
        return userId;
    }

    public String[] getRoles() {
        return roles;
    }

    public String[] getPermissions() {
        return permissions;
    }
}
