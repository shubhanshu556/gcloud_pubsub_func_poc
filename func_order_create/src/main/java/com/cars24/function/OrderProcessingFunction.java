package com.cars24.function;

import com.cars24.handler.IdempotentHandler;
import com.cars24.handler.RetryExhaustHandler;
import com.cars24.pojo.Order;
import com.cars24.processor.OrderProcessor;
import com.cars24.service.InventoryService;
import com.cars24.service.TaskQueueService;
import com.cars24.service.impl.CloudTaskQueueService;
import com.cars24.service.impl.WarehouseInventoryService;
import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.logging.Logger;

public class OrderProcessingFunction implements BackgroundFunction<PubsubMessage> {

    private static final Logger logger = Logger.getLogger(OrderProcessingFunction.class.getName());

    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_BACKOFF = 1000; // milliseconds
    private OrderProcessor orderProcessor;

    private IdempotentHandler idempotentHandler;
    private RetryExhaustHandler exhaustHandler;

    private Gson gson;

    protected Gson getGson() {
        if(gson != null) return gson;

        GsonBuilder builder = new GsonBuilder();
        gson = builder.create();
        return gson;
    }

    public OrderProcessingFunction() {
        this.idempotentHandler = new IdempotentHandler();
        this.exhaustHandler = new RetryExhaustHandler();
        InventoryService inventoryService = new WarehouseInventoryService();
        TaskQueueService taskQueueService = new CloudTaskQueueService();
        this.orderProcessor = new OrderProcessor(inventoryService, taskQueueService);
        gson = new Gson();
    }

    @Override
    public void accept(PubsubMessage message, Context context) {
        logger.info("Message Received :: "+message.getMessageId());
        if (!idempotentHandler.isAlreadyProcessed(context.eventId())) {
                processMessageWithRetry(message, context);
        }

    }


    private void processMessageWithRetry(PubsubMessage message, Context context) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(MAX_RETRIES)
                .waitDuration(Duration.ofMillis(INITIAL_BACKOFF))
                .retryExceptions(Exception.class)
                .build();

        Retry retry = Retry.of("idempotentRetry", config);
        try {
            Type t = TypeToken.getParameterized(Order.class).getType();
            Order order = getGson().fromJson(message.getData().toStringUtf8(), t);
            Retry.decorateSupplier(retry, () -> {
                orderProcessor.processOrder(order);
                idempotentHandler.saveProcessing(context.eventId());
                return null;
            }).get();
        } catch (Exception e) {
            logger.severe("Max retries reached or unexpected error occurred. Failed to process message: " + message);
            exhaustHandler.process(message);
        }
    }

}
