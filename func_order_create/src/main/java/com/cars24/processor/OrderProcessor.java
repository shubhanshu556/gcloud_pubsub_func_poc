package com.cars24.processor;

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

import static com.google.common.collect.Comparators.max;

public class OrderProcessor {
    private InventoryService inventoryService;
    private TaskQueueService taskQueueService;

    public OrderProcessor(InventoryService inventoryService, TaskQueueService taskQueueService) {
        this.inventoryService = inventoryService;
        this.taskQueueService = taskQueueService;
    }

    public void processOrder(Order order) {
        Map<String, Order> warehouseOrders = new HashMap<>();
        Date latestDeliveryTime = new Date();

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
                latestDeliveryTime = max(latestDeliveryTime, item.getDeliveryTime());

        }

        for (Order subOrder : warehouseOrders.values()) {
            // Set the dispatch time based on the latest delivery time
            subOrder.setDispatchTime(latestDeliveryTime);

            OrderTask dispatchTask = new OrderTask(subOrder);
            taskQueueService.addTask(dispatchTask);
        }
    }

    private Date max(Date date1, Date date2) {
        return (date1.after(date2)) ? date1 : date2;
    }
}
