package com.redhat.quarkus.cafe.barista.infrastructure;

import com.redhat.quarkus.cafe.barista.domain.*;
import com.redhat.quarkus.cafe.barista.domain.BeverageOrder;
import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.swing.plaf.basic.BasicViewportUI;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class KafkaResource {

    Logger logger = Logger.getLogger(KafkaResource.class);

    private static final String TOPIC = "orders";

    @Inject
    Vertx vertx;

    private KafkaProducer<String, String> producer;


    @Inject
    Barista barista;

    private Jsonb jsonb = JsonbBuilder.create();


    @Incoming("ordersin")
    public void orderIn(String message) {

        System.out.println("\nmessage received:\n" + message);
        logger.debug("\nOrder Received:\n" + message);

        JsonReader reader = Json.createReader(new StringReader(message));
        JsonObject jsonObject = reader.readObject();
        String eventType = jsonObject.getString("eventType");

        if (eventType.equals(EventType.BEVERAGE_ORDER_IN.toString())) {

            logger.debug("\nBarista Order In Received:\n");

            OrderInEvent orderEvent = jsonb.fromJson(message, OrderInEvent.class);
            onOrderIn(orderEvent).thenApply(res -> {
                updateKafka(res);
                return null;
            });
        }

    }

    private void updateKafka(final BeverageOrder orderEvent) {
        System.out.println("\nNow update Kafka!");
        logger.debug("\nSending:" + orderEvent.toString());

        KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(
                TOPIC,
                orderEvent.itemId,
                jsonb.toJson(orderEvent));
        System.out.println(record);
        producer.send(record, res ->{
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }
        });
    }


    private CompletionStage<BeverageOrder> onOrderIn(OrderInEvent orderInEvent) {

        return barista.orderIn(orderInEvent);
    }

/*
    @Incoming("ordersin")
    @Outgoing("ordersout")
    public CompletionStage<BeverageOrder> orderIn(String message) {

        logger.debug(message);

        System.out.println("order in:" + message);
        OrderInEvent orderInEvent = jsonb.fromJson(message, OrderInEvent.class);
        return barista.orderIn(orderInEvent);
    }
*/

    public class KafkaTopics{

        public static final String INCOMING = "events";
        public static final String OUTGOING = "events";
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