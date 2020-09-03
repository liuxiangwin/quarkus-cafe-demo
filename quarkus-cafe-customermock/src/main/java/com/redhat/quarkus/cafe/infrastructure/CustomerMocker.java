package com.redhat.quarkus.cafe.infrastructure;

import com.redhat.quarkus.cafe.domain.OrderInCommand;
import com.redhat.quarkus.cafe.domain.CustomerNames;
import com.redhat.quarkus.cafe.domain.Item;
import com.redhat.quarkus.cafe.domain.LineItem;
import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.redhat.quarkus.cafe.infrastructure.JsonUtil.toJson;

/**
 * Creates and sends CreateOrderCommand objects to the web application
 */
@ApplicationScoped
public class CustomerMocker {

    final Logger logger = LoggerFactory.getLogger(CustomerMocker.class);

    @Inject
    @RestClient
    OrderService orderService;

    @Scheduled(every = "30s")
    public void placeOrder() {
//        int seconds = new Random().nextInt(5);
/*
        try {
*/
//            Thread.sleep(seconds * 5000);
            int orders = new Random().nextInt(5);
            List<OrderInCommand> mockOrders = mockCustomerOrders(orders);
            mockOrders.forEach(mockOrder -> {
                orderService.placeOrders(mockOrder);
                logger.debug("placed order: {}", toJson(mockOrder));
            });
/*
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
*/
    }


    public List<OrderInCommand> mockCustomerOrders(int desiredNumberOfOrders) {

        return Stream.generate(() -> {
            OrderInCommand createOrderCommand = new OrderInCommand();
            createOrderCommand.id = UUID.randomUUID().toString();
            createOrderCommand.beverages = createBeverages();
            // not all orders have kitchen items
            if (desiredNumberOfOrders % 2 == 0) {
                createOrderCommand.kitchenOrders = createKitchenItems();
            }
            return createOrderCommand;
        }).limit(desiredNumberOfOrders).collect(Collectors.toList());
    }

    private List<LineItem> createBeverages() {

        List<LineItem> beverages = new ArrayList(2);
        beverages.add(new LineItem(randomBaristaItem(), randomCustomerName()));
        beverages.add(new LineItem(randomBaristaItem(), randomCustomerName()));
        return beverages;
    }

    private List<LineItem> createKitchenItems() {
        List<LineItem> kitchenOrders = new ArrayList(2);
        kitchenOrders.add(new LineItem(randomKitchenItem(), randomCustomerName()));
        kitchenOrders.add(new LineItem(randomKitchenItem(), randomCustomerName()));
        return kitchenOrders;
    }

    Item randomBaristaItem() {
        return Item.values()[new Random().nextInt(5)];
    }

    Item randomKitchenItem() {
        return Item.values()[new Random().nextInt(3)+5];
    }

    String randomCustomerName() {
        return CustomerNames.randomName();
    }

}
