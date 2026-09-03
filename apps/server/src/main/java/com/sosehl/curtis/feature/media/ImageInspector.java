package com.sosehl.curtis.feature.media;

import com.sosehl.curtis.shared.errors.ProblemException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;

/** Inspects untrusted image content through the installed ImageIO readers. */
@Component
final class ImageInspector {

    record ImageMetadata(String contentType, String extension) {}

    private enum Format {
        PNG("image/png", ".png"),
        JPEG("image/jpeg", ".jpg"),
        WEBP("image/webp", ".webp");

        private final String contentType;
        private final String extension;

        Format(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        private static Format from(ImageReader reader) throws IOException {
            String name = reader.getFormatName().toUpperCase(Locale.ROOT);
            return switch (name) {
                case "PNG" -> PNG;
                case "JPEG", "JPG" -> JPEG;
                case "WEBP" -> WEBP;
                default -> throw unsupported();
            };
        }
    }

    private static final int WEBP_ANIMATION_FLAG = 0x02;

    private final long maxPixels;

    ImageInspector(MediaProperties properties) {
        this.maxPixels = properties.maxPixels();
    }

    ImageMetadata inspect(Path image) {
        try (ImageInputStream input = ImageIO.createImageInputStream(image.toFile())) {
            if (input == null) throw invalid("Image data is invalid");

            ImageReader reader = requireReader(input);
            try {
                Format format = Format.from(reader);
                reader.setInput(input, true, true);
                validateDimensions(reader.getWidth(0), reader.getHeight(0));

                if (format == Format.WEBP) validateWebpContainer(image);
                BufferedImage decoded = reader.read(0);
                if (decoded == null) throw invalid("Image data is invalid");

                return new ImageMetadata(format.contentType, format.extension);
            } finally {
                reader.dispose();
            }
        } catch (ProblemException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw invalid("Image data is invalid");
        }
    }

    private ImageReader requireReader(ImageInputStream input) {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
        if (!readers.hasNext()) throw unsupported();
        return readers.next();
    }

    private void validateDimensions(int width, int height) {
        if (width < 1 || height < 1 || (long) width * height > maxPixels) {
            throw invalid("Image dimensions are too large");
        }
    }

    /**
     * ImageIO validates the pixels. This small container check rejects empty or
     * animated extended WebP containers that its current reader does not reject.
     */
    private void validateWebpContainer(Path image) throws IOException {
        long fileSize = Files.size(image);
        try (ImageInputStream input = ImageIO.createImageInputStream(image.toFile())) {
            if (input == null) throw invalid("Image data is invalid");
            input.setByteOrder(ByteOrder.LITTLE_ENDIAN);

            if (
                !"RIFF".equals(readFourCc(input)) ||
                Integer.toUnsignedLong(input.readInt()) + 8 != fileSize ||
                !"WEBP".equals(readFourCc(input))
            ) {
                throw invalid("Image data is invalid");
            }

            boolean hasImagePayload = false;
            while (input.getStreamPosition() < fileSize) {
                long headerStart = input.getStreamPosition();
                if (fileSize - headerStart < 8) {
                    throw invalid("Image data is invalid");
                }

                String chunkType = readFourCc(input);
                long chunkSize = Integer.toUnsignedLong(input.readInt());
                long dataStart = input.getStreamPosition();
                long dataEnd = dataStart + chunkSize;
                long paddedEnd = dataEnd + (chunkSize & 1L);
                if (paddedEnd > fileSize) {
                    throw invalid("Image data is invalid");
                }

                if ("VP8X".equals(chunkType)) {
                    if (
                        chunkSize != 10 ||
                        (input.readUnsignedByte() & WEBP_ANIMATION_FLAG) != 0
                    ) {
                        throw invalid("Animated WebP images are not supported");
                    }
                } else if ("ANIM".equals(chunkType) || "ANMF".equals(chunkType)) {
                    throw invalid("Animated WebP images are not supported");
                } else if (
                    "VP8 ".equals(chunkType) || "VP8L".equals(chunkType)
                ) {
                    hasImagePayload = true;
                }

                input.seek(paddedEnd);
            }

            if (!hasImagePayload) throw invalid("Image data is invalid");
        }
    }

    private String readFourCc(ImageInputStream input) throws IOException {
        byte[] bytes = new byte[4];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    private static ProblemException unsupported() {
        return invalid("Only decoded PNG, JPEG, and WebP images are accepted");
    }

    private static ProblemException invalid(String message) {
        return ProblemException.badRequest("media_invalid", message);
    }
}
