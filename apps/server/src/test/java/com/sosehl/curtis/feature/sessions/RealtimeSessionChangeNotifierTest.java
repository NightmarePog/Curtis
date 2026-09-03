package com.sosehl.curtis.feature.sessions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.feature.users.core.UserRole;
import com.sosehl.curtis.platform.realtime.application.LiveEventPublisher;
import com.sosehl.curtis.platform.realtime.domain.LiveEventType;
import com.sosehl.curtis.platform.realtime.domain.LiveInvalidation;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RealtimeSessionChangeNotifierTest {

    @Mock
    private LiveEventPublisher events;

    @Mock
    private UserDirectory users;

    @Test
    void resultInvalidationTargetsOwnersAndActiveAdministrators() {
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID administratorId = UUID.randomUUID();
        when(users.activeIdsWithRole(UserRole.ADMINISTRATOR))
            .thenReturn(Set.of(administratorId));
        RealtimeSessionChangeNotifier notifier =
            new RealtimeSessionChangeNotifier(events, users);

        notifier.resultsChanged(studentId, teacherId);

        ArgumentCaptor<LiveInvalidation> event = ArgumentCaptor.forClass(
            LiveInvalidation.class
        );
        verify(events).publish(event.capture());
        assertThat(event.getValue().type()).isEqualTo(
            LiveEventType.RESULTS_CHANGED
        );
        assertThat(event.getValue().broadcast()).isFalse();
        assertThat(event.getValue().recipients())
            .containsExactlyInAnyOrder(
                studentId,
                teacherId,
                administratorId
            );
    }
}
