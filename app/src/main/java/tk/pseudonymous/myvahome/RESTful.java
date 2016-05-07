package tk.pseudonymous.myvahome;

import android.util.Log;

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

    public static JSONObject GetJsonBody(String ip, String port, String environ, String user, String pass) throws JSONException {
        String response = String.valueOf(HttpRequest.get("http://192.168.1.122:9111/v3/environments/")// + ip + ":" + port + "/v3/" + environ)
                //.accept("application/json")
                .acceptJson()
                .basic(user, pass)
                .body());
        Log.d("GOT_RESPONSE_PLEASE", response);
        {
            JSONObject toRet;
            Object jsoner = new JSONTokener(response).nextValue();
            if (jsoner instanceof JSONArray) {
                toRet = new JSONObject(new JSONArray(response).get(0).toString());
            } else {
                toRet = new JSONObject(response);
            }
            return toRet;
        }
    }

}
