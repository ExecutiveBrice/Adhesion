package com.wild.corp.adhesion.models.resources;

import java.util.List;

public record SectionChatConfiguration(Long id, String nom, String cible, Long cibleId, String cibleNom,
                                       List<RolePermission> permissions) {
    public record RolePermission(String role, boolean ecriture) {
    }
}
