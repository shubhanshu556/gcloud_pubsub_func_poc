package com.cars24.tasks;

import com.cars24.pojo.OrderTask;
import com.cars24.processor.MessageProcessor;
import com.google.cloud.tasks.v2.*;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.time.Instant;
import java.util.logging.Logger;

import com.google.protobuf.Timestamp;
import com.google.pubsub.v1.PubsubMessage;

public class CloudTaskCreator implements MessageProcessor {

    private static final Logger logger = Logger.getLogger(CloudTaskCreator.class.getName());


    private final String projectId;
    private final String locationId;
    private final String queueId;
    private final String url;

    public CloudTaskCreator(String projectId, String locationId, String queueId, String url) {
        this.projectId = projectId;
        this.locationId = locationId;
        this.queueId = queueId;
        this.url = url;
    }

    /*
        Here, we are processing the message, Creating a Http task for each and
        publishing to task queue
    */
    @Override
    public void processMessage(OrderTask orderTask, long delayInSeconds) throws Exception {

        if (delayInSeconds == 0) {
            processMessage(orderTask);
            return;
        }


        String payload = new Gson().toJson(orderTask.getOrder());
        orderTask.getOrder();
        try (CloudTasksClient client = CloudTasksClient.create()) {
            String queuePath = QueueName.of(projectId, locationId, queueId).toString();
            Instant scheduleTime = Instant.now().plusSeconds(delayInSeconds);

            Task.Builder taskBuilder = Task.newBuilder()
                    .setHttpRequest(HttpRequest.newBuilder()
                            .setBody(ByteString.copyFromUtf8(payload))
                            .setUrl(url)
                            .setHttpMethod(HttpMethod.POST)
                            .build())
                    .setScheduleTime(Timestamp.newBuilder()
                        .setSeconds(scheduleTime.getEpochSecond())
                        .setNanos(scheduleTime.getNano())
                        .build());

            Task task = client.createTask(queuePath, taskBuilder.build());

            logger.info("Task created : {} , with delay: {}"+ task.getName());
        } catch (IOException e) {
            throw new Exception("Failed to create task", e);
        }
    }

    public void processMessage(OrderTask orderTask) throws Exception {
        String payload = new Gson().toJson(orderTask.getOrder());
        orderTask.getOrder();
        try (CloudTasksClient client = CloudTasksClient.create()) {
            String queuePath = QueueName.of(projectId, locationId, queueId).toString();
            Task.Builder taskBuilder = Task.newBuilder()
                    .setHttpRequest(HttpRequest.newBuilder()
                            .setBody(ByteString.copyFromUtf8(payload))
                            .setUrl(url)
                            .setHttpMethod(HttpMethod.POST)

                            .build());

            Task task = client.createTask(queuePath, taskBuilder.build());

            logger.info("Task created: {}"+ task.getName());
        } catch (IOException e) {
            throw new Exception("Failed to create task", e);
        }
    }
}
