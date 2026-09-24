package com.wild.corp.adhesion.shop.catalog.model;

import com.wild.corp.adhesion.shop.common.persistence.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "shop_products")
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 220, unique = true)
    private String slug;

    @Column(length = 5000)
    private String description;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Version
    @Column(nullable = false)
    private long version;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "shop_product_category_links",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<ProductCategory> categories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<ProductVariant> variants = new ArrayList<>();

    protected Product() {
        // Required by JPA.
    }

    public Product(String name, String slug, String description, boolean active, int displayOrder) {
        this.name = requireText(name, "Le nom du produit est obligatoire");
        this.slug = requireText(slug, "Le slug du produit est obligatoire");
        this.description = description;
        this.active = active;
        setDisplayOrder(displayOrder);
    }

    public void addCategory(ProductCategory category) {
        categories.add(Objects.requireNonNull(category, "La catégorie est obligatoire"));
    }

    public void addVariant(ProductVariant variant) {
        Objects.requireNonNull(variant, "La variante est obligatoire");
        if (variant.getProduct() != this) {
            throw new IllegalArgumentException("La variante appartient à un autre produit");
        }
        variants.add(variant);
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public void rename(String name) {
        this.name = requireText(name, "Le nom du produit est obligatoire");
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

    public Set<ProductCategory> getCategories() {
        return Collections.unmodifiableSet(categories);
    }

    public List<ProductVariant> getVariants() {
        return Collections.unmodifiableList(variants);
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
        if (!(object instanceof Product product)) {
            return false;
        }
        return id != null && Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
