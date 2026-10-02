package com.wild.corp.adhesion.shop.api;

import com.wild.corp.adhesion.shop.api.dto.AdminCategoryRequest;
import com.wild.corp.adhesion.shop.api.dto.AdminCategoryResponse;
import com.wild.corp.adhesion.shop.api.dto.AdminOrderResponse;
import com.wild.corp.adhesion.shop.api.dto.AdminOrderItemStatusRequest;
import com.wild.corp.adhesion.shop.api.dto.AdminOrderStatusRequest;
import com.wild.corp.adhesion.shop.api.dto.AdminProductRequest;
import com.wild.corp.adhesion.shop.api.dto.AdminProductResponse;
import com.wild.corp.adhesion.shop.api.dto.AdminVariantCreateRequest;
import com.wild.corp.adhesion.shop.api.dto.AdminVariantRequest;
import com.wild.corp.adhesion.shop.api.dto.AdminVariantResponse;
import com.wild.corp.adhesion.shop.api.dto.AdminVariantStockRequest;
import com.wild.corp.adhesion.shop.api.dto.OrderMessageRequest;
import com.wild.corp.adhesion.shop.api.dto.OrderMessageResponse;
import com.wild.corp.adhesion.shop.api.dto.ProductImageUploadResponse;
import com.wild.corp.adhesion.shop.api.dto.SupplierOrderCreateRequest;
import com.wild.corp.adhesion.shop.api.dto.SupplierOrderResponse;
import com.wild.corp.adhesion.shop.catalog.model.SupplierOrder;
import com.wild.corp.adhesion.shop.catalog.service.CatalogService;
import com.wild.corp.adhesion.shop.catalog.service.ProductImageStorageService;
import com.wild.corp.adhesion.shop.catalog.service.SupplierOrderService;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.repository.UserRepository;
import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.order.service.OrderService;
import com.wild.corp.adhesion.shop.order.service.OrderConversationService;
import com.wild.corp.adhesion.shop.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/shop/admin")
@PreAuthorize("hasRole('RESPONSABLE_BOUTIQUE')")
@Slf4j
public class ShopAdminController {

    private final CatalogService catalogService;
    private final OrderService orderService;
    private final OrderConversationService orderConversationService;
    private final UserRepository userRepository;
    private final ProductImageStorageService productImageStorage;
    private final PaymentService paymentService;
    private final SupplierOrderService supplierOrderService;

    public ShopAdminController(CatalogService catalogService, OrderService orderService,
                               OrderConversationService orderConversationService, UserRepository userRepository,
                               ProductImageStorageService productImageStorage, PaymentService paymentService,
                               SupplierOrderService supplierOrderService) {
        this.catalogService = catalogService;
        this.orderService = orderService;
        this.orderConversationService = orderConversationService;
        this.userRepository = userRepository;
        this.productImageStorage = productImageStorage;
        this.paymentService = paymentService;
        this.supplierOrderService = supplierOrderService;
    }

    @GetMapping("/supplier-orders")
    public List<SupplierOrderResponse> supplierOrders() {
        return supplierOrderService.findAll().stream().map(ShopAdminController::supplierOrder).toList();
    }

    @PostMapping("/supplier-orders")
    public ResponseEntity<SupplierOrderResponse> createSupplierOrder(@Valid @RequestBody SupplierOrderCreateRequest request) {
        SupplierOrder order = supplierOrderService.create(request.supplierName(), request.reference(),
                supplierLines(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierOrder(order));
    }

    @PostMapping("/supplier-orders/draft")
    public ResponseEntity<SupplierOrderResponse> createSupplierOrderDraft(@Valid @RequestBody SupplierOrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierOrder(supplierOrderService.createDraft(
                request.supplierName(), request.reference(), supplierLines(request))));
    }

    @PutMapping("/supplier-orders/{orderId}/draft")
    public SupplierOrderResponse updateSupplierOrderDraft(@PathVariable Long orderId,
            @Valid @RequestBody SupplierOrderCreateRequest request) {
        return supplierOrder(supplierOrderService.updateDraft(orderId, request.supplierName(),
                request.reference(), supplierLines(request)));
    }

    @PutMapping("/supplier-orders/{orderId}/place")
    public SupplierOrderResponse placeSupplierOrderDraft(@PathVariable Long orderId) {
        return supplierOrder(supplierOrderService.placeDraft(orderId));
    }

    @PutMapping("/supplier-orders/{orderId}/receive")
    public SupplierOrderResponse receiveSupplierOrder(@PathVariable Long orderId) {
        return supplierOrder(supplierOrderService.receive(orderId));
    }

    @PostMapping("/orders/{orderNumber}/payment/verify")
    public AdminOrderResponse verifyPayment(@PathVariable String orderNumber) {
        var order = paymentService.verifyPaymentForManager(orderNumber);
        return ShopAdminMapper.order(order, userRepository.findAllByIdIn(List.of(order.getCustomerUserId()))
                .stream().findFirst().orElse(null));
    }

    @GetMapping("/orders")
    public List<AdminOrderResponse> orders() {
        List<com.wild.corp.adhesion.shop.order.model.ShopOrder> orders = orderService.findAllOrders();
        Map<Long, User> customersById = userRepository.findAllByIdIn(orders.stream()
                        .map(com.wild.corp.adhesion.shop.order.model.ShopOrder::getCustomerUserId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return orders.stream().map(order -> ShopAdminMapper.order(order, customersById.get(order.getCustomerUserId()))).toList();
    }

    @PutMapping("/orders/{orderNumber}/status")
    public AdminOrderResponse updateOrderStatus(@PathVariable String orderNumber,
                                                @Valid @RequestBody AdminOrderStatusRequest request) {
        var order = orderService.updateAdminOrderStatus(orderNumber, request.status());
        return ShopAdminMapper.order(order, userRepository.findAllByIdIn(List.of(order.getCustomerUserId())).stream()
                .findFirst().orElse(null));
    }

    @PutMapping("/orders/{orderNumber}/items/{itemId}/status")
    public AdminOrderResponse updateOrderItemStatus(@PathVariable String orderNumber, @PathVariable Long itemId,
                                                         @Valid @RequestBody AdminOrderItemStatusRequest request) {
        var order = orderService.updateItemStatus(orderNumber, itemId, request.status());
        return ShopAdminMapper.order(order, userRepository.findAllByIdIn(List.of(order.getCustomerUserId())).stream()
                .findFirst().orElse(null));
    }

    @GetMapping("/orders/{orderNumber}/conversation")
    public List<OrderMessageResponse> conversation(@PathVariable String orderNumber) {
        return orderConversationService.messagesForManager(orderNumber).stream().map(ShopApiMapper::orderMessage).toList();
    }

    @PostMapping("/orders/{orderNumber}/conversation")
    public ResponseEntity<OrderMessageResponse> sendConversationMessage(@PathVariable String orderNumber,
                                                                          @Valid @RequestBody OrderMessageRequest request,
                                                                          Authentication authentication) {
        Long managerUserId = authentication != null && authentication.getPrincipal() instanceof com.wild.corp.adhesion.models.UserDetails user
                ? user.getId() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(ShopApiMapper.orderMessage(
                orderConversationService.sendAsManager(orderNumber, managerUserId, request.content())));
    }

    @GetMapping("/products")
    public List<AdminProductResponse> products() {
        List<AdminProductResponse> products = catalogService.findAllProducts().stream().map(ShopAdminMapper::product).toList();
        log.info("Gestion boutique : {} produit(s) chargé(s)", products.size());
        return products;
    }

    @PostMapping(value = "/product-images", consumes = "multipart/form-data")
    public ProductImageUploadResponse uploadProductImage(@RequestParam("file") MultipartFile file) {
        return new ProductImageUploadResponse(productImageStorage.store(file));
    }

    @PostMapping("/products")
    public ResponseEntity<AdminProductResponse> createProduct(@Valid @RequestBody AdminProductRequest request) {
        var product = catalogService.createProduct(request.name(), request.slug(), request.description(), request.imageUrl(),
                request.active(), request.displayOrder(), request.categoryIds());
        return ResponseEntity.created(URI.create("/shop/admin/products/" + product.getId()))
                .body(ShopAdminMapper.product(product));
    }

    @PutMapping("/products/{productId}")
    public AdminProductResponse updateProduct(@PathVariable Long productId, @Valid @RequestBody AdminProductRequest request) {
        return ShopAdminMapper.product(catalogService.updateProduct(productId, request.name(), request.slug(),
                request.description(), request.imageUrl(), request.active(), request.displayOrder(), request.categoryIds()));
    }

    @DeleteMapping("/products/{productId}")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long productId) {
        catalogService.deleteProduct(productId);
    }

    @PostMapping("/products/{productId}/variants")
    public ResponseEntity<AdminVariantResponse> createVariant(@PathVariable Long productId,
                                                                @Valid @RequestBody AdminVariantCreateRequest request) {
        var variant = catalogService.createVariant(productId, request.sku(), request.label(),
                new Money(request.priceAmountInCents(), request.currency().toUpperCase()), request.active(), request.displayOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(ShopAdminMapper.variant(variant));
    }

    @PutMapping("/variants/{variantId}")
    public AdminVariantResponse updateVariant(@PathVariable Long variantId, @Valid @RequestBody AdminVariantRequest request) {
        return ShopAdminMapper.variant(catalogService.updateVariant(variantId, request.sku(), request.label(), money(request),
                request.active(), request.displayOrder(), request.stockTracked(), request.expectedVersion()));
    }

    @PutMapping("/variants/{variantId}/stock")
    public AdminVariantResponse updateVariantStock(@PathVariable Long variantId,
                                                   @Valid @RequestBody AdminVariantStockRequest request) {
        return ShopAdminMapper.variant(catalogService.updateVariantStock(variantId, request.stockOnHand(), request.expectedVersion()));
    }

    @DeleteMapping("/variants/{variantId}")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVariant(@PathVariable Long variantId) {
        catalogService.deleteVariant(variantId);
    }

    @GetMapping("/categories")
    public List<AdminCategoryResponse> categories() {
        List<AdminCategoryResponse> categories = catalogService.findAllCategories().stream().map(ShopAdminMapper::category).toList();
        log.info("Gestion boutique : {} catégorie(s) chargée(s)", categories.size());
        return categories;
    }

    @PostMapping("/categories")
    public ResponseEntity<AdminCategoryResponse> createCategory(@Valid @RequestBody AdminCategoryRequest request) {
        var category = catalogService.createCategory(request.name(), request.slug(), request.description(), request.active(), request.displayOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(ShopAdminMapper.category(category));
    }

    @PutMapping("/categories/{categoryId}")
    public AdminCategoryResponse updateCategory(@PathVariable Long categoryId, @Valid @RequestBody AdminCategoryRequest request) {
        return ShopAdminMapper.category(catalogService.updateCategory(categoryId, request.name(), request.slug(),
                request.description(), request.active(), request.displayOrder()));
    }

    @DeleteMapping("/categories/{categoryId}")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long categoryId) {
        catalogService.deleteCategory(categoryId);
    }

    private static Money money(AdminVariantRequest request) {
        return new Money(request.priceAmountInCents(), request.currency().toUpperCase());
    }

    private static SupplierOrderResponse supplierOrder(SupplierOrder order) {
        return new SupplierOrderResponse(order.getId(), order.getSupplierName(), order.getReference(),
                order.getStatus(), order.getCreatedAt(), order.getReceivedAt(), order.getLines().stream()
                .map(line -> new SupplierOrderResponse.Line(line.getId(), line.getVariantId(), line.getProductName(),
                        line.getVariantName(), line.getSku(), line.getQuantity(),
                        line.getUnitCostAmountInCents(), line.getUnitCostCurrency(),
                        line.getLineTotalAmountInCents(), line.getExpectedNeed(), line.isExtraApproved())).toList());
    }

    private static List<SupplierOrderService.Line> supplierLines(SupplierOrderCreateRequest request) {
        return request.lines().stream().map(line -> new SupplierOrderService.Line(line.variantId(), line.quantity(),
                line.unitCostAmountInCents(), Boolean.TRUE.equals(line.extraApproved()))).toList();
    }
}
