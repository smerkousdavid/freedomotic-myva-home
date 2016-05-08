package tk.pseudonymous.myvahome;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RawRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.android.gms.appindexing.Action;
//import com.google.android.gms.appindexing.AppIndex;
//import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.MemoryHandler;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    private static final int SPEECH_REQUEST_CODE = 0;
    public static TextView infoText, resultText;
    public static EditText IPADD, PORTADD, USERT, PASST;
    public static ImageView icons;
    public static String IP = "192.168.1.122", USER="admin", PASS="admin";
    public static int PORT = 9111;
    public static String rootDir = "";
    public static InputStream procStream = null;
    public static Socket sock;

    public static boolean runner = false;
    //private GoogleApiClient client;
    public static double lat = 0;
    public static double lng = 0;
    public static TextToSpeech speak;
    public static final int CHECK_TTS_AVAILABILITY = 101;
    private static final String TAG = "ActivityTTS";
    public static Bitmap bitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        infoText = (TextView) findViewById(R.id.infoText);
        resultText = (TextView) findViewById(R.id.results);
        if(resultText != null) resultText.setGravity(Gravity.CENTER);
        icons = (ImageView) findViewById(R.id.iconView);
        IPADD = (EditText) findViewById(R.id.ipText);
        PORTADD = (EditText) findViewById(R.id.portText);
        USERT = (EditText) findViewById(R.id.UserText);
        PASST = (EditText) findViewById(R.id.PassText);

        String permission = "android.permission.INTERNET";
        int result = getApplicationContext().checkCallingOrSelfPermission(permission);
        Log.d(this.getClass().getName(), "Internet permission granted: "
                + (result == PackageManager.PERMISSION_GRANTED));


        rootDir = getApplicationContext().getFilesDir().getPath()+"/home.properties";
        try {
            File file = new File(rootDir);
            if((file.createNewFile()))
                Toast.makeText(getApplicationContext(),
                        "Hello welcome to Home Automation!",
                        Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "ERROR: couldn't create properties",
                    Toast.LENGTH_SHORT).show();
        }

        new props().execute("");

        try {
            Intent checkTTS = new Intent();
            checkTTS.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            startActivityForResult(checkTTS, CHECK_TTS_AVAILABILITY);
        } catch (Throwable party) {
            Log.d("Sent", "Already bound");
        }

        //client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    /*private InputStream openProc() {
        return getResources().openRawResource(getResources()
                .getIdentifier("epc", "raw", getPackageName()));
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {

            if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                onText(results.get(0));
            }

            //Log.d(TAG, "TTS Response: "+requestCode);
            if (requestCode == CHECK_TTS_AVAILABILITY) {
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {

                    // success, create the TTS instance
                    speak = new TextToSpeech(this, this);

                } else {
                    // missing data, install it
                    String NO_TTS_ANDROID_MARKET_REDIRECT = "'SpeechSynthesis Data Installer' is not installed on your system, you are being redirected to" +
                            " the installer package. You may also be able to install it my going to the 'Home Screen' then " +
                            "(Menu -> Settings -> Voice Input & output -> Text-to-speech settings)";
                    Toast.makeText(this, NO_TTS_ANDROID_MARKET_REDIRECT, Toast.LENGTH_LONG).show();

                    PackageManager pm = getPackageManager();
                    Intent installIntent = new Intent();
                    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    ResolveInfo resolveInfo = pm.resolveActivity( installIntent, PackageManager.MATCH_DEFAULT_ONLY );

                    if( resolveInfo == null ) {
                        // Not able to find the activity which should be started for this intent
                        String NO_TTS_AVAILABLE = "'SpeechSynthesis Data Installer' is not available on your system, " +
                                "you may have to install it manually yourself. You may also be able to install it my going to the 'Home Screen' " +
                                "then (Menu -> Settings -> Voice Input & output -> Text-to-speech settings)";
                        Toast.makeText(this, NO_TTS_AVAILABLE, Toast.LENGTH_LONG).show();
                    } else {
                        startActivity( installIntent );
                    }

                    finish();
                }
            }
        }catch (Exception e) {
            Log.e(TAG, "Unable to access service");
            finish();
        }

    }

    public void onText(String command)
    {
        String toDisplay = "C: " + command;
        infoText.setText(toDisplay);
        Object processed[] = processText(command);
        new GetRequest().execute(IP, String.valueOf(PORT), String.valueOf(processed[0]),
                USER, PASS, String.valueOf(processed[1]), String.valueOf(processed[2]));
        //new TcpClient().execute(command);

    }

    public boolean isAfter(String whole, String first, String second) {
        return (whole.indexOf(first) > whole.indexOf(second));
    }

    public Object[] processText(String toproc) {
        Object toRet[] = {"/commands/user", "GET", toproc};
        if(toproc.contains("shutdown") && toproc.contains("freedomotic")) {
            if(isAfter(toproc, "freedomotic", "shutdown")) {
                Log.d("SHUTDOWN", "TRUE");
                toRet[0] = "/system/exit";
                toRet[1] = "POST";
                toRet[2] = "{}";
            }
        } else if(toproc.contains("version") && toproc.contains("freedomotic")) {
            if(isAfter(toproc, "version", "freedomotic")) {
                Log.d("VERSION", "TRUE");
                toRet[0] = "/system/info/framework";
                toRet[1] = "GET";
                toRet[2] = "{}";
            }
        }
        return toRet;
    }

    public static boolean contains(String full, String container)
    {
        return full.contains("::" + container + "::");
    }


    public static String cuTTer(String full, String start, String stop)
    {
        start = "::"+start+"::";
        stop = "::"+stop+"::";
        return full.substring((full.indexOf(start) + (start.length())), full.indexOf(stop));
    }

    public void OnRecieveTCP(String command)
    {
        if(!command.contains("END"))
            return;

        boolean passed = false;

        if(contains(command, "SPEAKS"))
        {
            String toSpeak = cuTTer(command, "SPEAKS", "END:SPEAKS");
            speak.stop();
            speak.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            passed = true;
        }
        if(contains(command, "ICONS"))
        {
            String URLs = cuTTer(command, "ICONS", "END:ICONS");
            new LoadImage().execute(URLs);
            passed = true;
        }
        if(contains(command, "MAPS") && contains(command, "LAT"))
        {
            String toReturn = "Opening Map...";
            resultText.setText(toReturn);
            try {
                lat = Double.valueOf(cuTTer(command, "MAPS", "LAT"));
                lng = Double.valueOf(cuTTer(command, "LAT", "END:MAPS"));
            } catch (NullPointerException f) {
                lat = 0;
                lng = 0;
            }
            //Intent maps = new Intent(this, MapsActivity.class);
            //startActivity(maps);
            passed = true;
        }
        if(contains(command, "FLASHING"))
        {
            try {
                resultText.setText(cuTTer(command, "FLASHING", "END:FLASHING"));
            }catch (StringIndexOutOfBoundsException ig)
            {
                String toDisplay = "ERROR: parsing command!";
                resultText.setText(toDisplay);
                Toast.makeText(getApplicationContext(), "Make sure that you use the library " +
                                "provided and try not to mix commands as much as possible",
                        Toast.LENGTH_LONG).show();
            }
            passed = true;
        }
        if(!passed)
        {
            String toDisplay = "ERROR: receiving!";
            resultText.setText(toDisplay);
        }
    }

    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS)
        {
            speak.setLanguage(Locale.getDefault());
        }
        else
        {
            Toast.makeText(getApplicationContext(), "ERROR: Failed to load text to speech "+
                    "engine", Toast.LENGTH_SHORT).show();
        }
    }

    private void Speech() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }*/

    public void buttonGet(View view)
    {
        String toDisplay = "Ready!";
        resultText.setText(toDisplay);
        speak.stop();
        Object newVal[] = {null, null, null, null};
        newVal[0] = IPADD.getText().toString();
        newVal[1] = Integer.valueOf(PORTADD.getText().toString());
        newVal[2] = USERT.getText().toString();
        newVal[3] = PASST.getText().toString();
        new displayGet().execute(newVal);
    }

    @Override
    public void onStart() {
        super.onStart();
        //client.connect();
        //Action viewAction = Action.newAction(Action.TYPE_VIEW,
        // "Main Page", Uri.parse("http://host/path"),
        // Uri.parse("android-app://com.smerkous.david.homeautomation/http/host/path")
        //);
        //AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onDestroy() {

        if(speak != null) {
            speak.stop();
            speak.shutdown();
        }

        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
        speak.stop();
        //Action viewAction = Action.newAction(Action.TYPE_VIEW,
        //        "Main Page", Uri.parse("http://host/path"),
        //      Uri.parse("android-app://com.smerkous.david.homeautomation/http/host/path")
        //  );
        // AppIndex.AppIndexApi.end(client, viewAction);
        // client.disconnect();
    }

    private class LoadImage extends AsyncTask<String, String, Bitmap> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(MainActivity.this, "Loading image...",
                    Toast.LENGTH_SHORT).show();
            //String toDisplay = "Loading image...";
            //resultText.setText(toDisplay);

        }
        protected Bitmap doInBackground(String... args) {
            try {
                bitmap = BitmapFactory.decodeStream((InputStream) new URL(args[0]).getContent());
            } catch (Exception ignored) {}
            return bitmap;
        }

        protected void onPostExecute(Bitmap image) {
            if(image != null){
                icons.setImageBitmap(image);
            }else{
                Toast.makeText(MainActivity.this, "ERROR: loading requested image",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class displayGet extends AsyncTask<Object, Void, String> {
        protected String doInBackground(Object... vals) {
            if((vals[0] != IP) || (vals[1] != String.valueOf(PORT)) || (vals[2] != USER)
                    || (vals[3] != PASS)) {
                try {
                    OutputStream out = new FileOutputStream(rootDir);
                    Properties prop = new Properties();
                    prop.setProperty("IP", String.valueOf(vals[0]));
                    prop.setProperty("PORT", String.valueOf(vals[1]));
                    prop.setProperty("USER", String.valueOf(vals[2]));
                    prop.setProperty("PASS", String.valueOf(vals[3]));
                    IP = String.valueOf(vals[0]);
                    PORT = (Integer) vals[1];
                    USER = String.valueOf(vals[2]);
                    PASS = String.valueOf(vals[3]);
                    prop.store(out, "new");
                    out.close();
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "ERROR: properties not found",
                            Toast.LENGTH_SHORT).show();
                }
            }
            Speech();
            return "";
        }

        protected void onProgressUpdate(Void... progress) {
        }

        protected void onPostExecute(String result) {
        }
    }

    private class props extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... mone) {
            try {
                FileInputStream in = new FileInputStream(rootDir);
                Properties prop = new Properties();
                prop.load(in);
                in.close();
                IP = prop.getProperty("IP", "192.168.1.122");
                PORT = Integer.valueOf(prop.getProperty("PORT", "9111"));
                USER = prop.getProperty("USER", "admin");
                PASS = prop.getProperty("PASS", "admin");
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "ERROR: couldn't load properties",
                        Toast.LENGTH_SHORT).show();
            }
            return "";
        }

        protected void onProgressUpdate(Void... progress) {
        }

        protected void onPostExecute(String result) {
            IPADD.setText(IP);
            PORTADD.setText(String.valueOf(PORT));
            String toDisplay = "Ready!";
            resultText.setText(toDisplay);
        }
    }

    private class TcpClient extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... toSend) {
            String result;
            try {
                InetAddress address = InetAddress.getByName(IP);
                sock = new Socket(address, PORT);
                PrintWriter writer = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(sock.getOutputStream())), true);
                writer.println(Arrays.toString(toSend));
                writer.flush();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(sock.getInputStream()));
                result = reader.readLine();
                reader.close();
                writer.close();

            } catch (IOException ignored) {
                result = "ERROR connecting";
            }
            return result;
        }

        protected void onProgressUpdate(Void... progress) {
        }

        protected void onPostExecute(String result) {
            if(result != null) {
                OnRecieveTCP(result);
            }
            else
            {
                String toDisplay = "ERROR! got nothing";
                resultText.setText(toDisplay);
            }
        }
    }

    public String replaceList(String orig) {
        String first[] = {"living room", "closed", "bed room", "open this", "openers", "lights",
        "a front end", "simulator plug in", "rooms", "doors"};
        String second[] = {"livingroom", "close", "bedroom", "openness", "openness", "light",
        "Jfrontend", "simulator plugin", "room", "door"};
        for(int a = 0; a < first.length; a++) {
            orig = orig.replace(first[a], second[a]);
        }
        return orig;
    }


    private class GetRequest extends AsyncTask<String, String, String[]> {
        protected String[] doInBackground(String... urls) {
            if(!runner && Looper.myLooper() == null) {
                try {
                    Looper.prepare();
                } catch(Throwable ignored){}
                runner = true;
            }
            String toRet[] = {"", "", ""};
            try {
                publishProgress("Loading...");
                Object preParse = null;
                JSONObject toParse = null;
                if(Objects.equals(urls[5], "GET")) {
                     preParse = RESTful.GetJsonBody(urls[0], urls[1],
                            urls[2], urls[3], urls[4]);
                } else if(Objects.equals(urls[5], "POST")){
                    preParse = RESTful.SendGetJsonBody(urls[0], urls[1],
                            urls[2], urls[3], urls[4], urls[6]);
                }

                if(preParse != null)
                {
                    Log.d("MADE_IT", preParse.toString());
                    if(Objects.equals(urls[2], "/system/exit")) {
                        //Turn off the freedomotic framework
                        toParse = (JSONObject) preParse;
                        toRet[0] = ((int)toParse.get("status") == 202) ? "Shutdown complete" :
                                "Shutdown denied";
                        toRet[1] = toRet[0];
                        toRet[2] = "http://www.iconarchive.com/download/i89549/" +
                        "alecive/flatwoken/Apps-Dialog-Shutdown.ico";
                    } else if(Objects.equals(urls[2], "/system/info/framework")) {
                        //Get freedomotic information
                        toParse = (JSONObject) preParse;
                        toRet[0] = "'" + String.valueOf(toParse.get("FRAMEWORK_VERSION_CODENAME")) +
                                "' Version: " + String.valueOf(toParse.get("FRAMEWORK_MAJOR")) + "."
                                + String.valueOf(toParse.get("FRAMEWORK_MINOR"));
                        toRet[1] = "The current freedomotic release is " + toRet[0];
                        toRet[2] = "http://riscpi.co.uk/wp-content/uploads/2013/09/" +
                                "freedomotic_logo.png";
                    } else {
                        //Run user commands

                        JSONArray arr;
                        try {
                            arr = (JSONArray) preParse;
                        } catch(Throwable ignored) {
                            toRet[0] = "ERROR! bad response!";
                            return toRet;
                        }
                        List<String> idRun = new ArrayList<>();
                        int curNum = 0;
                        urls[6] = replaceList(urls[6].toLowerCase());
                        String comName = urls[6]
                                .replaceAll("[^A-Za-z0-9 ]", " ");//.split(" ");
                        //Iterate through all commands
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject curr = arr.getJSONObject(i);
                            //Log.d("KEY", curr.toString());

                            String single = curr.getString("name").toLowerCase()
                                    .replaceAll("[^A-Za-z0-9 ]", " ");
                            //Log.d("CURNAME", single);
                            String names[] = single.split(" ");
                            boolean pass = true;
                            for (String name : names) {
                                //Log.d("VALUE", name);
                                if(!name.isEmpty()) {
                                    if (!single.contains(comName)) {
                                        pass = false;
                                        //Log.d("failed", "Array failed parsing");
                                    }
                                }
                            }

                            if(pass) {
                                //Log.d("KEYADDING", single);
                                String uuidGet = curr.getString("uuid"); //Pull unique uuid commands
                                idRun.add(curNum, uuidGet);
                                curNum += 1;
                            }
                        }

                        Iterator<?> keys = idRun.iterator();
                        int failed = 0;

                        while( keys.hasNext() ) {
                            String key = (String)keys.next();
                            Log.d("FINALKEYS", key);
                            JSONObject ret = RESTful.SendGetJsonBody(urls[0], urls[1],
                                    "/commands/user/" + key + "/run", urls[3], urls[4], urls[6]);
                            if(ret.getInt("status") < 198 || ret.getInt("status") > 206) {
                                failed += 1;
                            }
                        }

                        if(failed == 0 && curNum != 0) {
                            if(curNum != 1) {
                                toRet[0] = "Ran " + String.valueOf(curNum) + " Successful commands";
                                toRet[1] = toRet[0] + " which includes " + urls[6];
                            } else {
                                toRet[0] = "Ran " + urls[6];
                                toRet[1] = toRet[0] + " Successfully";
                            }
                            toRet[2] = "http://www.iconsdb.com/icons/preview/green/" +
                                    "checkmark-xxl.png";
                        } else if(failed > 0) {
                            toRet[0] = String.valueOf(curNum - failed) + "\\" +
                                    String.valueOf(curNum) + " Ran successfully";
                            toRet[1] = String.valueOf(failed) + " Commands failed out of a total of"
                                    + " " + String.valueOf(curNum) + " Commands";
                            toRet[2] = "https://upload.wikimedia.org/wikipedia/commons/3/38/" +
                                    "Blank_space.png";
                        } else {
                            toRet[0] = "User command not found";
                            toRet[1] = "Command not found!";
                            toRet[2] = "http://ww2.justanswer.com/uploads/JO/jolinzh/" +
                                    "2011-5-14_121716_Blank2.64x64.gif";
                        }
                    }
                }



                return toRet;
            } catch (JSONException e) {
                Toast.makeText(MainActivity.this, "ERROR: getting response",
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return null;
            }
        }

        protected void onProgressUpdate(String... progress) {
            resultText.setText(progress[0]);
        }

        protected void onPostExecute(String[] result) {
            if(result != null) {
                //OnRecieveTCP(result);
                if(!Objects.equals(result[0], "")) resultText.setText(result[0]);
                if(!Objects.equals(result[1], "")) {
                    speak.stop();
                    speak.speak(result[1], TextToSpeech.QUEUE_FLUSH, null);
                }
                if(!Objects.equals(result[2], "")) new LoadImage().execute(result[2]);
            }
            else
            {
                String toDisplay = "ERROR! got no response";
                new LoadImage().execute("http://ww2.justanswer.com/uploads/JO/jolinzh/" +
                        "2011-5-14_121716_Blank2.64x64.gif");
                speak.stop();
                speak.speak("Freedomotic server must be down", TextToSpeech.QUEUE_FLUSH, null);
                resultText.setText(toDisplay);
            }
            runner = false;
        }
    }
}
