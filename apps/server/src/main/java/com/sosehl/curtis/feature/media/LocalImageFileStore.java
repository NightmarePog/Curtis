package com.sosehl.curtis.feature.media;

import com.sosehl.curtis.feature.media.ImageInspector.ImageMetadata;
import com.sosehl.curtis.shared.errors.ProblemException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/** Filesystem-backed storage for validated quiz images. */
@Component
public class LocalImageFileStore implements MediaFileStore {

    private record CopiedUpload(long byteSize, String sha256) {}

    private final Path root;
    private final long maxBytes;
    private final ImageInspector imageInspector;

    public LocalImageFileStore(
        MediaProperties properties,
        ImageInspector imageInspector
    ) {
        this.root = properties.root().toAbsolutePath().normalize();
        this.maxBytes = properties.maxBytes();
        this.imageInspector = imageInspector;
    }

    @PostConstruct
    public void initialize() throws IOException {
        Files.createDirectories(root);
        if (!Files.isDirectory(root) || !Files.isWritable(root)) {
            throw new IOException("Media root is not writable: " + root);
        }
    }

    @Override
    public StoredFile store(
        UUID mediaId,
        String originalName,
        InputStream content
    ) throws IOException {
        String safeName = requireSafeFilename(originalName);
        Path temporaryFile = Files.createTempFile(root, ".upload-", ".tmp");

        try {
            CopiedUpload upload = copyUpload(content, temporaryFile);
            ImageMetadata image = imageInspector.inspect(temporaryFile);
            String storageKey = mediaId + image.extension();
            moveIntoPlace(temporaryFile, resolveStorageKey(storageKey));
            return new StoredFile(
                storageKey,
                safeName,
                image.contentType(),
                upload.byteSize(),
                upload.sha256()
            );
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(temporaryFile);
            throw exception;
        }
    }

    @Override
    public Optional<Resource> find(String storageKey) {
        Path file = resolveStorageKey(storageKey);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(file));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveStorageKey(storageKey));
    }

    private CopiedUpload copyUpload(
        @Nullable InputStream content,
        Path destination
    ) throws IOException {
        if (content == null) throw invalid("Image file is required");

        MessageDigest sha256 = sha256();
        try (
            BoundedInputStream limited = BoundedInputStream.builder()
                .setInputStream(content)
                .setMaxCount(readLimit())
                .setPropagateClose(true)
                .get();
            DigestInputStream hashing = new DigestInputStream(limited, sha256)
        ) {
            long byteSize = Files.copy(
                hashing,
                destination,
                StandardCopyOption.REPLACE_EXISTING
            );
            if (byteSize == 0) throw invalid("Image file is empty");
            if (byteSize > maxBytes) {
                throw invalid("Image exceeds the size limit");
            }
            return new CopiedUpload(
                byteSize,
                HexFormat.of().formatHex(sha256.digest())
            );
        }
    }

    private long readLimit() {
        return maxBytes == Long.MAX_VALUE ? Long.MAX_VALUE : maxBytes + 1;
    }

    private String requireSafeFilename(@Nullable String filename) {
        try {
            if (
                filename == null ||
                filename.isBlank() ||
                filename.length() > 255 ||
                !filename.equals(FilenameUtils.getName(filename)) ||
                filename.equals(".") ||
                filename.equals("..") ||
                filename.codePoints().anyMatch(Character::isISOControl)
            ) {
                throw invalid("A valid original filename is required");
            }
            return filename;
        } catch (IllegalArgumentException exception) {
            throw invalid("A valid original filename is required");
        }
    }

    private Path resolveStorageKey(String storageKey) {
        Path file = root.resolve(storageKey).normalize();
        if (!root.equals(file.getParent())) {
            throw invalid("Invalid media key");
        }
        return file;
    }

    private void moveIntoPlace(Path source, Path target) throws IOException {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileAlreadyExistsException(target.toString());
        }
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static ProblemException invalid(String message) {
        return ProblemException.badRequest("media_invalid", message);
    }
}
