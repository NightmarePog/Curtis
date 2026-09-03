package com.sosehl.curtis.feature.media;

import com.sosehl.curtis.feature.media.MediaFileStore.StoredFile;
import com.sosehl.curtis.feature.media.core.MediaAccessContributor;
import com.sosehl.curtis.feature.media.core.MediaUsagePolicy;
import com.sosehl.curtis.feature.media.core.MediaWriter;
import com.sosehl.curtis.platform.persistence.SOSE_ReadOnlyTransaction;
import com.sosehl.curtis.shared.errors.ProblemException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@SOSE_ReadOnlyTransaction
public class MediaService implements MediaUsagePolicy, MediaWriter {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    private final MediaRepository repository;
    private final MediaFileStore fileStore;
    private final List<MediaAccessContributor> accessContributors;
    private final Clock clock;

    MediaService(
        MediaRepository repository,
        MediaFileStore fileStore,
        List<MediaAccessContributor> accessContributors,
        Clock clock
    ) {
        this.repository = repository;
        this.fileStore = fileStore;
        this.accessContributors = List.copyOf(accessContributors);
        this.clock = clock;
    }

    @Transactional(rollbackFor = IOException.class)
    public StoredMedia upload(
        UUID ownerId,
        String originalName,
        InputStream content
    ) throws IOException {
        return withContent(storeNew(ownerId, originalName, content));
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
    public UUID storeForImport(
        UUID ownerId,
        String originalName,
        InputStream content
    ) throws IOException {
        return storeNew(ownerId, originalName, content).getId();
    }

    public StoredMedia requireReadable(
        UUID userId,
        boolean administrator,
        UUID mediaId
    ) {
        MediaEntity media = requireEntity(mediaId);
        if (!canRead(media, userId, administrator)) throw notFound();
        return withContent(media);
    }

    @Override
    public void requireUsable(UUID mediaId, UUID ownerId, boolean administrator) {
        MediaEntity media = requireEntity(mediaId);
        if (!administrator && !media.getOwnerId().equals(ownerId)) {
            throw notFound();
        }
    }

    private MediaEntity storeNew(
        UUID ownerId,
        String originalName,
        InputStream content
    ) throws IOException {
        UUID mediaId = UUID.randomUUID();
        StoredFile storedFile = fileStore.store(
            mediaId,
            originalName,
            content
        );

        try {
            deleteFileAfterRollback(storedFile.storageKey());
            return repository.saveAndFlush(
                new MediaEntity(
                    mediaId,
                    ownerId,
                    storedFile.storageKey(),
                    storedFile.originalName(),
                    storedFile.contentType(),
                    storedFile.byteSize(),
                    storedFile.sha256(),
                    clock.instant()
                )
            );
        } catch (RuntimeException exception) {
            deleteFileOrSuppress(storedFile.storageKey(), exception);
            throw exception;
        }
    }

    private boolean canRead(
        MediaEntity media,
        UUID userId,
        boolean administrator
    ) {
        if (administrator || media.getOwnerId().equals(userId)) return true;
        return accessContributors.stream()
            .anyMatch(contributor -> contributor.canRead(userId, media.getId()));
    }

    private MediaEntity requireEntity(UUID mediaId) {
        return repository.findById(mediaId).orElseThrow(MediaService::notFound);
    }

    private StoredMedia withContent(MediaEntity media) {
        Resource content = fileStore.find(media.getStorageKey())
            .orElseThrow(MediaService::notFound);
        return new StoredMedia(
            media.getId(),
            media.getOriginalName(),
            media.getContentType(),
            media.getByteSize(),
            media.getSha256(),
            content
        );
    }

    private void deleteFileAfterRollback(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_ROLLED_BACK) return;
                    try {
                        fileStore.delete(storageKey);
                    } catch (IOException exception) {
                        log.warn(
                            "Could not remove rolled-back media file {}",
                            storageKey,
                            exception
                        );
                    }
                }
            }
        );
    }

    private void deleteFileOrSuppress(
        String storageKey,
        RuntimeException originalFailure
    ) {
        try {
            fileStore.delete(storageKey);
        } catch (IOException cleanupFailure) {
            originalFailure.addSuppressed(cleanupFailure);
        }
    }

    private static ProblemException notFound() {
        return ProblemException.notFound("media_not_found", "Media not found.");
    }
}
