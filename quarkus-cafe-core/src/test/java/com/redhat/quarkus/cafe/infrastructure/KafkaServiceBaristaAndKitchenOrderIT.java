package com.redhat.quarkus.cafe.infrastructure;

import com.redhat.quarkus.cafe.domain.*;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
@QuarkusTestResource(CafeITResource.class)
public class KafkaServiceBaristaAndKitchenOrderIT extends KafkaIT{

    @InjectMock
    OrderRepository orderRepository;

    @BeforeEach
    public void setup() {
        Mockito.doAnswer(new TestUtil.AssignIdToEntityAnswer(1L)).when(orderRepository).persist(any(Order.class));
    }

    @Test
    public void testOrderInBeveragesAndKitchen() {

        final List<LineItem> beverages = new ArrayList<>();
        beverages.add(new LineItem(Item.COFFEE_WITH_ROOM, "Harry"));
        beverages.add(new LineItem(Item.ESPRESSO_DOUBLE, "Ron"));

        final List<LineItem> menuItems = new ArrayList<>();
        menuItems.add(new LineItem(Item.CAKEPOP, "Harry"));
        menuItems.add(new LineItem(Item.MUFFIN, "Hermione"));

        final CreateOrderCommand createOrderCommand = new CreateOrderCommand(beverages, menuItems);

        // send the order to Kafka
        producerMap.get("web-in").send(new ProducerRecord("web-in", jsonb.toJson(createOrderCommand)));

        try {
            Thread.sleep(2000);
        } catch (final InterruptedException e) {
            assertNull(e);
        }

        // Get the appropriate consumer, point to the first message, and pull all messages
        final KafkaConsumer baristaConsumer = consumerMap.get("barista-in");
        baristaConsumer.seekToBeginning(new ArrayList<TopicPartition>());

        final KafkaConsumer kitchenConsumer = consumerMap.get("kitchen-in");
        kitchenConsumer.seekToBeginning(new ArrayList<TopicPartition>()); //

        final ConsumerRecords<String, String> baristaRecords = baristaConsumer.poll(Duration.ofMillis(1000));
        final ConsumerRecords<String, String> kitchenRecords = kitchenConsumer.poll(Duration.ofMillis(1000));

        // verify that the records are of the correct type
        baristaRecords.forEach(record -> {
            System.out.println(record.value());
            final OrderInEvent orderInEvent = JsonUtil.jsonb.fromJson(record.value(), OrderInEvent.class);
            assertEquals(EventType.BEVERAGE_ORDER_IN, orderInEvent.eventType);
            assertTrue(orderInEvent.item.equals(Item.ESPRESSO_DOUBLE) || orderInEvent.item.equals(Item.COFFEE_WITH_ROOM),
                    "The item should be either a " + Item.ESPRESSO_DOUBLE + " or a " + Item.COFFEE_WITH_ROOM + " not a " + orderInEvent.item);
        });

        // verify that the records are of the correct type
        kitchenRecords.forEach(record -> {
            System.out.println(record.value());
            final OrderInEvent orderInEvent = JsonUtil.jsonb.fromJson(record.value(), OrderInEvent.class);
            assertEquals(EventType.KITCHEN_ORDER_IN, orderInEvent.eventType);

            assertTrue(orderInEvent.item.equals(Item.CAKEPOP) || orderInEvent.item.equals(Item.MUFFIN),
                    "The item should be either a " + Item.MUFFIN + " or a " + Item.CAKEPOP + " not a " + orderInEvent.item);
        });

        // verify the number of new records
        assertEquals(2, baristaRecords.count());
        // verify the number of new records
        assertEquals(2, kitchenRecords.count());
    }
}
