package studio.clashbuddy.clashaccess.gateway;


public class ClashAuthPayload {
    private final String userId;
    private final String userId2;
    private final String[] roles;
    private final String[] permissions;
    private final String tokenVersion;


    public ClashAuthPayload(String userId,String userId2, String[] roles, String[] permissions, String tokenVersion) {
        this.userId = userId;
        this.userId2 = userId2;
        this.roles = roles;
        this.permissions = permissions;
        this.tokenVersion = tokenVersion;
    }

    public String getTokenVersion() {
        return tokenVersion;
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

    public String getUserId2() {
        return userId2;
    }
}
