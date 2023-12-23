**Cloud Function for Handling Split Orders**

**Overview**

This Cloud Function is designed to handle orders that require splitting due to
item availability in different warehouses. It triggers from a Pub/Sub message
containing order details, checks the items against inventory data, splits the order
if necessary, and queues tasks for each sub-order with synchronized dispatch times.