package tk.pseudonymous.myvahome;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    private static final int SPEECH_REQUEST_CODE = 0;
    public static TextView infoText, resultText;
    public static EditText IPADD, PORTADD, USERT, PASST;
    public static ImageView icons;
    public static String IP = "192.168.1.122", USER="admin", PASS="admin";
    public static int PORT = 9111;
    public static String rootDir = "";
    public static Socket sock;
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
// Print "true" even though the internet permission is not defined in the manifest file
        Log.d(this.getClass().getName(), "Internet permission granted: " + (result == PackageManager.PERMISSION_GRANTED));


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
        new GetRequest().execute(IP, String.valueOf(PORT), "environments", USER, PASS);
        //new TcpClient().execute(command);

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
            String toDisplay = "Loading image...";
            resultText.setText(toDisplay);

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

    private class GetRequest extends AsyncTask<String, String, String> {
        protected String doInBackground(String... urls) {
            try {
                publishProgress("Loading...");
                JSONObject toParse = RESTful.GetJsonBody(urls[0], urls[1],
                        urls[2], urls[3], urls[4]);



                return "Made it";
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onProgressUpdate(String... progress) {
            resultText.setText(progress[0]);
        }

        protected void onPostExecute(String result) {
            resultText.setText(result);
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
}
