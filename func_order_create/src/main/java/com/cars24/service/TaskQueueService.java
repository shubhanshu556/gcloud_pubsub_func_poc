package com.cars24.service;

import com.cars24.pojo.OrderTask;
import com.google.cloud.tasks.v2.Task;

public interface TaskQueueService {

    void addTask(OrderTask task);
}
