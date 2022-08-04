package codes.rafael.mavenchecksumextension.validator;

import org.eclipse.aether.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ArtifactChecksumValidationListener implements ArtifactChecksumListener.Delegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactChecksumValidationListener.class);

    private final Map<String, CharSequence> checksums;

    private final boolean require;

    private final String algorithm;

    private final boolean dryRun;

    private final ConcurrentMap<Map.Entry<String, File>, Boolean> visited = new ConcurrentHashMap<Map.Entry<String, File>, Boolean>();

    ArtifactChecksumValidationListener(File file, boolean require, String algorithm, boolean dryRun) {
        checksums = new HashMap<String, CharSequence>();
        ArtifactChecksumUtils.read(LOGGER, file, checksums);
        this.require = require;
        this.algorithm = algorithm;
        this.dryRun = dryRun;
    }

    ArtifactChecksumValidationListener(ConcurrentMap<String, CharSequence> checksums, boolean require, String algorithm, boolean dryRun) {
        this.checksums = checksums;
        this.require = require;
        this.algorithm = algorithm;
        this.dryRun = dryRun;
    }

    @Override
    public void artifactResolved(RepositoryEvent event) {
        if (ArtifactChecksumUtils.checkDuplicate(LOGGER, event, visited)) {
            return;
        }
        CharSequence expected = checksums.get(event.getArtifact().toString());
        if (expected != null) {
            CharSequence actual = ArtifactChecksumUtils.toHash(algorithm, event.getFile());
            if (ArtifactChecksumUtils.contentEquals(expected, actual)) {
                LOGGER.debug("Matched checksums for {} ({}): {}", event.getArtifact(), event.getFile(), expected);
            } else if (dryRun) {
                LOGGER.warn("Failed to validate checksums for {} ({}): expected {} but computed {}", event.getArtifact(), event.getFile(), expected, actual);
            } else {
                throw new ArtifactChecksumError("Checksums did not match "
                        + "for " + event.getArtifact() + " (" + event.getFile() + "): "
                        + "expected " + expected + " but computed " + actual);
            }
        } else if (require) {
            throw new ArtifactChecksumError("No checksum found for " + event.getArtifact());
        } else if (dryRun) {
            LOGGER.warn("No checksum specified for {}", event.getArtifact());
        }
    }
}
