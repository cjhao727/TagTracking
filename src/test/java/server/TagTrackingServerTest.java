package server;

import com.j.client.TagTrackingClient;
import com.j.server.TagTrackingServer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

public class TagTrackingServerTest {
    private static int port;
    private static final String LOCALHOST = "127.0.0.1";

    @BeforeClass
    public static void start() throws InterruptedException, IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        serverSocket.close();

        Executors.newSingleThreadExecutor().submit(() -> new TagTrackingServer().start(port));
        Thread.sleep(500);
    }

    //single request - one user
    @Test
    public void givenJsonString() {
        TagTrackingClient tagTrackingClient = new TagTrackingClient();
        tagTrackingClient.startConnection(LOCALHOST, port);

        String jsonInput = "{\"user\": \"Secret Squirrel\", \"add\": [\"beyhive_member\", \"timbers_army\", \"jojo\", \"maodan\"], \"remove\": [\"maodan\"], \"timestamp\": \"2018-08-10T06:49:04.440Z\"}";
        String jsonMsg = tagTrackingClient.sendMessage(jsonInput);
        String terminate = tagTrackingClient.sendMessage(".");

        String jsonOutput = "{\"user\":\"Secret Squirrel\",\"tags\":[\"beyhive_member\",\"timbers_army\",\"jojo\"]}";
        assertEquals(jsonOutput, jsonMsg);
        assertEquals(terminate, "bye");
        tagTrackingClient.stopConnection();
    }

    @Test
    public void givenInValidJsonString() {
        TagTrackingClient tagTrackingClient = new TagTrackingClient();
        tagTrackingClient.startConnection(LOCALHOST, port);

//        String testJsonMsg = tagTrackingClient.sendMessage("({})");
//        String terminate = tagTrackingClient.sendMessage(".");
//
//        String expectedJsonOutput = "{\"error\":\"Expected BEGIN_OBJECT but was STRING at line 1 column 1 path $\"}";
//        assertEquals(expectedJsonOutput, testJsonMsg);
//        assertEquals(terminate, "bye");
//        tagTrackingClient.stopConnection();
    }

}
