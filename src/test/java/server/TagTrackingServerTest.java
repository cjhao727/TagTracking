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
import java.util.stream.Collectors;
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
    public void givenInValidJsonString() {
        TagTrackingClient tagTrackingClient = new TagTrackingClient();
        tagTrackingClient.startConnection(LOCAL_HOST, port);

        String testJsonMsg = tagTrackingClient.sendMessage("({})");
        String expectedJsonOutput = "{\"error\":\"java.lang.IllegalStateException: Expected BEGIN_OBJECT but was STRING at line 1 column 1 path $\"}";
        assertEquals(expectedJsonOutput, testJsonMsg);
        tagTrackingClient.stopConnection();
    }

    @Test
    public void givenSingleRequest() {
        TagTrackingClient tagTrackingClient = new TagTrackingClient();
        tagTrackingClient.startConnection(LOCAL_HOST, port);

        String jsonInput = "{\"user\": \"Secret Squirrel\", \"add\": [\"beyhive_member\", \"timbers_army\", \"jojo\", \"maodan\"], \"remove\": [\"maodan\"], \"timestamp\": \"2018-08-10T06:49:04.430Z\"}";
        String jsonMsg = tagTrackingClient.sendMessage(jsonInput);

        String jsonOutput = "{\"user\":\"Secret Squirrel\",\"tags\":[\"beyhive_member\",\"timbers_army\",\"jojo\"]}";

        assertEquals(jsonOutput, jsonMsg);

        tagTrackingClient.stopConnection();
    }

    @Test
    public void givenMultipleRequestsInOrder() {
        TagTrackingClient tagTrackingClient = new TagTrackingClient();
        tagTrackingClient.startConnection(LOCAL_HOST, port);

        String jsonInput = "{\"user\": \"Secret Squirrel\", \"add\": [\"beyhive_member\", \"timbers_army\", \"jojo\", \"maodan\"], \"remove\": [\"maodan\"], \"timestamp\": \"2018-08-10T06:49:04.430Z\"}";
        String jsonInput2 = "{\"user\": \"Secret Squirrel\", \"add\": [\"beyhive_member\", \"timbers_army\", \"jojo\", \"maodan\"], \"remove\": [\"jojo\"], \"timestamp\": \"2018-08-10T06:49:04.440Z\"}";
        String jsonMsg = tagTrackingClient.sendMessage(jsonInput);
        String jsonMsg2 = tagTrackingClient.sendMessage(jsonInput2);

        String jsonOutput = "{\"user\":\"Secret Squirrel\",\"tags\":[\"beyhive_member\",\"timbers_army\",\"jojo\"]}";
        String jsonOutput2 = "{\"user\":\"Secret Squirrel\",\"tags\":[\"beyhive_member\",\"timbers_army\",\"maodan\"]}";

        assertEquals(jsonOutput, jsonMsg);
        assertEquals(jsonOutput2, jsonMsg2);

        tagTrackingClient.stopConnection();
    }

    @Test
    public void givenMultipleRequestsOutOfOrder() {
        TagTrackingClient tagTrackingClient = new TagTrackingClient();
        tagTrackingClient.startConnection(LOCAL_HOST, port);

        String request1 = "{\"user\": \"Siri\", \"add\": [\"jojo\"], \"remove\": [], \"timestamp\": \"2018-08-10T06:49:04.420Z\"}";
        String request2 = "{\"user\": \"Siri\", \"add\": [\"jojo\"], \"remove\": [], \"timestamp\": \"2018-08-10T06:49:04.410Z\"}";
        String request3 = "{\"user\": \"Siri\", \"add\": [], \"remove\": [\"jojo\"], \"timestamp\": \"2018-08-10T06:49:04.415Z\"}";

        String testJsonMsg1 = tagTrackingClient.sendMessage(request1);
        String testJsonMsg2 = tagTrackingClient.sendMessage(request2);
        String testJsonMsg3 = tagTrackingClient.sendMessage(request3);

        String expectedJsonOutput = "{\"user\":\"Siri\",\"tags\":[\"jojo\"]}";

        assertEquals(expectedJsonOutput, testJsonMsg1);
        assertEquals(expectedJsonOutput, testJsonMsg2);
        assertEquals(expectedJsonOutput, testJsonMsg3);

        tagTrackingClient.stopConnection();
    }

    @Test
    public void givenMultipleRequestsOutOfOrder2() {
        TagTrackingClient tagTrackingClient = new TagTrackingClient();
        tagTrackingClient.startConnection(LOCAL_HOST, port);

        String jsonInput2 = "{\"user\": \"Secret Squirrel\", \"add\": [\"beyhive_member\", \"timbers_army\", \"jojo\", \"maodan\"], \"remove\": [\"jojo\"], \"timestamp\": \"2018-08-10T06:49:04.440Z\"}";
        String jsonInput = "{\"user\": \"Secret Squirrel\", \"add\": [\"beyhive_member\", \"timbers_army\", \"jojo\", \"maodan\"], \"remove\": [\"maodan\"], \"timestamp\": \"2018-08-10T06:49:04.430Z\"}";

        String jsonMsg2 = tagTrackingClient.sendMessage(jsonInput2);
        String jsonMsg = tagTrackingClient.sendMessage(jsonInput);

        String jsonOutput = "{\"user\":\"Secret Squirrel\",\"tags\":[\"beyhive_member\",\"timbers_army\",\"maodan\"]}";

        assertEquals(jsonOutput, jsonMsg2);
        assertEquals(jsonOutput, jsonMsg);


        tagTrackingClient.stopConnection();
    }

    @Test
    public void givenDelayedRequests() {
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
        String jsonOutput = "{\"user\":\"Secret Squirrel\",\"tags\":[\"beyhive_member\",\"timbers_army\",\"jojo\"]}";

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
            //System.out.println(jsonMsg); - verify out put.
            assertEquals(jsonOutput, jsonMsg);
        });

        tagTrackingClients.forEach(TagTrackingClient::stopConnection);
    }

}
