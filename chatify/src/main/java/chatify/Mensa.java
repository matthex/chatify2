package chatify;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.http.HttpResponse;

public class Mensa
{
    private static String baseUrl = "http://www.mensa-kl.de/api.php";

    public JSONObject getMailsForToday() {
        try {
            HttpResponse<String> response = Unirest.get(baseUrl + "?date=0&role=B&format=json").asString();
            try {
                JSONArray mensaRaw = new JSONArray(response.getBody());
                JSONObject mensaJson = new JSONObject();
                String foodString = "Heute gibt es in der Mensa:\n";
                for (int i = 0; i < mensaRaw.length(); i++) {
                    JSONObject counter = mensaRaw.getJSONObject(i);
                    foodString += "Ausgabe " + counter.getString("loc") + ": `" + counter.getString("title") + "`\n";
                }
                mensaJson.put("success", foodString);
                return mensaJson;
            } catch (JSONException e) {
                JSONObject msgJson = new JSONObject();
                msgJson.put("error", "Kein Essen verfügbar.");
                return msgJson;
            }
        } catch (UnirestException e) {
            JSONObject msgJson = new JSONObject();
            msgJson.put("error", "Fehler beim Abrufen des Essensplans.");
            return msgJson;
        }
    }
}