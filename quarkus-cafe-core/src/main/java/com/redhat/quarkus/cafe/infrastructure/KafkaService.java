package com.redhat.quarkus.cafe.infrastructure;

import com.redhat.quarkus.cafe.domain.*;
import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class KafkaService {

    private static final Logger logger = Logger.getLogger(KafkaService.class);

    private static final String TOPIC = "orders";

    private static final String TOPIC_UI = "webui";

    @Inject
    Cafe cafe;

    @Inject
    private Vertx vertx;

    Jsonb jsonb = JsonbBuilder.create();

    private KafkaProducer<String, String> producer;

    @Incoming("ordersup")
    public void orderUp(String message) {

        System.out.println("\nmessage received:\n" + message);
        logger.debug("\nOrder Received:\n" + message);

        JsonReader reader = Json.createReader(new StringReader(message));
        JsonObject jsonObject = reader.readObject();
        String eventType = jsonObject.getString("eventType");

        if (eventType.equals(EventType.BEVERAGE_ORDER_UP.toString()) || eventType.equals(EventType.KITCHEN_ORDER_UP.toString())) {

            OrderUpEvent orderUpEvent = jsonb.fromJson(message, OrderUpEvent.class);
            cafe.orderUp(Arrays.asList(orderUpEvent));
        }
    }

/*
    public CompletableFuture<Void> produce(List<OrderInEvent> cafeEventList) {

        return CompletableFuture.runAsync(() -> {
            cafeEventList.stream().forEach(cafeEvent -> {
                logger.debug("\nSending:" + cafeEvent.toString());
                System.out.println(cafeEvent);
                KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(
                        TOPIC,
                        cafeEvent.itemId,
                        jsonb.toJson(cafeEvent));
                System.out.println(record);
                producer.send(record, res ->{
                    if (res.failed()) {
                        throw new RuntimeException(res.cause());
                    }
                });
            });
        });

    }
*/

    public CompletableFuture<Void> updateOrders(List<OrderEvent> orderEvents) {

        return sendToKafka(orderEvents, TOPIC);
    }

    private CompletableFuture<Void> sendToKafka(final List<OrderEvent> orderEvents, final String topic) {

        return CompletableFuture.runAsync(() -> {
            orderEvents.stream().forEach(cafeEvent -> {
                logger.debug("\nSending:" + cafeEvent.toString());
                System.out.println(cafeEvent);
                KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(
                        topic,
                        cafeEvent.itemId,
                        jsonb.toJson(cafeEvent));
                System.out.println(record);
                producer.send(record, res ->{
                    if (res.failed()) {
                        throw new RuntimeException(res.cause());
                    }
                });
            });
        });
    }

    @PostConstruct
    public void postConstruct() {
        // Config values can be moved to application.properties
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", "localhost:9092");
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("acks", "1");
        producer = KafkaProducer.create(vertx, config);

    }

}