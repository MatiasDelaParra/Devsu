package com.devsu.account.messaging;

import com.devsu.account.event.CustomerEvent;
import com.devsu.account.event.CustomerEventType;
import com.devsu.account.exception.InvalidCustomerEventException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CustomerEventMessageMapper {

    private static final Map<String, CustomerEventType> ROUTING_KEY_TYPES = Map.of(
            "customer.created", CustomerEventType.CUSTOMER_CREATED,
            "customer.updated", CustomerEventType.CUSTOMER_UPDATED,
            "customer.status-changed", CustomerEventType.CUSTOMER_STATUS_CHANGED,
            "customer.deleted", CustomerEventType.CUSTOMER_DELETED
    );

    private final ObjectMapper objectMapper;

    public CustomerEvent map(Message message) {
        try {
            JsonNode root = objectMapper.readTree(message.getBody());
            JsonNode payload = payloadNode(root);
            CustomerEventType type = resolveType(message, root);
            return new CustomerEvent(
                    eventId(message, root),
                    type,
                    text(payload, "customerId"),
                    text(payload, "name"),
                    text(payload, "identification"),
                    booleanValue(payload, "status"),
                    instant(payload, "occurredAt")
            );
        } catch (InvalidCustomerEventException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidCustomerEventException("Malformed customer event payload", exception);
        }
    }

    private UUID eventId(Message message, JsonNode root) {
        Object header = firstNonNull(
                message.getMessageProperties().getHeaders().get("eventId"),
                message.getMessageProperties().getHeaders().get("x-event-id")
        );
        String value = header == null ? text(root, "eventId") : header.toString();
        if (!StringUtils.hasText(value)) {
            throw new InvalidCustomerEventException("Customer event id is required");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidCustomerEventException("Invalid customer event id", exception);
        }
    }

    private JsonNode payloadNode(JsonNode root) throws Exception {
        JsonNode payload = root.get("payload");
        if (payload == null || payload.isNull()) {
            return root;
        }
        return payload.isTextual() ? objectMapper.readTree(payload.textValue()) : payload;
    }

    private CustomerEventType resolveType(Message message, JsonNode root) {
        Object headerType = firstNonNull(
                message.getMessageProperties().getHeaders().get("eventType"),
                message.getMessageProperties().getHeaders().get("x-event-type")
        );
        String rawType = headerType == null ? text(root, "eventType") : headerType.toString();
        if (StringUtils.hasText(rawType)) {
            try {
                return CustomerEventType.valueOf(rawType);
            } catch (IllegalArgumentException exception) {
                throw new InvalidCustomerEventException("Unsupported customer event type: " + rawType);
            }
        }

        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        CustomerEventType type = ROUTING_KEY_TYPES.get(routingKey);
        if (type == null) {
            throw new InvalidCustomerEventException(
                    "Unsupported customer event routing key: " + routingKey
            );
        }
        return type;
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Boolean booleanValue(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asBoolean();
    }

    private Instant instant(JsonNode node, String field) {
        String value = text(node, field);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new InvalidCustomerEventException("Invalid occurredAt timestamp", exception);
        }
    }
}
