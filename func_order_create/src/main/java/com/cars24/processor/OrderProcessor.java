package com.cars24.processor;

import com.cars24.function.OrderProcessingFunction;
import com.cars24.pojo.Item;
import com.cars24.pojo.Order;
import com.cars24.pojo.OrderTask;
import com.cars24.service.InventoryService;
import com.cars24.service.TaskQueueService;
import com.google.cloud.tasks.v2.Task;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class OrderProcessor {

    private static final Logger logger = Logger.getLogger(OrderProcessingFunction.class.getName());

    private InventoryService inventoryService;
    private TaskQueueService taskQueueService;

    public OrderProcessor(InventoryService inventoryService, TaskQueueService taskQueueService) {
        this.inventoryService = inventoryService;
        this.taskQueueService = taskQueueService;
    }

    public void processOrder(Order order) {
        Map<String, Order> warehouseOrders = new HashMap<>();


        for (Item item : order.getItems()) {
            if (!inventoryService.checkAvailability(item)) {
                // Logic to handle unavailable items
                String warehouseId = inventoryService.getWarehouseId(item);
                Date deliveryTime = inventoryService.getDeliveryTime(warehouseId, item);
                item.setDeliveryTime(deliveryTime);
                item.setWarehouseId(warehouseId);
            }
            String warehouseId = item.getWarehouseId();
            warehouseOrders.putIfAbsent(warehouseId, new Order(order.getOrderId(), new ArrayList<>(), order.getDispatchTime()));

            warehouseOrders.get(warehouseId).addItem(item);
        }
        Date latestDeliveryTime = findLatestDeliveryTime(order);
        for (Order subOrder : warehouseOrders.values()) {
            // Set the dispatch time based on the latest delivery time
            subOrder.setDispatchTime(latestDeliveryTime);

            long delayInSeconds = getDelayInSeconds(subOrder, latestDeliveryTime);
            OrderTask dispatchTask = new OrderTask(subOrder);

            // Create tasks with appropriate delay
            try {
                taskQueueService.addTaskWithDelay(dispatchTask, delayInSeconds);
            } catch (Exception e) {
                logger.severe("Failed to add task in taskqueue: " + dispatchTask);
                throw new RuntimeException(e);
            }
        }
    }

    private Date findLatestDeliveryTime(Order order) {
        return order.getItems().stream()
                .map(Item::getDeliveryTime)
                .max(Date::compareTo)
                .orElse(new Date());
    }

    private long getDelayInSeconds(Order subOrder, Date latestDeliveryTime) {
        long delayMillis = latestDeliveryTime.getTime() - subOrder.getDispatchTime().getTime();
        return TimeUnit.MILLISECONDS.toSeconds(Math.max(delayMillis, 0));
    }
}
