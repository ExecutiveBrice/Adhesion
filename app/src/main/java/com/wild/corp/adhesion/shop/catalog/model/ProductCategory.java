package com.wild.corp.adhesion.shop.catalog.model;

import com.wild.corp.adhesion.shop.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.Objects;

@Entity
@Table(name = "shop_product_categories")
public class ProductCategory extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 180, unique = true)
    private String slug;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Version
    @Column(nullable = false)
    private long version;

    protected ProductCategory() {
        // Required by JPA.
    }

    public ProductCategory(String name, String slug, String description, boolean active, int displayOrder) {
        this.name = requireText(name, "Le nom de la catégorie est obligatoire");
        this.slug = requireText(slug, "Le slug de la catégorie est obligatoire");
        this.description = description;
        this.active = active;
        setDisplayOrder(displayOrder);
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public void updateDetails(String name, String slug, String description, boolean active, int displayOrder) {
        this.name = requireText(name, "Le nom de la catégorie est obligatoire");
        this.slug = requireText(slug, "Le slug de la catégorie est obligatoire");
        this.description = description;
        this.active = active;
        setDisplayOrder(displayOrder);
    }

    public void setDisplayOrder(int displayOrder) {
        if (displayOrder < 0) {
            throw new IllegalArgumentException("L'ordre d'affichage ne peut pas être négatif");
        }
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public long getVersion() {
        return version;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ProductCategory category)) {
            return false;
        }
        return id != null && Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
