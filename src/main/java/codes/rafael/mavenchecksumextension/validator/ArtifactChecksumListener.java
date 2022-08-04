package codes.rafael.mavenchecksumextension.validator;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;

@Singleton
@Component(role = RepositoryListener.class, hint = "codes.rafael.mavenchecksumextension.validator")
public class ArtifactChecksumListener extends AbstractRepositoryListener {

    private final Delegate delegate;

    @Inject
    public ArtifactChecksumListener() {
        String mode = System.getProperty("codes.rafael.mavenchecksumextension.mode");
        if (mode == null) {
            delegate = null;
        } else {
            String fileName = System.getProperty("codes.rafael.mavenchecksumextension.file");
            if (fileName == null) {
                throw new ArtifactChecksumError("Checksum validation file was not specified");
            }
            File file = new File(fileName);
            String algorithm = System.getProperty("codes.rafael.mavenchecksumextension.algorithm", "SHA-256");
            boolean dryRun = Boolean.getBoolean("codes.rafael.mavenchecksumextension.dryrun");
            if (mode.equalsIgnoreCase("collect")) {
                if (file.isDirectory()) {
                    throw new ArtifactChecksumError("Cannot validate against directory: " + fileName);
                }
                boolean append = Boolean.getBoolean("codes.rafael.mavenchecksumextension.append");
                delegate = new ArtifactChecksumCollectionListener(file, append, algorithm, dryRun);
            } else if (mode.equalsIgnoreCase("enforce")) {
                if (!file.isFile()) {
                    throw new ArtifactChecksumError("Cannot validate against non-existing file: " + fileName);
                }
                boolean relaxed = Boolean.getBoolean("codes.rafael.mavenchecksumextension.relaxed");
                delegate = new ArtifactChecksumValidationListener(file, relaxed, algorithm, dryRun);
            } else {
                throw new ArtifactChecksumError("Unknown checksum validation mode: " + mode);
            }
        }
    }

    @Override
    public void artifactResolved(RepositoryEvent event) {
        if (delegate != null) {
            delegate.artifactResolved(event);
        }
    }

    interface Delegate {

        void artifactResolved(RepositoryEvent event);
    }
}
