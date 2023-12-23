package com.cars24.service;

import com.cars24.pojo.Item;

import java.util.Date;

public interface InventoryService {

    boolean checkAvailability(Item item);

    String getWarehouseId(Item item);

    Date getDeliveryTime(String warehouseId, Item item);
}
