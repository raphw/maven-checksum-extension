package codes.rafael.mavenchecksumextension.validator;

import org.eclipse.aether.RepositoryEvent;
import org.slf4j.Logger;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;

class ArtifactChecksumUtils {

    static boolean checkDuplicate(Logger logger, RepositoryEvent event, ConcurrentMap<Map.Entry<String, File>, Boolean> visited) {
        Map.Entry<String, File> entry = new AbstractMap.SimpleEntry<String, File>(event.getArtifact().toString(), event.getFile());
        if (visited.put(entry, true) != null) {
            logger.debug("Already validated artifact {} ({})", event.getArtifact(), event.getFile());
            return true;
        } else {
            return false;
        }
    }

    static CharSequence toHash(String algorithm, File file) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            try {
                InputStream inputStream = new FileInputStream(file);
                try {
                    byte[] buffer = new byte[1024 * 8];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        messageDigest.update(buffer, 0, length);
                    }
                } finally {
                    inputStream.close();
                }
            } catch (IOException e) {
                throw new ArtifactChecksumError("Failed to compute " + algorithm + " of " + file, e);
            }
            byte[] digest = messageDigest.digest();
            StringBuilder actualSum = new StringBuilder(digest.length * 2);
            for (byte aByte : digest) {
                actualSum.append(String.format("%02x", aByte));
            }
            return actualSum;
        } catch (NoSuchAlgorithmException e) {
            throw new ArtifactChecksumError("Could not resolve checksum algorithm " + algorithm, e);
        }
    }

    static void read(Logger logger, File file, Map<String, CharSequence> checksums) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    String[] elements = line.split(" ", 2);
                    if (elements.length != 2) {
                        logger.warn("Ignoring line '{}' which does not follow expected format", line);
                    } else {
                        CharSequence previous = checksums.put(elements[0], elements[1]);
                        if (previous != null && !elements[1].contentEquals(previous)) {
                            logger.warn("Duplicate checksum for '{}', replacing {} with {}", elements[0], previous, elements[1]);
                        }
                    }
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new ArtifactChecksumError("Failed to read checksums from " + file, e);
        }
    }

    static void write(Logger logger, File file, Map<String, ? extends CharSequence> checksums) {
        Set<String> artifacts = new TreeSet<String>(checksums.keySet());
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            try {
                for (String artifact : artifacts) {
                    CharSequence hash = checksums.get(artifact);
                    writer.append(artifact);
                    writer.append(" ");
                    writer.append(hash);
                    writer.newLine();
                }
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            logger.error("Failed to write checksums to {}", file, e);
        }
    }

    static boolean contentEquals(CharSequence left, CharSequence right) {
        if (left instanceof String) {
            return ((String) left).contentEquals(right);
        } else if (right instanceof String) {
            return ((String) right).contentEquals(left);
        } else {
            return left.toString().contentEquals(right);
        }
    }
}
