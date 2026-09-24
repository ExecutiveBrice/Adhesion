package com.wild.corp.adhesion.shop.api;

import com.wild.corp.adhesion.models.UserDetails;
import com.wild.corp.adhesion.shop.api.dto.CartItemRequest;
import com.wild.corp.adhesion.shop.api.dto.CartQuoteRequest;
import com.wild.corp.adhesion.shop.api.dto.CartQuoteResponse;
import com.wild.corp.adhesion.shop.api.dto.OrderResponse;
import com.wild.corp.adhesion.shop.api.dto.ProductResponse;
import com.wild.corp.adhesion.shop.cart.service.CartPricingService;
import com.wild.corp.adhesion.shop.catalog.service.CatalogService;
import com.wild.corp.adhesion.shop.order.service.OrderItemRequest;
import com.wild.corp.adhesion.shop.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/shop")
@Validated
@Tag(name = "Boutique", description = "Catalogue public, prévisualisation du panier et commandes")
public class ShopController {

    private final CatalogService catalogService;
    private final CartPricingService cartPricingService;
    private final OrderService orderService;

    public ShopController(CatalogService catalogService,
                          CartPricingService cartPricingService,
                          OrderService orderService) {
        this.catalogService = catalogService;
        this.cartPricingService = cartPricingService;
        this.orderService = orderService;
    }

    @GetMapping("/products")
    @Operation(summary = "Liste les produits actifs de la boutique")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Catalogue actif",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))))
    public ResponseEntity<List<ProductResponse>> products() {
        return ResponseEntity.ok(catalogService.findActiveProducts().stream().map(ShopApiMapper::product).toList());
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Retourne un produit actif")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produit actif"),
            @ApiResponse(responseCode = "404", description = "Produit introuvable")
    })
    public ResponseEntity<ProductResponse> product(@PathVariable Long productId) {
        return ResponseEntity.ok(ShopApiMapper.product(catalogService.findActiveProduct(productId)));
    }

    @PostMapping("/cart/quote")
    @Operation(summary = "Calcule le panier côté serveur sans réserver le stock")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Panier chiffré côté serveur"),
            @ApiResponse(responseCode = "400", description = "Panier invalide"),
            @ApiResponse(responseCode = "422", description = "Article indisponible")
    })
    public ResponseEntity<CartQuoteResponse> quote(@Valid @RequestBody CartQuoteRequest request) {
        return ResponseEntity.ok(ShopApiMapper.quote(cartPricingService.quote(toOrderItems(request.items()))));
    }

    @PostMapping("/orders")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crée une commande à partir des références du panier")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Commande créée ou rejouée de façon idempotente"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "422", description = "Article indisponible")
    })
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody CartQuoteRequest request,
            Authentication authentication) {
        UserDetails user = currentUser(authentication);
        var order = orderService.createOrder(user.getId(), idempotencyKey, toOrderItems(request.items()));
        return ResponseEntity.created(URI.create("/shop/orders/" + order.getOrderNumber()))
                .body(ShopApiMapper.order(order));
    }

    @GetMapping("/orders/{orderNumber}")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Retourne une commande appartenant à l'utilisateur connecté")
    public ResponseEntity<OrderResponse> order(@PathVariable String orderNumber, Authentication authentication) {
        UserDetails user = currentUser(authentication);
        return ResponseEntity.ok(ShopApiMapper.order(orderService.findOrderForCustomer(orderNumber, user.getId())));
    }

    private static List<OrderItemRequest> toOrderItems(List<CartItemRequest> items) {
        return items.stream().map(item -> new OrderItemRequest(item.variantId(), item.quantity())).toList();
    }

    private static UserDetails currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails user) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur authentifié introuvable");
    }
}
