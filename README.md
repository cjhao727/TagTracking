Tag Tracking Server
-
#### Before Coding
##### Analysis

According to the instruction, my original solution was to build a spring-boot application.
But after confirmed the specific requirements, I started working on my command-line application.

- Based on the description, I am thinking of have several domains as below to start with.
    ```java
    public class TagRequest {
        private String user;
        private List<String> add;
        private List<String> remove;
        private String timestamp;
    }
    
    public class TagResponse {
        private String user;
        private List<String> tags;
    }
    
    public class ErrorResponse {
        private String error;
    }
    ```
Regard to TagRequest and TagResponse, I am considering Set<> and List<>. But I decide to implement List<> here. 

- Key points I got from the instruction listed out as below.
  * ServerSocket
  * JSON request and response
  * Error handling
  * Handle multiple simultaneous requests (at least 50)
  * The server should continue running until it's sent Ctrl-C (Runtime.getRuntime.addShutDownHook).
  * The order of requests could be out of order.
  
  Based on these points above, I start thinking of MultiThread socket server and Thread Pooled server. Because the
  request is pretty short and constant. I decide to implement MultiThread socket. One thread takes care of one client.
  
  I need to serialize and deserialize JSON object, I'd like use Gson here.
  
  The tricky part is the order of requests. The immediate ideas contains sorting and extra space to store
  the request history.

---
#### Assumption
- If a tag is added and removed in the same millisecond, it should be treated as removed.

    I assume the request should look like this
    ```json
    {"user": "Siri", "add": ["jojo"], "remove": ["jojo"], "timestamp": "2018-08-10T06:49:04.420Z"}
    ```
    And the following case won't need to be handled.
    ```json
    {"user": "Siri", "add": ["jojo"], "remove": [], "timestamp": "2018-08-10T06:49:04.420Z"}
    {"user": "Siri", "add": [], "remove": ["jojo"], "timestamp": "2018-08-10T06:49:04.420Z"}
    ```

---
#### Ready to code

Start my Maven project.

- Main dependencies include lombok, gson and junit.
- Extra domains
    ```java
    public class UserRecord {
        private String userId;
        private List<UserOperationRecord> userOperationRecords;
        private List<String> tagCollection;
    }
    
    public class UserOperationRecord {
        private String timestamp;
        private List<String> addOperation;
        private List<String> removeOperation;
    }
    ```
- Implement a DAO layer to mimic the behaviors related to data container.

---
#### Testing

- Manual test
  * Start the server
      ```
      java -jar tagTracking-1.0-SNAPSHOT-jar-with-dependencies.jar
      ```
  * Test
      ```
      telent localhost 9527
      Trying ::1...
      Connected to localhost.
      Escape character is '^]'.
      {"user": "c1", "add": ["beyhive_member", "timbers_army"], "remove": [timbers_army], "timestamp": "2018-08-10T06:49:04.440Z"}
      {"user":"c1","tags":["beyhive_member"]}
      ```

- Automated test
  * Not 100%, but I followed the TDD(or could say BDD) especially for the request and response test.
  * Negative test
      ```java
      @Test
      public void testInvalidJsonString(){}
      ```
  * Different scenarios test
      ```java
      @Test
      public void testSingleRequest()
  
      @Test
      public void testMultipleRequestsInOrder()
  
      @Test
      public void testMultipleRequestsOutOfOrder()
      
      @Test
      public void testMultipleRequestsOutOfOrder2() 
        
      @Test
      public void testDelayedRequests()
      ```
  * Test server capacity
      ```java
      @Test
      public void testServerCapacity()
      ```
#### To build and run the app

This is maven application. I have attached the jar file. If necessary, to build the jar file just need to run
maven clean install and it will run my unit tests at same time.

And please check the test section above to run the jar file manually (note -jar-with-dependencies.jar).

Another alternative is to import this project into Intellij, and simply run the Application.

#### Improvement

Due to the time, here are some items I think I could improve in the future.

- Error handling. During my manual test, I found my application cannot handle empty input very well.
- Automated test. I'd like to add more test case to increase the code coverage.
- If I get time, I'd like to play with NIO2 to build the application.
- Refactor my current more to follow best coding and naming practices. 
- May need a better understanding of "If two requests ​RequestA​ and ​RequestZ​ for the same user are being processed concurrently"
  For my current solution, I am using testMultipleRequestsOutOfOrder2() to test this case, but I am still not feeling
  100% comfortable with this scenario.