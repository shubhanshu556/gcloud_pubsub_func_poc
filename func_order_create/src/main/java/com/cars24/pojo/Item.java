package com.cars24.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class Item {

    private String itemId;
    private String warehouseId;
    private boolean isAvailable;
    private Date deliveryTime;
}
