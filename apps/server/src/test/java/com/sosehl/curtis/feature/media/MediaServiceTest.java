package com.sosehl.curtis.feature.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sosehl.curtis.feature.media.MediaFileStore.StoredFile;
import com.sosehl.curtis.feature.media.core.MediaAccessContributor;
import com.sosehl.curtis.shared.errors.ProblemException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    private static final UUID OWNER_ID = UUID.fromString(
        "10000000-0000-0000-0000-000000000001"
    );
    private static final UUID USER_ID = UUID.fromString(
        "10000000-0000-0000-0000-000000000002"
    );
    private static final UUID MEDIA_ID = UUID.fromString(
        "20000000-0000-0000-0000-000000000001"
    );
    private static final String STORAGE_KEY = MEDIA_ID + ".png";
    private static final Instant CREATED_AT = Instant.parse(
        "2026-09-02T12:00:00Z"
    );
    private static final Resource CONTENT = new ByteArrayResource(
        new byte[] { 1, 2, 3 }
    );

    @Mock
    private MediaRepository repository;

    @Mock
    private MediaFileStore fileStore;

    @Mock
    private MediaAccessContributor accessContributor;

    private MediaService service;

    @BeforeEach
    void setUp() {
        service = new MediaService(
            repository,
            fileStore,
            List.of(accessContributor),
            Clock.fixed(CREATED_AT, ZoneOffset.UTC)
        );
    }

    @Test
    void uploadPersistsStoredMetadataAndReturnsItsContent() throws Exception {
        InputStream input = new ByteArrayInputStream(new byte[] { 9, 8, 7 });
        when(fileStore.store(any(UUID.class), eq("diagram.png"), same(input)))
            .thenReturn(storedFile());
        when(repository.saveAndFlush(any(MediaEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStore.find(STORAGE_KEY)).thenReturn(Optional.of(CONTENT));

        StoredMedia result = service.upload(OWNER_ID, "diagram.png", input);

        ArgumentCaptor<UUID> generatedId = ArgumentCaptor.forClass(UUID.class);
        verify(fileStore).store(
            generatedId.capture(),
            eq("diagram.png"),
            same(input)
        );
        ArgumentCaptor<MediaEntity> persisted = ArgumentCaptor.forClass(
            MediaEntity.class
        );
        verify(repository).saveAndFlush(persisted.capture());

        MediaEntity entity = persisted.getValue();
        assertThat(entity.getId()).isEqualTo(generatedId.getValue());
        assertThat(entity.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(entity.getStorageKey()).isEqualTo(STORAGE_KEY);
        assertThat(entity.getOriginalName()).isEqualTo("diagram.png");
        assertThat(entity.getContentType()).isEqualTo("image/png");
        assertThat(entity.getByteSize()).isEqualTo(3);
        assertThat(entity.getSha256()).isEqualTo("abc123");
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);

        assertThat(result.id()).isEqualTo(generatedId.getValue());
        assertThat(result.originalName()).isEqualTo("diagram.png");
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.byteSize()).isEqualTo(3);
        assertThat(result.sha256()).isEqualTo("abc123");
        assertThat(result.content()).isSameAs(CONTENT);
    }

    @Test
    void importPersistsMetadataWithoutOpeningTheStoredContent() throws Exception {
        InputStream input = new ByteArrayInputStream(new byte[] { 4, 5, 6 });
        when(fileStore.store(any(UUID.class), eq("import.png"), same(input)))
            .thenReturn(storedFile());
        when(repository.saveAndFlush(any(MediaEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UUID result = service.storeForImport(OWNER_ID, "import.png", input);

        ArgumentCaptor<MediaEntity> persisted = ArgumentCaptor.forClass(
            MediaEntity.class
        );
        verify(repository).saveAndFlush(persisted.capture());
        assertThat(result).isEqualTo(persisted.getValue().getId());
        verify(fileStore, never()).find(anyString());
    }

    @Test
    void repositoryFailureDeletesTheStoredFile() throws Exception {
        RuntimeException persistenceFailure = new IllegalStateException(
            "database unavailable"
        );
        when(fileStore.store(any(UUID.class), anyString(), any(InputStream.class)))
            .thenReturn(storedFile());
        when(repository.saveAndFlush(any(MediaEntity.class)))
            .thenThrow(persistenceFailure);

        assertThatThrownBy(() -> service.upload(
            OWNER_ID,
            "diagram.png",
            new ByteArrayInputStream(new byte[] { 1 })
        )).isSameAs(persistenceFailure);

        verify(fileStore).delete(STORAGE_KEY);
    }

    @Test
    void cleanupFailureIsSuppressedOnTheRepositoryFailure() throws Exception {
        RuntimeException persistenceFailure = new IllegalStateException(
            "database unavailable"
        );
        IOException cleanupFailure = new IOException("disk unavailable");
        when(fileStore.store(any(UUID.class), anyString(), any(InputStream.class)))
            .thenReturn(storedFile());
        when(repository.saveAndFlush(any(MediaEntity.class)))
            .thenThrow(persistenceFailure);
        doThrow(cleanupFailure).when(fileStore).delete(STORAGE_KEY);

        assertThatThrownBy(() -> service.upload(
            OWNER_ID,
            "diagram.png",
            new ByteArrayInputStream(new byte[] { 1 })
        ))
            .isSameAs(persistenceFailure)
            .satisfies(exception ->
                assertThat(exception.getSuppressed())
                    .containsExactly(cleanupFailure)
            );
    }

    @Test
    void transactionRollbackDeletesTheStoredFile() throws Exception {
        initializeSuccessfulStore();
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.storeForImport(
                OWNER_ID,
                "diagram.png",
                new ByteArrayInputStream(new byte[] { 1 })
            );

            TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization ->
                    synchronization.afterCompletion(
                        TransactionSynchronization.STATUS_ROLLED_BACK
                    )
                );

            verify(fileStore).delete(STORAGE_KEY);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void transactionCommitKeepsTheStoredFile() throws Exception {
        initializeSuccessfulStore();
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.storeForImport(
                OWNER_ID,
                "diagram.png",
                new ByteArrayInputStream(new byte[] { 1 })
            );

            TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization ->
                    synchronization.afterCompletion(
                        TransactionSynchronization.STATUS_COMMITTED
                    )
                );

            verify(fileStore, never()).delete(anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void ownerAndAdministratorReadWithoutConsultingContributors() {
        when(repository.findById(MEDIA_ID)).thenReturn(Optional.of(media()));
        when(fileStore.find(STORAGE_KEY)).thenReturn(Optional.of(CONTENT));

        assertThat(service.requireReadable(OWNER_ID, false, MEDIA_ID).content())
            .isSameAs(CONTENT);
        assertThat(service.requireReadable(USER_ID, true, MEDIA_ID).content())
            .isSameAs(CONTENT);

        verifyNoInteractions(accessContributor);
    }

    @Test
    void contributorCanGrantReadAccess() {
        when(repository.findById(MEDIA_ID)).thenReturn(Optional.of(media()));
        when(accessContributor.canRead(USER_ID, MEDIA_ID)).thenReturn(true);
        when(fileStore.find(STORAGE_KEY)).thenReturn(Optional.of(CONTENT));

        StoredMedia result = service.requireReadable(USER_ID, false, MEDIA_ID);

        assertThat(result.content()).isSameAs(CONTENT);
        verify(accessContributor).canRead(USER_ID, MEDIA_ID);
    }

    @Test
    void deniedReadIsHiddenAsNotFoundWithoutOpeningContent() {
        when(repository.findById(MEDIA_ID)).thenReturn(Optional.of(media()));

        assertMediaNotFound(() ->
            service.requireReadable(USER_ID, false, MEDIA_ID)
        );

        verify(accessContributor).canRead(USER_ID, MEDIA_ID);
        verify(fileStore, never()).find(anyString());
    }

    @Test
    void missingContentIsReportedAsNotFound() {
        when(repository.findById(MEDIA_ID)).thenReturn(Optional.of(media()));
        when(fileStore.find(STORAGE_KEY)).thenReturn(Optional.empty());

        assertMediaNotFound(() ->
            service.requireReadable(OWNER_ID, false, MEDIA_ID)
        );

        verifyNoInteractions(accessContributor);
    }

    @Test
    void missingMetadataIsReportedAsNotFound() {
        when(repository.findById(MEDIA_ID)).thenReturn(Optional.empty());

        assertMediaNotFound(() ->
            service.requireReadable(OWNER_ID, false, MEDIA_ID)
        );

        verifyNoInteractions(fileStore, accessContributor);
    }

    @Test
    void mediaIsUsableOnlyByItsOwnerOrAnAdministrator() {
        when(repository.findById(MEDIA_ID)).thenReturn(Optional.of(media()));

        service.requireUsable(MEDIA_ID, OWNER_ID, false);
        service.requireUsable(MEDIA_ID, USER_ID, true);
        assertMediaNotFound(() ->
            service.requireUsable(MEDIA_ID, USER_ID, false)
        );

        verifyNoInteractions(fileStore, accessContributor);
    }

    @Test
    void writeMethodsRollBackForIoFailures() throws Exception {
        assertRollsBackForIo(
            "upload",
            UUID.class,
            String.class,
            InputStream.class
        );
        assertRollsBackForIo(
            "storeForImport",
            UUID.class,
            String.class,
            InputStream.class
        );
    }

    private StoredFile storedFile() {
        return new StoredFile(
            STORAGE_KEY,
            "diagram.png",
            "image/png",
            3,
            "abc123"
        );
    }

    private void initializeSuccessfulStore() throws Exception {
        when(fileStore.store(any(UUID.class), anyString(), any(InputStream.class)))
            .thenReturn(storedFile());
        when(repository.saveAndFlush(any(MediaEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private MediaEntity media() {
        return new MediaEntity(
            MEDIA_ID,
            OWNER_ID,
            STORAGE_KEY,
            "diagram.png",
            "image/png",
            3,
            "abc123",
            CREATED_AT
        );
    }

    private void assertMediaNotFound(ThrowingCallable operation) {
        assertThatThrownBy(operation).isInstanceOfSatisfying(
            ProblemException.class,
            exception -> {
                assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(exception.code()).isEqualTo("media_not_found");
                assertThat(exception.getMessage()).isEqualTo("Media not found.");
            }
        );
    }

    private void assertRollsBackForIo(
        String methodName,
        Class<?>... parameterTypes
    ) throws Exception {
        Transactional annotation = AnnotatedElementUtils.findMergedAnnotation(
            MediaService.class.getDeclaredMethod(methodName, parameterTypes),
            Transactional.class
        );

        assertThat(annotation).isNotNull();
        assertThat(annotation.rollbackFor()).containsExactly(IOException.class);
    }
}
