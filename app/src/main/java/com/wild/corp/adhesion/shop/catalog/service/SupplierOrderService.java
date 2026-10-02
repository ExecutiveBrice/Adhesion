package com.wild.corp.adhesion.shop.catalog.service;

import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import com.wild.corp.adhesion.shop.catalog.model.SupplierOrder;
import com.wild.corp.adhesion.shop.catalog.model.SupplierOrderStatus;
import com.wild.corp.adhesion.shop.catalog.repository.ProductVariantRepository;
import com.wild.corp.adhesion.shop.catalog.repository.SupplierOrderRepository;
import com.wild.corp.adhesion.shop.order.model.OrderItemStatus;
import com.wild.corp.adhesion.shop.order.model.OrderStatus;
import com.wild.corp.adhesion.shop.order.repository.ShopOrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@Transactional
public class SupplierOrderService {

    private final SupplierOrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final ShopOrderRepository customerOrderRepository;

    public SupplierOrderService(SupplierOrderRepository orderRepository, ProductVariantRepository variantRepository,
                                ShopOrderRepository customerOrderRepository) {
        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
        this.customerOrderRepository = customerOrderRepository;
    }

    public List<SupplierOrder> findAll() {
        return orderRepository.findAllByOrderByCreatedAtDescIdDesc();
    }

    public SupplierOrder create(String supplierName, String reference, List<Line> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Une commande fournisseur doit contenir au moins une variante");
        }
        Set<Long> seen = new HashSet<>();
        for (Line line : lines) {
            if (line == null || line.variantId() == null || line.quantity() <= 0) {
                throw new IllegalArgumentException("Chaque ligne doit contenir une variante et une quantité positive");
            }
            if (!seen.add(line.variantId())) {
                throw new IllegalArgumentException("Une variante ne peut figurer qu’une fois dans la commande fournisseur");
            }
        }
        SupplierOrder order = new SupplierOrder(supplierName, reference);
        lines.stream().sorted(Comparator.comparing(Line::variantId)).forEach(line -> {
            ProductVariant variant = variantRepository.findByIdForUpdate(line.variantId())
                    .orElseThrow(() -> new NoSuchElementException("Variante introuvable : " + line.variantId()));
            if (!variant.isStockTracked()) {
                throw new IllegalArgumentException("Le stock de la variante " + line.variantId() + " n’est pas suivi");
            }
            order.addLine(variant, line.quantity()).setPurchaseDetails(line.unitCostAmountInCents(),
                    line.unitCostAmountInCents() == null ? null : "EUR", null, false);
        });
        return orderRepository.save(order);
    }

    public SupplierOrder createDraft(String supplierName, String reference, List<Line> lines) {
        SupplierOrder order = new SupplierOrder(supplierName, reference, true);
        fillDraft(order, lines);
        return orderRepository.save(order);
    }

    public SupplierOrder updateDraft(Long orderId, String supplierName, String reference, List<Line> lines) {
        SupplierOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NoSuchElementException("Commande fournisseur introuvable"));
        order.replaceDraft(supplierName, reference);
        orderRepository.flush();
        fillDraft(order, lines);
        return order;
    }

    public SupplierOrder placeDraft(Long orderId) {
        SupplierOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NoSuchElementException("Commande fournisseur introuvable"));
        if (order.getStatus() != SupplierOrderStatus.DRAFT) {
            throw new IllegalStateException("Seul un brouillon peut être passé au fournisseur");
        }
        var lines = order.getLines().stream().sorted(Comparator.comparing(line -> line.getVariantId())).toList();
        var lockedVariants = new java.util.HashMap<Long, ProductVariant>();
        for (var line : lines) {
            ProductVariant variant = variantRepository.findByIdForUpdate(line.getVariantId())
                    .orElseThrow(() -> new NoSuchElementException("Variante introuvable : " + line.getVariantId()));
            lockedVariants.put(line.getVariantId(), variant);
        }
        var incoming = outstandingSupplierQuantities();
        var demand = openCustomerDemand();
        for (var line : lines) {
            int currentNeed = need(lockedVariants.get(line.getVariantId()),
                    demand.getOrDefault(line.getVariantId(), 0L), incoming.getOrDefault(line.getVariantId(), 0L));
            if (line.getExpectedNeed() == null || currentNeed != line.getExpectedNeed()) {
                throw new IllegalStateException("Les besoins ont changé depuis l'enregistrement du brouillon. Actualisez-le avant de passer commande.");
            }
            if (line.getUnitCostAmountInCents() == null) {
                throw new IllegalStateException("Renseignez le prix d'achat de chaque article avant de passer commande");
            }
            if (line.getQuantity() > currentNeed && !line.isExtraApproved()) {
                throw new IllegalStateException("Une quantité dépasse le besoin actuel. Confirmez l'achat supplémentaire dans le brouillon.");
            }
        }
        order.markOrdered();
        return order;
    }

    private void fillDraft(SupplierOrder order, List<Line> lines) {
        validateLines(lines);
        var variants = new java.util.HashMap<Long, ProductVariant>();
        lines.stream().sorted(Comparator.comparing(Line::variantId)).forEach(line -> {
            ProductVariant variant = variantRepository.findByIdForUpdate(line.variantId())
                    .orElseThrow(() -> new NoSuchElementException("Variante introuvable : " + line.variantId()));
            if (!variant.isStockTracked()) {
                throw new IllegalArgumentException("Le stock de la variante " + line.variantId() + " n’est pas suivi");
            }
            variants.put(line.variantId(), variant);
        });
        var incoming = outstandingSupplierQuantities();
        var demand = openCustomerDemand();
        lines.stream().sorted(Comparator.comparing(Line::variantId)).forEach(line -> {
            ProductVariant variant = variants.get(line.variantId());
            order.addLine(variant, line.quantity()).setPurchaseDetails(line.unitCostAmountInCents(),
                    line.unitCostAmountInCents() == null ? null : "EUR",
                    need(variant, demand.getOrDefault(line.variantId(), 0L),
                            incoming.getOrDefault(line.variantId(), 0L)), line.extraApproved());
        });
    }

    private static int need(ProductVariant variant, long demand, long incoming) {
        if (!variant.isStockTracked() || variant.getStockOnHand() == null) {
            throw new IllegalStateException("Le stock de la variante n'est plus suivi");
        }
        // Le stock comptable actuel inclut déjà les déductions des commandes payées.
        long presentForPaid = Math.max(0, Math.subtractExact(
                Math.addExact(variant.getStockOnHand(), demand), variant.getStockReserved()));
        long shortage = Math.max(0, Math.subtractExact(Math.subtractExact(demand, presentForPaid), incoming));
        return Math.toIntExact(shortage);
    }

    private java.util.Map<Long, Long> openCustomerDemand() {
        var demand = new java.util.HashMap<Long, Long>();
        for (var order : customerOrderRepository.findAllByOrderByCreatedAtDescIdDesc()) {
            if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PROCESSING) continue;
            order.getItems().stream()
                    .filter(item -> item.getProductVariantId() != null)
                    .filter(item -> item.getStatus() != OrderItemStatus.COMPLETED
                            && item.getStatus() != OrderItemStatus.CANCELLED)
                    .forEach(item -> demand.merge(item.getProductVariantId(), (long) item.getQuantity(), Math::addExact));
        }
        return demand;
    }

    private java.util.Map<Long, Long> outstandingSupplierQuantities() {
        var incoming = new java.util.HashMap<Long, Long>();
        for (SupplierOrder supplierOrder : orderRepository.findAllByOrderByCreatedAtDescIdDesc()) {
            if (supplierOrder.getStatus() != SupplierOrderStatus.ORDERED) continue;
            supplierOrder.getLines().forEach(line -> incoming.merge(line.getVariantId(), (long) line.getQuantity(), Math::addExact));
        }
        return incoming;
    }

    private static void validateLines(List<Line> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Une commande fournisseur doit contenir au moins une variante");
        }
        Set<Long> seen = new HashSet<>();
        for (Line line : lines) {
            if (line == null || line.variantId() == null || line.quantity() <= 0 || !seen.add(line.variantId())) {
                throw new IllegalArgumentException("Chaque variante doit figurer une seule fois avec une quantité positive");
            }
            if (line.unitCostAmountInCents() != null && line.unitCostAmountInCents() < 0) {
                throw new IllegalArgumentException("Le prix d'achat ne peut pas être négatif");
            }
        }
    }

    public SupplierOrder receive(Long orderId) {
        SupplierOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NoSuchElementException("Commande fournisseur introuvable"));
        if (order.getStatus() != com.wild.corp.adhesion.shop.catalog.model.SupplierOrderStatus.ORDERED) {
            throw new IllegalStateException("Cette commande fournisseur a déjà été réceptionnée");
        }
        order.getLines().stream().sorted(Comparator.comparing(line -> line.getVariantId())).forEach(line -> {
            ProductVariant variant = variantRepository.findByIdForUpdate(line.getVariantId())
                    .orElseThrow(() -> new NoSuchElementException("Variante introuvable : " + line.getVariantId()));
            variant.receiveStock(line.getQuantity());
        });
        order.markReceived();
        return order;
    }

    public record Line(Long variantId, int quantity, Long unitCostAmountInCents, boolean extraApproved) {
        public Line(Long variantId, int quantity) { this(variantId, quantity, null, false); }
    }
}
