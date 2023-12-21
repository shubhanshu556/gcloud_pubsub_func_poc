package com.cars24.handler;

import com.google.pubsub.v1.PubsubMessage;

public class RetryExhaustHandler {

    /*
    Here, we can process through multiple ways,
    1. we can publish to any DeadLetter queue for failure processing
    2. we can store in Cloud storage/ database for further scheduled processing
    */
    public void process(PubsubMessage message) {

    }
}
