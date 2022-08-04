package codes.rafael.mavenchecksumextension.validator;

public class ArtifactChecksumError extends Error {

    public ArtifactChecksumError(String message) {
        super(message);
    }

    public ArtifactChecksumError(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
