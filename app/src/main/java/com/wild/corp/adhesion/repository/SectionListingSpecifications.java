package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.Activite;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

/** Restricts read-only lists to the sections assigned to the requesting account. */
public final class SectionListingSpecifications {
    private SectionListingSpecifications() { }

    public static Specification<Adhesion> adhesions(Set<Long> sectionIds) {
        return (root, query, builder) -> root.get("activite").get("section").get("id").in(sectionIds);
    }

    public static Specification<Activite> activites(Set<Long> sectionIds) {
        return (root, query, builder) -> root.get("section").get("id").in(sectionIds);
    }

    public static Specification<Adherent> membresDesTribus(Set<Long> sectionIds) {
        return (root, query, builder) -> {
            var membership = query.subquery(Long.class);
            var adhesion = membership.from(Adhesion.class);
            membership.select(adhesion.get("id"));
            membership.where(
                    builder.equal(adhesion.get("adherent").get("tribu"), root.get("tribu")),
                    builder.isNotNull(root.get("tribu")),
                    adhesion.get("activite").get("section").get("id").in(sectionIds));
            return builder.exists(membership);
        };
    }
}
