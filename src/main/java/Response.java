/**
 * Response
 */
public class Response {
    private boolean success;

    /**
     * Parameterized class builder.
     * 
     * @param success
     */
    public Response(boolean success) {
        this.success = success;
    }

    /**
     * Get - success.
     * 
     * @param
     * @return success status
     */
    public boolean getSuccess() {
        return this.success;
    }
}