package common;

/**
 * common.GetTopics
 */
public class GetTopics {
    private String username;

    /**
     * Parameterized class builder.
     * 
     * @param username
     */
    public GetTopics(String username) {
        this.username = username;
    }

    /**
     * Get - username.
     * 
     * @param
     * @return username
     */
    public String getUsername() {
        return username;
    }
}
