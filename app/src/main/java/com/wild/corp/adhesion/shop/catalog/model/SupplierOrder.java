package com.wild.corp.adhesion.shop.catalog.model;

import com.wild.corp.adhesion.shop.common.persistence.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "shop_supplier_orders")
public class SupplierOrder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_name", nullable = false, length = 200)
    private String supplierName;

    @Column(name = "supplier_reference", length = 100)
    private String reference;

    @Column(name = "invoice_reference", length = 100)
    private String invoiceReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupplierOrderStatus status = SupplierOrderStatus.ORDERED;

    @Column(name = "received_at")
    private Instant receivedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SupplierOrderLine> lines = new ArrayList<>();

    protected SupplierOrder() { }

    public SupplierOrder(String supplierName, String reference) {
        this(supplierName, reference, false);
    }

    public SupplierOrder(String supplierName, String reference, boolean draft) {
        if (supplierName == null || supplierName.isBlank() || supplierName.trim().length() > 200) {
            throw new IllegalArgumentException("Le nom du fournisseur est obligatoire (200 caractères maximum)");
        }
        if (reference != null && reference.trim().length() > 100) {
            throw new IllegalArgumentException("La référence fournisseur est trop longue");
        }
        this.supplierName = supplierName.trim();
        this.reference = reference == null || reference.isBlank() ? null : reference.trim();
        this.status = draft ? SupplierOrderStatus.DRAFT : SupplierOrderStatus.ORDERED;
    }

    public SupplierOrderLine addLine(ProductVariant variant, int quantity) {
        if (status == SupplierOrderStatus.RECEIVED) {
            throw new IllegalStateException("Cette commande fournisseur a déjà été réceptionnée");
        }
        SupplierOrderLine line = new SupplierOrderLine(this, variant, quantity);
        lines.add(line);
        return line;
    }

    public void replaceDraft(String supplierName, String reference) {
        requireDraft();
        if (supplierName == null || supplierName.isBlank() || supplierName.trim().length() > 200) {
            throw new IllegalArgumentException("Le nom du fournisseur est obligatoire (200 caractères maximum)");
        }
        if (reference != null && reference.trim().length() > 100) {
            throw new IllegalArgumentException("La référence fournisseur est trop longue");
        }
        this.supplierName = supplierName.trim();
        this.reference = reference == null || reference.isBlank() ? null : reference.trim();
        lines.clear();
    }

    public void markOrdered() {
        requireDraft();
        if (lines.isEmpty()) throw new IllegalStateException("Le brouillon est vide");
        status = SupplierOrderStatus.ORDERED;
    }

    private void requireDraft() {
        if (status != SupplierOrderStatus.DRAFT) {
            throw new IllegalStateException("Seul un brouillon peut être modifié ou passé au fournisseur");
        }
    }

    public void markReceived() {
        if (status != SupplierOrderStatus.ORDERED) {
            throw new IllegalStateException("Cette commande fournisseur a déjà été réceptionnée");
        }
        status = SupplierOrderStatus.RECEIVED;
        receivedAt = Instant.now();
    }

    public void recordInvoice(String invoiceReference) {
        if (status != SupplierOrderStatus.ORDERED) {
            throw new IllegalStateException("Seule une commande fournisseur en cours peut être complétée");
        }
        if (invoiceReference == null || invoiceReference.isBlank() || invoiceReference.trim().length() > 100) {
            throw new IllegalArgumentException("La référence de facture est obligatoire (100 caractères maximum)");
        }
        this.invoiceReference = invoiceReference.trim();
    }

    public Long getId() { return id; }
    public String getSupplierName() { return supplierName; }
    public String getReference() { return reference; }
    public String getInvoiceReference() { return invoiceReference; }
    public SupplierOrderStatus getStatus() { return status; }
    public Instant getReceivedAt() { return receivedAt; }
    public List<SupplierOrderLine> getLines() { return Collections.unmodifiableList(lines); }
}
