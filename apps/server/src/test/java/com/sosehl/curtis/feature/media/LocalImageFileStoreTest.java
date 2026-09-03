package com.sosehl.curtis.feature.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sosehl.curtis.feature.media.MediaFileStore.StoredFile;
import com.sosehl.curtis.shared.errors.ProblemException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LocalImageFileStoreTest {

    private static final long DEFAULT_MAX_BYTES = 1024 * 1024;
    private static final long DEFAULT_MAX_PIXELS = 100;

    @TempDir
    Path directory;

    private LocalImageFileStore files;

    @BeforeEach
    void setUp() throws IOException {
        files = fileStore(DEFAULT_MAX_BYTES, DEFAULT_MAX_PIXELS);
    }

    @ParameterizedTest(name = "stores decoded {0} content")
    @MethodSource("supportedImages")
    void storesSupportedImagesWithExactBytesAndDigest(
        String format,
        byte[] content,
        String contentType,
        String extension
    ) throws Exception {
        UUID mediaId = UUID.randomUUID();

        StoredFile stored = files.store(
            mediaId,
            "diagram." + format.toLowerCase(),
            new ByteArrayInputStream(content)
        );

        assertThat(stored.storageKey()).isEqualTo(mediaId + extension);
        assertThat(stored.contentType()).isEqualTo(contentType);
        assertThat(stored.byteSize()).isEqualTo(content.length);
        assertThat(stored.sha256()).isEqualTo(sha256(content));
        assertThat(Files.readAllBytes(directory.resolve(stored.storageKey())))
            .isEqualTo(content);
    }

    @Test
    void detectsContentInsteadOfTrustingFilenameExtension() throws Exception {
        byte[] content = png();

        StoredFile stored = files.store(
            UUID.randomUUID(),
            "actually-a-png.webp",
            new ByteArrayInputStream(content)
        );

        assertThat(stored.originalName()).isEqualTo("actually-a-png.webp");
        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.storageKey()).endsWith(".png");
    }

    @Test
    void acceptsAnImageAtTheExactByteLimit() throws Exception {
        byte[] content = png();
        LocalImageFileStore exactLimit = fileStore(
            content.length,
            DEFAULT_MAX_PIXELS
        );

        StoredFile stored = exactLimit.store(
            UUID.randomUUID(),
            "limit.png",
            new ByteArrayInputStream(content)
        );

        assertThat(stored.byteSize()).isEqualTo(content.length);
        assertThat(Files.readAllBytes(directory.resolve(stored.storageKey())))
            .isEqualTo(content);
    }

    @Test
    void rejectsOneByteOverTheLimitWithoutLeavingAFile() throws Exception {
        byte[] content = png();
        LocalImageFileStore belowLimit = fileStore(
            content.length - 1L,
            DEFAULT_MAX_PIXELS
        );

        assertThatThrownBy(() -> belowLimit.store(
            UUID.randomUUID(),
            "too-large.png",
            new ByteArrayInputStream(content)
        )).isInstanceOfSatisfying(ProblemException.class, exception -> {
            assertThat(exception.code()).isEqualTo("media_invalid");
            assertThat(exception.getMessage()).isEqualTo(
                "Image exceeds the size limit"
            );
        });
        assertThat(directory).isEmptyDirectory();
    }

    @Test
    void rejectsEmptyContentWithoutLeavingATemporaryFile() {
        assertThatThrownBy(() -> files.store(
            UUID.randomUUID(),
            "empty.png",
            new ByteArrayInputStream(new byte[0])
        )).isInstanceOfSatisfying(ProblemException.class, exception -> {
            assertThat(exception.code()).isEqualTo("media_invalid");
            assertThat(exception.getMessage()).isEqualTo("Image file is empty");
        });
        assertThat(directory).isEmptyDirectory();
    }

    @Test
    void rejectsMissingContentWithoutLeavingATemporaryFile() {
        assertThatThrownBy(() ->
            files.store(UUID.randomUUID(), "missing.png", null)
        ).isInstanceOfSatisfying(ProblemException.class, exception -> {
            assertThat(exception.code()).isEqualTo("media_invalid");
            assertThat(exception.getMessage()).isEqualTo(
                "Image file is required"
            );
        });
        assertThat(directory).isEmptyDirectory();
    }

    @Test
    void rejectsOriginalFilenameContainingAPath() throws Exception {
        assertThatThrownBy(() -> files.store(
            UUID.randomUUID(),
            "../diagram.png",
            new ByteArrayInputStream(png())
        )).isInstanceOfSatisfying(ProblemException.class, exception ->
            assertThat(exception.code()).isEqualTo("media_invalid")
        );
        assertThat(directory).isEmptyDirectory();
    }

    @Test
    void rejectsCorruptContentWithoutLeavingATemporaryFile() {
        assertThatThrownBy(() -> files.store(
            UUID.randomUUID(),
            "corrupt.png",
            new ByteArrayInputStream("not an image".getBytes(StandardCharsets.UTF_8))
        )).isInstanceOfSatisfying(ProblemException.class, exception ->
            assertThat(exception.code()).isEqualTo("media_invalid")
        );
        assertThat(directory).isEmptyDirectory();
    }

    @Test
    void rejectsAValidButUnsupportedImageFormat() throws Exception {
        assertThatThrownBy(() -> files.store(
            UUID.randomUUID(),
            "unsupported.gif",
            new ByteArrayInputStream(image("gif"))
        )).isInstanceOfSatisfying(ProblemException.class, exception -> {
            assertThat(exception.code()).isEqualTo("media_invalid");
            assertThat(exception.getMessage()).isEqualTo(
                "Only decoded PNG, JPEG, and WebP images are accepted"
            );
        });
        assertThat(directory).isEmptyDirectory();
    }

    @Test
    void rejectsPixelBombBeforeDecodingTheCompleteImage() throws Exception {
        LocalImageFileStore strict = fileStore(DEFAULT_MAX_BYTES, 100);

        assertThatThrownBy(() -> strict.store(
            UUID.randomUUID(),
            "huge.png",
            new ByteArrayInputStream(pngHeader(50_000, 50_000))
        )).isInstanceOfSatisfying(ProblemException.class, exception -> {
            assertThat(exception.code()).isEqualTo("media_invalid");
            assertThat(exception.getMessage()).isEqualTo(
                "Image dimensions are too large"
            );
        });
        assertThat(directory).isEmptyDirectory();
    }

    @Test
    void rejectsWebpHeaderWithoutAnImagePayload() {
        assertThatThrownBy(() -> files.store(
            UUID.randomUUID(),
            "forged.webp",
            new ByteArrayInputStream(webpExtendedHeader(2, 1))
        )).isInstanceOfSatisfying(ProblemException.class, exception -> {
            assertThat(exception.code()).isEqualTo("media_invalid");
            assertThat(exception.getMessage()).isEqualTo("Image data is invalid");
        });
        assertThat(directory).isEmptyDirectory();
    }

    @Test
    void rejectsAnimatedWebpContainers() {
        assertThatThrownBy(() -> files.store(
            UUID.randomUUID(),
            "animated.webp",
            new ByteArrayInputStream(webpExtendedHeader(2, 1, 0x02))
        )).isInstanceOfSatisfying(ProblemException.class, exception -> {
            assertThat(exception.code()).isEqualTo("media_invalid");
            assertThat(exception.getMessage()).isEqualTo("Image data is invalid");
        });
        assertThat(directory).isEmptyDirectory();
    }

    @Test
    void removesTemporaryFileWhenTheUploadStreamFails() throws Exception {
        byte[] content = png();

        assertThatThrownBy(() -> files.store(
            UUID.randomUUID(),
            "broken-stream.png",
            new FailingInputStream(content, content.length / 2)
        )).isInstanceOf(IOException.class)
            .hasMessage("simulated upload failure");
        assertThat(directory).isEmptyDirectory();
    }

    @Test
    void collisionDoesNotOverwriteOrDeleteTheExistingFile() throws Exception {
        UUID mediaId = UUID.randomUUID();
        Path existing = directory.resolve(mediaId + ".png");
        byte[] original = "existing media".getBytes(StandardCharsets.UTF_8);
        Files.write(existing, original);

        assertThatThrownBy(() -> files.store(
            mediaId,
            "replacement.png",
            new ByteArrayInputStream(png())
        )).isInstanceOf(FileAlreadyExistsException.class);

        assertThat(Files.readAllBytes(existing)).isEqualTo(original);
        try (Stream<Path> storedFiles = Files.list(directory)) {
            assertThat(storedFiles).containsExactly(existing);
        }
    }

    private LocalImageFileStore fileStore(long maxBytes, long maxPixels)
        throws IOException {
        MediaProperties properties = new MediaProperties(
            directory,
            maxBytes,
            maxPixels
        );
        LocalImageFileStore store = new LocalImageFileStore(
            properties,
            new ImageInspector(properties)
        );
        store.initialize();
        return store;
    }

    private static Stream<Arguments> supportedImages() throws IOException {
        return Stream.of(
            Arguments.of("PNG", png(), "image/png", ".png"),
            Arguments.of("JPEG", image("jpeg"), "image/jpeg", ".jpg"),
            Arguments.of("WebP", validWebp(), "image/webp", ".webp")
        );
    }

    private static byte[] png() throws IOException {
        return image("png");
    }

    private static byte[] image(String format) throws IOException {
        BufferedImage image = new BufferedImage(
            2,
            1,
            BufferedImage.TYPE_INT_RGB
        );
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, format, output)) {
                throw new IllegalStateException("No writer for " + format);
            }
            return output.toByteArray();
        }
    }

    private static String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(content)
        );
    }

    private static byte[] validWebp() {
        return Base64.getDecoder().decode(
            "UklGRjIAAABXRUJQVlA4ICYAAACQAQCdASoCAAEAAgA0JYgCdLoAA5gA/vd+L8UApHDnfm+x7McAAA=="
        );
    }

    private static byte[] webpExtendedHeader(int width, int height) {
        return webpExtendedHeader(width, height, 0);
    }

    private static byte[] webpExtendedHeader(
        int width,
        int height,
        int flags
    ) {
        byte[] bytes = new byte[30];
        System.arraycopy(ascii("RIFF"), 0, bytes, 0, 4);
        writeLittleEndian(bytes, 4, 22, 4);
        System.arraycopy(ascii("WEBP"), 0, bytes, 8, 4);
        System.arraycopy(ascii("VP8X"), 0, bytes, 12, 4);
        writeLittleEndian(bytes, 16, 10, 4);
        bytes[20] = (byte) flags;
        writeLittleEndian(bytes, 24, width - 1, 3);
        writeLittleEndian(bytes, 27, height - 1, 3);
        return bytes;
    }

    private static void writeLittleEndian(
        byte[] bytes,
        int offset,
        int value,
        int size
    ) {
        for (int index = 0; index < size; index++) {
            bytes[offset + index] = (byte) (value >>> (index * 8));
        }
    }

    private static byte[] pngHeader(int width, int height) throws IOException {
        byte[] type = ascii("IHDR");
        ByteArrayOutputStream dataBytes = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(dataBytes)) {
            data.writeInt(width);
            data.writeInt(height);
            data.writeByte(8);
            data.writeByte(2);
            data.writeByte(0);
            data.writeByte(0);
            data.writeByte(0);
        }
        byte[] header = dataBytes.toByteArray();
        CRC32 crc = new CRC32();
        crc.update(type);
        crc.update(header);

        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
        try (DataOutputStream image = new DataOutputStream(imageBytes)) {
            image.write(new byte[] {
                (byte) 0x89,
                0x50,
                0x4e,
                0x47,
                0x0d,
                0x0a,
                0x1a,
                0x0a,
            });
            image.writeInt(header.length);
            image.write(type);
            image.write(header);
            image.writeInt((int) crc.getValue());
        }
        return imageBytes.toByteArray();
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private static final class FailingInputStream extends InputStream {

        private final byte[] content;
        private final int failAfter;
        private int position;

        private FailingInputStream(byte[] content, int failAfter) {
            this.content = content;
            this.failAfter = failAfter;
        }

        @Override
        public int read() throws IOException {
            if (position >= failAfter) throw failure();
            return content[position++] & 0xff;
        }

        @Override
        public int read(byte[] target, int offset, int length) throws IOException {
            if (position >= failAfter) throw failure();
            int read = Math.min(length, failAfter - position);
            System.arraycopy(content, position, target, offset, read);
            position += read;
            return read;
        }

        private IOException failure() {
            return new IOException("simulated upload failure");
        }
    }
}
