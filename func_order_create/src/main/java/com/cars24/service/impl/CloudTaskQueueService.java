package com.cars24.service.impl;

import com.cars24.pojo.OrderTask;
import com.cars24.service.TaskQueueService;
import com.google.cloud.tasks.v2.Task;

public class CloudTaskQueueService implements TaskQueueService {
    public void addTask(OrderTask task) {
        // Add task to cloud task queue
    }
}
