package chatify;

import java.net.URI;
import java.net.URISyntaxException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import io.github.cdimascio.dotenv.Dotenv;

import org.json.JSONObject;
import org.json.JSONArray;

import org.apache.commons.codec.digest.DigestUtils;

public class RocketChatChatBot extends WebSocketClient 
{
    static private Dotenv dotenv;
    static private WebSocketClient client;

    public RocketChatChatBot(URI serverUri, Draft draft) {
		super(serverUri, draft);
    }
    
    public RocketChatChatBot(URI serverUri) {
        super(serverUri);
    }
    
    @Override
	public void onOpen(ServerHandshake handshakedata) {
        System.out.println("new connection opened");
        chatConnect();
        chatLogin();
        subscribeRoom();
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		System.out.println("closed with exit code " + code + " additional info: " + reason);
	}

	@Override
	public void onMessage(String message) {
        System.out.println("received message: " + message);
        JSONObject messageJson = new JSONObject(message);
        if(messageJson.has("msg")) {
            switch(messageJson.getString("msg")) {
                case "ping":
                    JSONObject pongJson = new JSONObject();
                    pongJson.put("msg", "pong");
                    sendMessage(pongJson.toString());
                    break;
                case "changed":
                    switch(messageJson.getJSONObject("fields").getJSONArray("args").getJSONObject(0).getString("msg")) {
                        case "!mensa":
                        Mensa mensa = new Mensa();
                            JSONObject mensaJson = mensa.getMailsForToday();
                            if(mensaJson.has("error")) {
                                JSONObject msgJson = new JSONObject();
                                msgJson.put("msg", mensaJson.getString("error"));
                                sendMessage(msgJson.toString());
                            } else {
                                sendRoomMessage(mensaJson.getString("success"), messageJson.getJSONObject("fields").getJSONArray("args").getJSONObject(0).getString("rid"));
                            }
                            break;
                    }
                    break;
            }
        }
	}

	@Override
	public void onError(Exception ex) {
		System.err.println("an error occurred:" + ex);
    }

    public static void sendMessage(String message) {
        System.out.println("sent message: " + message);
        client.send(message);
    }

    public static void sendRoomMessage(String message, String room) {
        System.out.println("sent message: " + message + " to " + room);
        //Prepare JSON
        JSONObject msgJson = new JSONObject();
        msgJson.put("msg", "method");
        msgJson.put("method", "sendMessage");
        msgJson.put("id", "42");
        JSONObject msgDetails = new JSONObject();
        msgDetails.put("_id", generateId(10));
        msgDetails.put("rid", room);
        msgDetails.put("msg", message);
        JSONArray params = new JSONArray();
        params.put(msgDetails);
        msgJson.put("params", params);

        client.send(msgJson.toString());
    }

    public static void subscribeRoom() {
        //Prepare JSON
        JSONObject subJson = new JSONObject();
        subJson.put("msg", "sub");
        subJson.put("id", "43");
        subJson.put("name", "stream-room-messages");
        JSONArray params = new JSONArray();
        params.put(dotenv.get("ROOM"));
        params.put(false);
        subJson.put("params", params);

        //Subscribe
        sendMessage(subJson.toString());
    }
    
    public static void chatLogin() {
        //Prepare JSON
        JSONObject loginJson = new JSONObject();
        loginJson.put("msg", "method");
        loginJson.put("method", "login");
        loginJson.put("id", "42");
        JSONObject user = new JSONObject();
        user.put("username", dotenv.get("USER"));
        JSONObject password = new JSONObject();
        password.put("digest", DigestUtils.sha256Hex(dotenv.get("PASSWORD")));
        password.put("algorithm", "sha-256");
        JSONObject loginDetails = new JSONObject();
        loginDetails.put("user", user);
        loginDetails.put("password", password);
        JSONArray params = new JSONArray();
        params.put(loginDetails);
        loginJson.put("params", params);

        //Login
        sendMessage(loginJson.toString());
    }

    public static void chatConnect() {
        //Prepare JSON
        JSONObject connectJson = new JSONObject();
        connectJson.put("msg", "connect");
        connectJson.put("version", "1");
        JSONArray support = new JSONArray();
        support.put("1");
        connectJson.put("support", support);

        //Connect
        sendMessage(connectJson.toString());
    }

    public static String generateId(int n) {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                                    + "0123456789"
                                    + "abcdefghijklmnopqrstuvxyz"; 
        StringBuilder sb = new StringBuilder(n); 
        for (int i = 0; i < n; i++) { 
            int index = (int)(AlphaNumericString.length() * Math.random()); 
            sb.append(AlphaNumericString.charAt(index)); 
        }
        return sb.toString();
    }

    public static void main( String[] args ) throws URISyntaxException
    {
        dotenv = Dotenv.load();
        String socketUrl = dotenv.get("SOCKETURL");
        client = new RocketChatChatBot(new URI(socketUrl));
        client.connect();
    }
}
