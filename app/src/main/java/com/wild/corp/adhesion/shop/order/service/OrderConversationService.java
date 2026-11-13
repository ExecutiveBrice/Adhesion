package com.wild.corp.adhesion.shop.order.service;

import com.wild.corp.adhesion.shop.order.model.OrderMessage;
import com.wild.corp.adhesion.shop.order.model.OrderMessageSender;
import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import com.wild.corp.adhesion.shop.order.repository.OrderMessageRepository;
import com.wild.corp.adhesion.shop.order.repository.ShopOrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class OrderConversationService {

    private final ShopOrderRepository orderRepository;
    private final OrderMessageRepository messageRepository;

    public OrderConversationService(ShopOrderRepository orderRepository, OrderMessageRepository messageRepository) {
        this.orderRepository = orderRepository;
        this.messageRepository = messageRepository;
    }

    public List<OrderMessage> messagesForCustomer(String orderNumber, Long customerUserId) {
        return messages(requireCustomerOrder(orderNumber, customerUserId));
    }

    public OrderMessage sendAsCustomer(String orderNumber, Long customerUserId, String content) {
        return send(requireCustomerOrder(orderNumber, customerUserId), customerUserId, OrderMessageSender.CUSTOMER, content);
    }

    public List<OrderMessage> messagesForManager(String orderNumber) {
        return messages(requireOrder(orderNumber));
    }

    public OrderMessage sendAsManager(String orderNumber, Long managerUserId, String content) {
        return send(requireOrder(orderNumber), managerUserId, OrderMessageSender.SHOP_MANAGER, content);
    }

    private List<OrderMessage> messages(ShopOrder order) {
        requireRefundRequested(order);
        return messageRepository.findAllByOrderIdOrderByCreatedAtAscIdAsc(order.getId());
    }

    private OrderMessage send(ShopOrder order, Long senderUserId, OrderMessageSender senderType, String content) {
        requireRefundRequested(order);
        return messageRepository.save(new OrderMessage(order, senderUserId, senderType, content));
    }

    private ShopOrder requireCustomerOrder(String orderNumber, Long customerUserId) {
        ShopOrder order = requireOrder(orderNumber);
        if (!order.getCustomerUserId().equals(customerUserId)) {
            throw new NoSuchElementException("Commande introuvable");
        }
        return order;
    }

    private ShopOrder requireOrder(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NoSuchElementException("Commande introuvable"));
    }

    private void requireRefundRequested(ShopOrder order) {
        if (!order.isRefundRequested()) {
            throw new IllegalStateException("Aucune demande de remboursement n'est ouverte pour cette commande");
        }
    }
}
