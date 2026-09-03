package com.sosehl.curtis.feature.users;

import com.sosehl.curtis.feature.users.core.UserRole;
import com.sosehl.curtis.feature.users.dto.UserDtos.UpdateUserRequest;
import com.sosehl.curtis.feature.users.dto.UserDtos.UserResponse;
import com.sosehl.curtis.platform.security.web.SOSE_AdminApiController;
import com.sosehl.curtis.platform.security.web.SOSE_CurrentUser;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@SOSE_AdminApiController
@RequestMapping("/v1/admin/users")
public class AdminUserController {

    private final UserService userDirectory;

    public AdminUserController(UserService userDirectory) {
        this.userDirectory = userDirectory;
    }

    @GetMapping
    List<UserResponse> list(
        @RequestParam(required = false) UserRole role,
        @RequestParam(required = false) Boolean active,
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        return userDirectory.list(role, active).stream().map(this::response).toList();
    }

    @PatchMapping("/{userId}")
    UserResponse update(
        @PathVariable UUID userId,
        @Valid @RequestBody UpdateUserRequest request,
        @SOSE_CurrentUser CurrentUser actor
    ) {
        return response(
            userDirectory.setActive(
                actor.id(),
                userId,
                request.active(),
                request.version()
            )
        );
    }

    private UserResponse response(
        UserAccountSnapshot account
    ) {
        return new UserResponse(
            account.id(),
            account.username(),
            account.displayName(),
            account.roles(),
            account.active(),
            account.firstLoginAt(),
            account.lastLoginAt(),
            account.version()
        );
    }

}
