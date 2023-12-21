package com.cars24.handler;

public class IdempotentHandler {

    /*
    Here, we are checking if any message Id is duplicate or already processed
    1. If yes, then we are returning as true
    2. otherwise returning false
    */
    public boolean isAlreadyProcessed(String messageId) {

        // check if exist in persistent storage ,
            // if exist : return true
            // if not : return false

        return false;
    }

    /*
       Here, saving the messageId in storage
       */
    public String saveProcessing(String messageId) {

        // save the message Id in persistent storage

        return messageId;
    }
}
