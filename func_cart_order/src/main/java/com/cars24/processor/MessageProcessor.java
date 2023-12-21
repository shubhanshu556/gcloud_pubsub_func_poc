package com.cars24.processor;

import com.google.pubsub.v1.PubsubMessage;

public interface MessageProcessor {

    /*
    abstract method, to handle multiple ways of processing based
     on business logic
    */
    void processMessage(PubsubMessage message) throws Exception;

}
