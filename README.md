# Maven checksum extension

This extension allows for the creation and the enforcement of checksums for any artifact that is resolved by Maven. Without such validation, 
a corrupted version could otherwise enable supply-chain attacks via remote code execution, if an attacker is able to replace an artifact in 
a Maven repository, or otherwise intercepts a download via a man-in-the-middle attack. 

If a secure checksum is available for each artifact, this extension can validate each artifact against this checksum before allowing the
artifact to execute any code. If a checksum does not match, this extension ends the execution process immediately to avoid any possible
damage. For this purpose, this extension can generate a file which is checked into a project against which all later downloads are validated.
As a side effect, this validation would also discover retagged versions, for example if snapshots were used.

To activate this extension, its jar file should be either placed in the Maven installation directory under *lib/ext* or be specified via the 
*-Dmaven.ext.class.path=<extension>* property. It should however **not** be specified via *.mvn/extensions.xml* as this would require the
extension to be downloaded without any validation of the extension itself what would defeat its purpose. (Hopefully, something similar will
however become a Maven core feature at some point.)

### Collecting checksums

The extension will collect any checksums of artifacts that are being resolved. Therefore, it is important to run the same goal as would
be run during validation. Typically, checksum validation is required during sensitive builds, such as during a release build:

```cmd
mvn release:prepare release:perform -DdryRun \
  -Dmaven.ext.class.path=maven-checksum-extension.jar \
  -Dcodes.rafael.mavenchecksumextension.file=./checksums.sha256 \
  -Dcodes.rafael.mavenchecksumextension.mode=collect
```

By default, this extension uses SHA-256 checksums which are easier to compute than SHA-512 checksums but still considered non-immitatable.
A different algorithm can be specified by setting *codes.rafael.mavenchecksumextension.algorithm*, but it is highly discouraged to use 
weak checksums such as SHA1 or MD5.

It is possible to retain values of a previously generated file by setting *-Dcodes.rafael.mavenchecksumextension.append*. Changed checksums
for the same artifact will be replaced in such a run.

### Validating checksums

During the actual build, which usually happens on a different machine in the network, the collected checksums are now available via the 
file that was previously collected and which is stored in the version control system, together with the project's source code. These
checksums are now enforced to ensure that any remotly altered artifact is downloaded and handed execution privilege:

```cmd
mvn release:prepare release:perform \
  -Dmaven.ext.class.path=maven-checksum-extension.jar \
  -Dcodes.rafael.mavenchecksumextension.file=./checksums.sha256 \
  -Dcodes.rafael.mavenchecksumextension.mode=enforce
```

It is possible to ignore unknown artifacts by setting *-Dcodes.rafael.mavenchecksumextension.relaxed*. This is not recommended as it opens
for corruption via these artifacts, but allows to accept artifacts that might not otherwise be available and are known to be trustworthy.

### Recommended workflow

Of course, if a remote repository already is corrupted, the generated checksums will reflect the corrupted artifacts during creation. 
However, at least, if another machine builds the same project and fails checksum validation when it has a different artifact representation
stored in its local cache or fetches artifacts from a different Maven repository server, the corruption of the collection run can be 
discovered rather easily. Ideally, checksums are however fetched from a properly patched and supervised machine. In particular, it reduces
the danger of executing against shared caches in any subsequent, validating build.

Checksum collection must be repeated after each update of Maven or a project's POM as different artifact or artifact versions might be resolved.
This extension enforces a stable sort order for the generated file such that artifact changes, including transitives, become visible by their
checksum changes.

By default, the extension does neither collect nor enforce checksums for snapshot versions. Set `-Dcodes.rafael.mavenchecksumextension.mode`
to enable such validation.

### Integrating the extension

When building on a server with a generic Maven installation, the extension will not be available. To download the extension securely, 
the `mvnchecksum` or `mvnchecksum.cmd` files in this project can be added to any project for execution. Both scripts will also 
validate the downloaded jar file against a SHA-256 checksum to avoid the corruption of the extension itself, prior to its execution.

The script can be executed directly from a build server script, or can be integrated by adding `/bin/sh .mvn/maven-checksum/mvnc` to a
shell script, or `CALL .mvn\maven-checksum\mvnc.cmd` to a Windows batch file. This way, the extension can for example be integrated into 
Maven Wrapper. If doing so, guard the script execution with an `if` statement (or error level jump on Windows), to exit Maven wrapper 
in case of a failure. Note that a non-existing Maven extension is unfortunately by Maven what will result in your build executing without
checksum verification.
