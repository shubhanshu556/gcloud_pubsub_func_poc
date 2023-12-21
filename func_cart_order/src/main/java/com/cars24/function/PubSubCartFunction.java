package com.cars24.function;

import com.cars24.constants.FunctionConstants;
import com.cars24.handler.IdempotentHandler;
import com.cars24.handler.RetryExhaustHandler;
import com.cars24.processor.MessageProcessor;
import com.cars24.tasks.CloudTaskCreator;
import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

import java.util.logging.Logger;

public class PubSubCartFunction implements BackgroundFunction<PubsubMessage> {

    private static final Logger logger = Logger.getLogger(PubSubCartFunction.class.getName());
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_BACKOFF = 1000; // milliseconds
    private final MessageProcessor processor;

    private final RetryExhaustHandler exhaustHandler;

    private final IdempotentHandler idempotentHandler;

    /*
    Here, we are initializing,
    1. MessageProcessor
    2. RetryExhaustHandler
    */
    public PubSubCartFunction() {
        this.processor = new CloudTaskCreator(System.getenv(FunctionConstants.PROJECT_ID)
                ,System.getenv(FunctionConstants.LOCATION),
                System.getenv(FunctionConstants.TASK_QUEUE_ID),
                System.getenv(FunctionConstants.HTTP_URL));
        this.exhaustHandler = new RetryExhaustHandler();
        this.idempotentHandler =  new IdempotentHandler();
    }

    /*
    we are accepting the pub/sub event with subscription message
    and then sending it to retryProcessMessage method for processing
    and retry mechanism
    Also, checking if the same messageId was processed before
    */
    @Override
    public void accept(PubsubMessage message, Context context) {
        String data = message.getData() != null
                ? message.getData().toStringUtf8()
                : "No message data";

        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(data))
                .build();
        if (!idempotentHandler.isAlreadyProcessed(message.getMessageId()))
                retryProcessMessage(pubsubMessage, 0);
    }


    /*
    Here, we are accepting message and current count of attempt for failure handling
    doing few things here,
    1. processing the message based on logic written in CloudTaskCreator
    2. saving the messageId in cloud storage persistent
    2. If any failure occurs:
        a) we are retrying untill the MAX_RETRIES exhaust
        b) For every retry there is backoff time, calculated based on attempt count
        c) sleeping the thread as per backoff time
        d) again calling itself with attempt + 1
        e) If retry mechanism fails, we are logging the message and providing the
        message to further handling
    */
    private void retryProcessMessage(PubsubMessage message, int attempt) {
        try {
            processor.processMessage(message);
            idempotentHandler.saveProcessing(message.getMessageId());
        } catch (Exception e) {
            logger.warning("Some issue occured in processing : "+e);
            if (attempt < MAX_RETRIES) {
                long backoffTime = calculateBackoffTime(attempt);
                logger.info("Retrying due to failure. Attempt: " + (attempt + 1) + " of " + MAX_RETRIES + ". Waiting " + backoffTime + "ms.");
                sleep(backoffTime);
                retryProcessMessage(message, attempt + 1);
            } else {
                logger.severe("Max retries reached. Failed to process message: " + message);
                exhaustHandler.process(message);
            }
        }
    }

    /*
        Here, calculating the backoff time based on attempt count
    */
    private static long calculateBackoffTime(int attempt) {
        long exponentialBackoff = INITIAL_BACKOFF * (1L << attempt);
        double jitter = Math.random() * INITIAL_BACKOFF;
        return exponentialBackoff + (long) jitter;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.severe("Interrupted during backoff wait");
        }
    }
}
