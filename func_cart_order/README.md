**Idempotent Cloud Function for Pub/Sub Processing**

**Introduction**
This README outlines the design and implementation of an idempotent Google Cloud Function that processes messages from Google Cloud Pub/Sub. It ensures that each unique message is processed exactly once, preventing duplicate processing and side effects.

**Objectives**
Idempotent Processing: Ensure each message is processed once.
Resilience to Failures: Handle failures to allow retries without duplicate processing.
Efficient Message Handling: Skip processing for already processed messages.
Scalable and Performant: Handle varying loads with consistent performance.

**Key Features**
Unique Message IDs: Leverage unique identifiers in messages for tracking.
Persistent Message Store: Use persistent storage for recording processed IDs.
Pre-Processing Verification: Check if incoming messages are already processed.
Business Logic Processing: Process only new messages.
Post-Processing Recording: Record message IDs post successful processing.
Failure Handling: Ensure message redelivery in case of processing failures.

**Workflow**
Trigger: Function is triggered by a Pub/Sub message.
Duplication Check: Verify if the message ID exists in the store.
Message Processing: Process if the message is new.
Record Processed IDs: Store ID post successful processing.
Failure Handling: Allow redelivery for unprocessed messages.

**Deployment Instructions**

gcloud functions deploy func_cart_order \
--entry-point com.cars24.function.PubSubCartFunction \
--runtime java11 \
--trigger-topic [YOUR_TRIGGER_TOPIC] \
--memory 256MB \
--project [YOUR_PROJECT_ID]
