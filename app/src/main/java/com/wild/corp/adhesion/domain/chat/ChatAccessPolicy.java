package com.wild.corp.adhesion.domain.chat;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Chat;
import com.wild.corp.adhesion.models.ChatPermission;
import com.wild.corp.adhesion.models.User;

import java.util.List;

/** CHAT-001..003: decisions only; callers must load current links for every request. */
public final class ChatAccessPolicy {
    private ChatAccessPolicy() {
    }

    public static boolean canRead(User user, Chat chat, List<Activite> linkedActivities) {
        return chat.getPermissions().stream().anyMatch(permission -> hasRole(user, permission))
                || switch (chat.getCible()) {
                    case SECTION -> chat.getSection().getReferents().stream()
                            .anyMatch(ref -> user.getId().equals(ref.getId()))
                            || linkedActivities.stream().map(Activite::getSection)
                            .anyMatch(section -> section != null && chat.getSection().getId().equals(section.getId()));
                    case ACTIVITE -> linkedActivities.stream()
                            .anyMatch(activity -> chat.getActivite().getId().equals(activity.getId()));
                    case ASSOCIATION -> false;
                };
    }

    public static boolean canWrite(User user, Chat chat) {
        return chat.getPermissions().stream()
                .anyMatch(permission -> permission.isEcriture() && hasRole(user, permission));
    }

    private static boolean hasRole(User user, ChatPermission permission) {
        return user.getRoles().stream().anyMatch(role -> role.name().equals(permission.getRole()));
    }
}
