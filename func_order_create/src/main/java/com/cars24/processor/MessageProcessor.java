package com.cars24.processor;

import com.cars24.pojo.OrderTask;
import com.google.pubsub.v1.PubsubMessage;

public interface MessageProcessor {

    /*
    abstract method, to handle multiple ways of processing based
     on business logic
    */
    void processMessage(OrderTask task) throws Exception;

}
