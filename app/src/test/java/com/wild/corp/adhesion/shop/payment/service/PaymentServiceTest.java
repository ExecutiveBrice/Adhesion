package com.wild.corp.adhesion.shop.payment.service;

import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.repository.UserRepository;
import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.order.model.OrderItem;
import com.wild.corp.adhesion.shop.order.model.OrderStatus;
import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import com.wild.corp.adhesion.shop.order.repository.ShopOrderRepository;
import com.wild.corp.adhesion.shop.order.service.OrderService;
import com.wild.corp.adhesion.shop.payment.config.PaymentProperties;
import com.wild.corp.adhesion.shop.payment.model.Payment;
import com.wild.corp.adhesion.shop.payment.model.PaymentAttempt;
import com.wild.corp.adhesion.shop.payment.model.PaymentStatus;
import com.wild.corp.adhesion.shop.payment.provider.FakePaymentProvider;
import com.wild.corp.adhesion.shop.payment.provider.PaymentGateway;
import com.wild.corp.adhesion.shop.payment.provider.PaymentLine;
import com.wild.corp.adhesion.shop.payment.provider.PaymentRequest;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderException;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderType;
import com.wild.corp.adhesion.shop.payment.provider.PaymentSession;
import com.wild.corp.adhesion.shop.payment.repository.PaymentAttemptRepository;
import com.wild.corp.adhesion.shop.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentAttemptRepository attemptRepository;
    @Mock
    private ShopOrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderService orderService;

    private FakePaymentProvider provider;
    private PaymentService paymentService;
    private ShopOrder order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider(PaymentProviderType.FAKE);
        provider = new FakePaymentProvider();
        PaymentGateway gateway = new PaymentGateway(properties, List.of(provider));
        paymentService = new PaymentService(paymentRepository, attemptRepository, orderRepository,
                userRepository, orderService, gateway);

        order = new ShopOrder("CMD-2026-000042", 42L, "EUR");
        ReflectionTestUtils.setField(order, "id", 42L);
        order.addItem(OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M",
                new Money(1_500, "EUR"), 2));
        order.submitForPayment();

        payment = new Payment(42L, order.getTotal());
        ReflectionTestUtils.setField(payment, "id", 7L);
        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(42L)).thenReturn(Optional.of(payment));
    }

    @Test
    void createsAnIdempotentSessionThenVerifiesPaymentServerSide() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptRepository.findByPaymentIdAndIdempotencyKey(7L, "payment-key"))
                .thenReturn(Optional.empty());

        PaymentSession session = paymentService.createPaymentSession(42L, "payment-key",
                URI.create("https://shop.example.test/orders/42"), URI.create("https://shop.example.test/cart"));
        PaymentAttempt attempt = payment.getAttempts().getFirst();
        ReflectionTestUtils.setField(attempt, "id", 9L);

        when(attemptRepository.findByPaymentIdAndIdempotencyKey(7L, "payment-key"))
                .thenReturn(Optional.of(attempt));
        PaymentSession replay = paymentService.createPaymentSession(42L, "payment-key",
                URI.create("https://shop.example.test/orders/42"), URI.create("https://shop.example.test/cart"));

        assertThat(replay).isEqualTo(session);
        assertThat(provider.createdSessionCount()).isEqualTo(1);

        provider.markSucceeded(session.externalPaymentId(), new Money(3_000, "EUR"));
        when(attemptRepository.findById(9L)).thenReturn(Optional.of(attempt));
        paymentService.refreshPaymentStatus(9L);
        paymentService.refreshPaymentStatus(9L);

        order.transitionTo(OrderStatus.PROCESSING);
        paymentService.refreshPaymentStatus(9L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(orderService).consumeReservedStock(order);
    }

    @Test
    void providerFailureIsRecordedOnTheAttemptOwnerWhenRepositoriesReturnDifferentPaymentInstances() {
        Payment attemptOwner = new Payment(42L, order.getTotal());
        ReflectionTestUtils.setField(attemptOwner, "id", 7L);
        PaymentAttempt existingAttempt = attemptOwner.startAttempt(PaymentProviderType.FAKE, "payment-key");
        when(attemptRepository.findByPaymentIdAndIdempotencyKey(7L, "payment-key"))
                .thenReturn(Optional.of(existingAttempt));

        PaymentGateway failingGateway = mock(PaymentGateway.class);
        when(failingGateway.configuredProvider()).thenReturn(PaymentProviderType.FAKE);
        PaymentProviderException failure = new PaymentProviderException(PaymentProviderType.FAKE,
                "PROVIDER_UNAVAILABLE", "Le fournisseur est indisponible", true);
        when(failingGateway.createPayment(any())).thenThrow(failure);
        PaymentService service = new PaymentService(paymentRepository, attemptRepository,
                orderRepository, userRepository, orderService, failingGateway);

        assertThatThrownBy(() -> service.createPaymentSession(42L, "payment-key",
                URI.create("https://shop.example.test/orders/42"), URI.create("https://shop.example.test/cart")))
                .isSameAs(failure);
        assertThat(existingAttempt.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(attemptOwner.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void sendsTheOrderSnapshotToThePaymentProvider() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptRepository.findByPaymentIdAndIdempotencyKey(7L, "payment-key"))
                .thenReturn(Optional.empty());
        PaymentGateway gateway = mock(PaymentGateway.class);
        when(gateway.configuredProvider()).thenReturn(PaymentProviderType.FAKE);
        when(gateway.createPayment(any())).thenReturn(new PaymentSession("payment-1",
                URI.create("https://payments.example.test/1"), PaymentStatus.PENDING));
        PaymentService service = new PaymentService(paymentRepository, attemptRepository,
                orderRepository, userRepository, orderService, gateway);

        service.createPaymentSession(42L, "payment-key", URI.create("https://shop.example.test/orders/42"),
                URI.create("https://shop.example.test/cart"));

        ArgumentCaptor<PaymentRequest> request = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(gateway).createPayment(request.capture());
        assertThat(request.getValue().lines()).containsExactly(new PaymentLine("Tee-shirt", "M", 2));
    }

    @Test
    void sendsTheAccountHolderContactDetailsAsOptionalCheckoutPayer() {
        User user = new User("alice@example.org", "unused");
        Adherent adherent = new Adherent();
        adherent.setPrenom("Alice");
        adherent.setNom("Martin");
        user.setAdherent(adherent);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptRepository.findByPaymentIdAndIdempotencyKey(7L, "payment-key"))
                .thenReturn(Optional.empty());
        PaymentGateway gateway = mock(PaymentGateway.class);
        when(gateway.configuredProvider()).thenReturn(PaymentProviderType.FAKE);
        when(gateway.createPayment(any())).thenReturn(new PaymentSession("payment-1",
                URI.create("https://payments.example.test/1"), PaymentStatus.PENDING));
        PaymentService service = new PaymentService(paymentRepository, attemptRepository,
                orderRepository, userRepository, orderService, gateway);

        service.createPaymentSession(42L, "payment-key", URI.create("https://shop.example.test/orders/42"),
                URI.create("https://shop.example.test/cart"));

        ArgumentCaptor<PaymentRequest> request = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(gateway).createPayment(request.capture());
        assertThat(request.getValue().payer().firstName()).isEqualTo("Alice");
        assertThat(request.getValue().payer().lastName()).isEqualTo("Martin");
        assertThat(request.getValue().payer().email()).isEqualTo("alice@example.org");
    }
}
