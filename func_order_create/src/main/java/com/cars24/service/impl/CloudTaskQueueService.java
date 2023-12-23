package com.cars24.service.impl;

import com.cars24.constants.FunctionConstants;
import com.cars24.pojo.OrderTask;
import com.cars24.processor.MessageProcessor;
import com.cars24.service.TaskQueueService;
import com.cars24.tasks.CloudTaskCreator;
import com.google.cloud.tasks.v2.Task;

public class CloudTaskQueueService implements TaskQueueService {

    private final MessageProcessor messageProcessor;

    public CloudTaskQueueService() {
        this.messageProcessor = new CloudTaskCreator(System.getenv(FunctionConstants.PROJECT_ID)
                ,System.getenv(FunctionConstants.LOCATION),
                System.getenv(FunctionConstants.TASK_QUEUE_ID),
                System.getenv(FunctionConstants.HTTP_URL));
    }

    public void addTask(OrderTask task) throws Exception {
        messageProcessor.processMessage(task,0);
    }

    @Override
    public void addTaskWithDelay(OrderTask task, long delayInSec) throws Exception {
        messageProcessor.processMessage(task,delayInSec);
    }

}
