package tk.pseudonymous.myvahome;

import android.util.Log;
import android.widget.Toast;

import com.github.kevinsawicki.http.HttpRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Created by David on 5/6/2016.
 * For the use of a static instance of RESTapi
 */

public class RESTful {

    public static Object GetJsonBody(String ip, String port, String environ, String user, String pass) throws JSONException {
        String response = "";
        try {
            response = String.valueOf(HttpRequest.get("http://" + ip + ":" + port + "/v3" + environ)
                    //.accept("application/json")
                    .acceptJson()
                    .basic(user, pass)
                    .body());
            Log.d("GOT_RESPONSE_PLEASE", response);
        } catch(Throwable ignored) {
            throw new JSONException("No response!");
        }
        {
            //JSONObject toRet;
            Object jsoner = new JSONTokener(response).nextValue();
            if (jsoner instanceof JSONArray) {
                return new JSONArray(response);
            } else {
                return new JSONObject(response);
            }
        }
    }

    public static JSONObject SendGetJsonBody(String ip, String port, String environ, String user, String pass, String toSend) throws JSONException {
        String response = "";
        try {
            response = String.valueOf(HttpRequest.post("http://" + ip + ":" + port + "/v3" + environ)
                    .acceptJson()
                    .basic(user, pass)
                    .code());
            Log.d("RESPONSE", "FINISHED");
            Log.d("GOT_RESPONSE_PLEASE", response);
        } catch (Throwable ignored) {
            throw new JSONException("No response!");
        }
            if (response.contains("{") && response.contains("}")) {
                JSONObject toRet;
                Object jsoner = new JSONTokener(response).nextValue();
                if (jsoner instanceof JSONArray) {
                    toRet = new JSONObject(new JSONArray(response).get(0).toString());
                } else {
                    toRet = new JSONObject(response);
                }
                return toRet;
            } else {
                return new JSONObject("{\"status\" : " + response + "}");
            }
    }

}
