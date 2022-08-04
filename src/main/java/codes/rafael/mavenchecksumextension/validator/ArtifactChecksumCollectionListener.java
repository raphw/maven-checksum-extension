package codes.rafael.mavenchecksumextension.validator;

import org.eclipse.aether.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class ArtifactChecksumCollectionListener implements ArtifactChecksumListener.Delegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactChecksumValidationListener.class);

    private final File file;

    private final String algorithm;

    private final ConcurrentMap<String, CharSequence> checksums;

    private final ConcurrentMap<Map.Entry<String, File>, Boolean> visited = new ConcurrentHashMap<Map.Entry<String, File>, Boolean>();

    ArtifactChecksumCollectionListener(final File file, boolean append, String algorithm, boolean dryRun) {
        this.algorithm = algorithm;
        this.file = file;
        checksums = new ConcurrentHashMap<String, CharSequence>();
        if (this.file.exists() && append) {
            ArtifactChecksumUtils.read(LOGGER, this.file, checksums);
        }
        if (!dryRun) {
            Runtime.getRuntime().addShutdownHook(new Thread("codes.rafael.mavenchecksumextension.validator") {
                @Override
                public void run() {
                    ArtifactChecksumUtils.write(LOGGER, file, checksums);
                }
            });
        }
    }

    ArtifactChecksumCollectionListener(File file, String algorithm, ConcurrentMap<String, CharSequence> checksums) {
        this.file = file;
        this.algorithm = algorithm;
        this.checksums = checksums;
    }

    @Override
    public void artifactResolved(RepositoryEvent event) {
        if (ArtifactChecksumUtils.checkDuplicate(LOGGER, event, visited)) {
            return;
        }
        CharSequence checksum = ArtifactChecksumUtils.toHash(algorithm, event.getFile());
        CharSequence previous = checksums.put(event.getArtifact().toString(), checksum);
        if (previous != null && !ArtifactChecksumUtils.contentEquals(previous, checksum)) {
            LOGGER.warn("Replacing previously collected checksum of {} ({}): {} with {}", event.getArtifact(), event.getFile(), previous, checksum);
        }
    }
}
