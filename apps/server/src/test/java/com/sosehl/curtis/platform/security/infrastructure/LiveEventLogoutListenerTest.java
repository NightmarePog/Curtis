package com.sosehl.curtis.platform.security.infrastructure;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sosehl.curtis.platform.realtime.application.LiveEventRegistry;
import com.sosehl.curtis.platform.security.application.CurrentUserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class LiveEventLogoutListenerTest {

    @Mock
    private CurrentUserService currentUsers;

    @Mock
    private LiveEventRegistry events;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LiveEventLogoutListener listener;

    @Test
    void logoutCompletesEveryStreamForTheAccount() {
        String identityKey = "issuer\nsubject";
        when(currentUsers.findIdentityKey(authentication))
            .thenReturn(Optional.of(identityKey));

        listener.disconnectLiveEvents(new LogoutSuccessEvent(authentication));

        verify(events).disconnectIdentity(identityKey);
    }
}
