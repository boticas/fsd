/**
 * Response
 */
public class Response {
    private boolean success;

    public Response(boolean success) {
        this.success = success;
    }

    /**
     * @return the success status
     */
    public boolean getSuccess() {
        return this.success;
    }
}
