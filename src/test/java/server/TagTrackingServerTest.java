package server;

import com.j.client.TagTrackingClient;
import com.j.server.TagTrackingServer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class TagTrackingServerTest {
    private static int port;
    private static final String LOCAL_HOST = "127.0.0.1";

    @BeforeClass
    public static void start() throws InterruptedException, IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        serverSocket.close();

        Executors.newSingleThreadExecutor().submit(() -> new TagTrackingServer().start(port));
        Thread.sleep(500);
    }

    @Test
    public void testInvalidJsonString() {
        TagTrackingClient tagTrackingClient = new TagTrackingClient();
        tagTrackingClient.startConnection(LOCAL_HOST, port);

        String testJsonResponse = tagTrackingClient.sendMessage("({})");
        String expectedJsonOutput = "{\"error\":\"java.lang.IllegalStateException: Expected BEGIN_OBJECT but was STRING at line 1 column 1 path $\"}";

        assertEquals(expectedJsonOutput, testJsonResponse);

        tagTrackingClient.stopConnection();
    }

    @Test
    public void testSingleRequest() {
        TagTrackingClient tagTrackingClient = new TagTrackingClient();
        tagTrackingClient.startConnection(LOCAL_HOST, port);

        String testJsonInput = "{\"user\": \"Secret Squirrel\", \"add\": [\"beyhive_member\", \"timbers_army\", \"jojo\", \"maodan\"], \"remove\": [\"maodan\"], \"timestamp\": \"2018-08-10T06:49:04.430Z\"}";
        String testJsonResponse = tagTrackingClient.sendMessage(testJsonInput);
        String expectedJsonOutput = "{\"user\":\"Secret Squirrel\",\"tags\":[\"beyhive_member\",\"timbers_army\",\"jojo\"]}";

        assertEquals(expectedJsonOutput, testJsonResponse);

        tagTrackingClient.stopConnection();
    }

    @Test
    public void testMultipleRequestsInOrder() {
        TagTrackingClient tagTrackingClient = new TagTrackingClient();
        tagTrackingClient.startConnection(LOCAL_HOST, port);

        String testJsonInput = "{\"user\": \"Secret Squirrel\", \"add\": [\"beyhive_member\", \"timbers_army\", \"jojo\", \"maodan\"], \"remove\": [\"maodan\"], \"timestamp\": \"2018-08-10T06:49:04.430Z\"}";
        String testJsonInput2 = "{\"user\": \"Secret Squirrel\", \"add\": [\"beyhive_member\", \"timbers_army\", \"jojo\", \"maodan\"], \"remove\": [\"jojo\"], \"timestamp\": \"2018-08-10T06:49:04.440Z\"}";
        String testJsonResponse = tagTrackingClient.sendMessage(testJsonInput);
        String testJsonResponse2 = tagTrackingClient.sendMessage(testJsonInput2);
        String expectedJsonOutput = "{\"user\":\"Secret Squirrel\",\"tags\":[\"beyhive_member\",\"timbers_army\",\"jojo\"]}";
        String expectedJsonOutput2 = "{\"user\":\"Secret Squirrel\",\"tags\":[\"beyhive_member\",\"timbers_army\",\"maodan\"]}";

        assertEquals(expectedJsonOutput, testJsonResponse);
        assertEquals(expectedJsonOutput2, testJsonResponse2);

        tagTrackingClient.stopConnection();
    }

    @Test
    public void testMultipleRequestsOutOfOrder() {
        TagTrackingClient tagTrackingClient = new TagTrackingClient();
        tagTrackingClient.startConnection(LOCAL_HOST, port);

        String testRequest1 = "{\"user\": \"Siri\", \"add\": [\"jojo\"], \"remove\": [], \"timestamp\": \"2018-08-10T06:49:04.420Z\"}";
        String testRequest2 = "{\"user\": \"Siri\", \"add\": [\"jojo\"], \"remove\": [], \"timestamp\": \"2018-08-10T06:49:04.410Z\"}";
        String testRequest3 = "{\"user\": \"Siri\", \"add\": [], \"remove\": [\"jojo\"], \"timestamp\": \"2018-08-10T06:49:04.415Z\"}";

        String testJsonMsg1 = tagTrackingClient.sendMessage(testRequest1);
        String testJsonMsg2 = tagTrackingClient.sendMessage(testRequest2);
        String testJsonMsg3 = tagTrackingClient.sendMessage(testRequest3);

        String expectedJsonOutput = "{\"user\":\"Siri\",\"tags\":[\"jojo\"]}";

        assertEquals(expectedJsonOutput, testJsonMsg1);
        assertEquals(expectedJsonOutput, testJsonMsg2);
        assertEquals(expectedJsonOutput, testJsonMsg3);

        tagTrackingClient.stopConnection();
    }

    @Test
    public void testMultipleRequestsOutOfOrder2() {
        TagTrackingClient tagTrackingClient = new TagTrackingClient();
        tagTrackingClient.startConnection(LOCAL_HOST, port);

        String testRequest2 = "{\"user\": \"Secret Squirrel\", \"add\": [\"beyhive_member\", \"timbers_army\", \"jojo\", \"maodan\"], \"remove\": [\"jojo\"], \"timestamp\": \"2018-08-10T06:49:04.440Z\"}";
        String testRequest = "{\"user\": \"Secret Squirrel\", \"add\": [\"beyhive_member\", \"timbers_army\", \"jojo\", \"maodan\"], \"remove\": [\"maodan\"], \"timestamp\": \"2018-08-10T06:49:04.430Z\"}";

        String testJsonMsg2 = tagTrackingClient.sendMessage(testRequest2);
        String testJsonMsg = tagTrackingClient.sendMessage(testRequest);

        String expectedJsonOutput = "{\"user\":\"Secret Squirrel\",\"tags\":[\"beyhive_member\",\"timbers_army\",\"maodan\"]}";

        assertEquals(expectedJsonOutput, testJsonMsg2);
        assertEquals(expectedJsonOutput, testJsonMsg);

        tagTrackingClient.stopConnection();
    }

    @Test
    public void testDelayedRequests() {
        TagTrackingClient tagTrackingClient = new TagTrackingClient();
        tagTrackingClient.startConnection(LOCAL_HOST, port);

        String request1 = "{\"user\": \"Siri\", \"add\": [], \"remove\": [\"jojo\"], \"timestamp\": \"2018-08-10T06:49:04.420Z\"}";
        String request2 = "{\"user\": \"Siri\", \"add\": [\"jojo\"], \"remove\": [], \"timestamp\": \"2018-08-10T06:49:04.410Z\"}";

        String testJsonMsg1 = tagTrackingClient.sendMessage(request1);
        String testJsonMsg2 = tagTrackingClient.sendMessage(request2);

        String expectedJsonOutput = "{\"user\":\"Siri\",\"tags\":[]}";

        assertEquals(expectedJsonOutput, testJsonMsg1);
        assertEquals(expectedJsonOutput, testJsonMsg2);

        tagTrackingClient.stopConnection();
    }


    @Test
    public void testServerCapacity() {
        String jsonInput = "{\"user\": \"Secret Squirrel\", \"add\": [\"beyhive_member\", \"timbers_army\", \"jojo\", \"maodan\"], \"remove\": [\"maodan\"], \"timestamp\": \"2018-08-10T06:49:04.430Z\"}";
        String expectedJsonOutput = "{\"user\":\"Secret Squirrel\",\"tags\":[\"beyhive_member\",\"timbers_army\",\"jojo\"]}";

        final int clientNum = 100;

        List<TagTrackingClient> tagTrackingClients = new ArrayList<>();
        IntStream.range(0, clientNum)
                .forEach(i -> {
                    TagTrackingClient tagTrackingClient = new TagTrackingClient();
                    tagTrackingClient.startConnection(LOCAL_HOST, port);
                    tagTrackingClients.add(tagTrackingClient);
                });

        tagTrackingClients.forEach(connectedClient -> {
            String jsonMsg = connectedClient.sendMessage(jsonInput);
            assertEquals(expectedJsonOutput, jsonMsg);
        });

        tagTrackingClients.forEach(TagTrackingClient::stopConnection);
    }

}
