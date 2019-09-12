package com.example.myvoiceapp;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telecom.Call;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TimeUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.media.session.PlaybackState.ACTION_PLAY;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final String ACTION_VOICE_SEARCH = "com.google.android.gms.actions.SEARCH_ACTION";
    private static final int PICK_CONTACT = 1;
    private SearchView searchView;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecog;
    TextView firstNumTextView;
    TextView secondNumTextView;
    TextView operatorTextView;
    private int FIRST_NUMBER;
    private int SECOND_NUMBER;
    private char OPERATOR;
    private int RESULT;
    private Call call;
    private String cNumber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View view) {
                // Here, thisActivity is the current activity
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Permission is not granted
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.RECORD_AUDIO)) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                    } else {
                        // No explanation needed; request the permission
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.RECORD_AUDIO},MY_PERMISSIONS_REQUEST_RECORD_AUDIO);

                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                } else {
                    // Permission has already been granted
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
                    speechRecog.startListening(intent);
                    RESULT = performCalculations();
                }
            }
        });

        initializeTextToSpeech();
        initializeSpeechRecognizer();
    }


    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecog = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecog.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {

                }

                @Override
                public void onBeginningOfSpeech() {

                }

                @Override
                public void onRmsChanged(float rmsdB) {

                }

                @Override
                public void onBufferReceived(byte[] buffer) {

                }

                @Override
                public void onEndOfSpeech() {

                }

                @Override
                public void onError(int error) {

                }

                @Override
                public void onResults(Bundle results) {
                    List<String> result_arr = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    processResult(result_arr.get(0));

                }

                @Override
                public void onPartialResults(Bundle partialResults) {

                }

                @Override
                public void onEvent(int eventType, Bundle params) {

                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case 10:
                    int intFound = getNumberFromResult(data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS));
                    if (intFound != -1) {
                        FIRST_NUMBER = intFound;
                        firstNumTextView.setText(intFound);
                    } else {
                        Toast.makeText(getApplicationContext(), "Sorry, I didn't catch that! Please try again", Toast.LENGTH_LONG).show();
                    }
                    break;
                case 20:
                    intFound = getNumberFromResult(data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS));
                    if (intFound != -1) {
                        SECOND_NUMBER = intFound;
                        secondNumTextView.setText(intFound);
                    } else {
                        Toast.makeText(getApplicationContext(), "Sorry, I didn't catch that! Please try again", Toast.LENGTH_LONG).show();
                    }
                    break;
                case 30:
                    char operatorFound = getOperatorFromResult(data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS));
                    if (operatorFound != '0') {
                        OPERATOR = operatorFound;
                        operatorTextView.setText(operatorFound);
                    } else {
                        Toast.makeText(getApplicationContext(), "Sorry, I didn't catch that! Please try again", Toast.LENGTH_LONG).show();
                    }
            }
        } else {
        }

        int reqCode =1;
        switch (reqCode) {
            case (PICK_CONTACT) :
                if (resultCode == Activity.RESULT_OK) {

                    Uri contactData = data.getData();
                    Cursor c =  managedQuery(contactData, null, null, null, null);
                    if (c.moveToFirst()) {


                        String id =c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));

                        String hasPhone =c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                        if (hasPhone.equalsIgnoreCase("1")) {
                            Cursor phones = getContentResolver().query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ id,
                                    null, null);
                            phones.moveToFirst();
                            cNumber = phones.getString(phones.getColumnIndex("data1"));
                            System.out.println("number is:"+cNumber);
                        }
                        String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));


                    }
                }
                break;
        }
    }


    private int getNumberFromResult(ArrayList<String> results) {
        for (String str : results) {
            if (getIntNumberFromText(str) != -1) {
                return getIntNumberFromText(str);
            }
        }
        return -1;
    }

    // method to convert string number to integer
    private int getIntNumberFromText(String strNum) {
        switch (strNum) {
            case "zero":
                return 0;
            case "one":
                return 1;
            case "two":
                return 2;
            case "three":
                return 3;
            case "four":
                return 4;
            case "five":
                return 5;
            case "six":
                return 6;
            case "seven":
                return 7;
            case "eight":
                return 8;
            case "nine":
                return 9;
        }
        return -1;
    }

    private char getOperatorFromResult(ArrayList<String> results) {
        for (String str : results) {
            if (getCharOperatorFromText(str) != '0') {
                return getCharOperatorFromText(str);
            }
        }
        return '0';
    }

    // method to convert string operator to char
    private char getCharOperatorFromText(String strOper) {
        switch (strOper) {
            case "plus":
            case "add":
                return '+';
            case "minus":
            case "subtract":
                return '-';
            case "times":
            case "multiply":
                return '*';
            case "divided by":
            case "divide":
                return '/';
            case "power":
            case "raised to":
                return '^';
        }
        return '0';
    }

    private int performCalculations() {
        switch (OPERATOR) {
            case '+':
                return FIRST_NUMBER + SECOND_NUMBER;
            case '-':
                return FIRST_NUMBER - SECOND_NUMBER;
            case '*':
                return FIRST_NUMBER * SECOND_NUMBER;
            case '/':
                return FIRST_NUMBER / SECOND_NUMBER;
            case '^':
                return FIRST_NUMBER ^ SECOND_NUMBER;
        }
        return -999;
    }

    private void processResult(String result_message) {
        result_message = result_message.toLowerCase();

        if(result_message.indexOf("what") != -1){
            if(result_message.indexOf("your name") != -1){
                speak("I'm Gideon. Nice to meet you!");
            }
            if (result_message.indexOf("time") != -1){
                String time_now = DateUtils.formatDateTime(this, new Date().getTime(),DateUtils.FORMAT_SHOW_TIME);
                speak("The time is now: " + time_now);
            }
        } else if (result_message.indexOf("earth") != -1){
            speak("Don't be silly, The earth is a sphere. As are all other planets and celestial bodies");


        } else if (result_message.indexOf("youtube") != -1){
            speak("Right away");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com"));
            startActivity(intent);

        } else if (result_message.indexOf("created") != -1){
                speak("I am created in Java but I love the Scorpion language !!");


        } else if (result_message.indexOf("scorpion") != -1){
            if(result_message.indexOf("language") != -1) {
                speak("It's a language that is created only by my master. Also known as the best language to be recorded in the human history.");
            }
        } else if (result_message.indexOf("tell") != -1){
            if(result_message.indexOf("me anything") != -1) {
                speak("What should I say, please ask me a question and I will help !");
            }

        } else if (result_message.indexOf("how") != -1){
            if(result_message.indexOf("old are") != -1) {
                speak("I am a Millennial year old.");
            }

        } else if (result_message.indexOf("who") != -1){
            if(result_message.indexOf("your master") != -1) {
                speak("My master is... Sorry I cannot find what you are looking for. Please ask me something else.");
            }

        } else if (result_message.indexOf("do") != -1){
            if(result_message.indexOf("you know") != -1) {
                speak("I know everything my master knows.");
            }

        } else if (result_message.indexOf("maps") != -1){
                speak("Right away");
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps"));
                startActivity(intent);

        } else if (result_message.indexOf("linkedin") != -1){
            speak("Right away");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://uk.linkedin.com/"));
            startActivity(intent);

        } else if (result_message.indexOf("instagram") != -1){
            speak("Right away");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/"));
            startActivity(intent);

        } else if (result_message.indexOf("created") != -1){
            speak("I was created to do good in this life and the next.");


        } else if (result_message.indexOf("weather") != -1) {
            speak("Right away.");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://weather.com/en-GB/weather/today/l/51.55,-0.37?par=google"));
            startActivity(intent);

        } else if (result_message.indexOf("facebook") != -1) {
            speak("Right away.");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/"));
            startActivity(intent);

        } else if (result_message.indexOf("messenger") != -1) {
            speak("Right away.");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.messenger.com/"));
            startActivity(intent);

        } else if (result_message.indexOf("email") != -1) {
            speak("Right away.");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gmail.com/"));
            startActivity(intent);

        } else if (result_message.indexOf("search") != -1) {
            speak("Right away.");
            Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND);
            if (intent != null && ACTION_VOICE_SEARCH.equals(intent.getAction())) {
                String query = intent.getStringExtra(SearchManager.QUERY);
                setSearchViewVisible(true);
                searchView.setQuery(query, true);
            }
            startActivity(intent);


        } else if (result_message.indexOf("mail") != -1) {
            if(result_message.indexOf("send") != -1) {
                speak("Right away.");
                Intent mailIntent = new Intent(Intent.ACTION_VIEW);
                Uri data = Uri.parse("mailto:?subject=" + " " + "&body=" + " " + "&to=" + " ");
                mailIntent.setData(data);
                startActivity(Intent.createChooser(mailIntent, "Send mail..."));
            }

        } else if (result_message.indexOf("contacts") != -1) {
            speak("Right away.");
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, PICK_CONTACT);


        }else if (result_message.indexOf("call") != -1) {
            if(result_message.indexOf("bato") != -1) {
                speak("Right away.");
                Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel: 07453953766"));

                callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    //request permission from user if the app hasn't got the required permission
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CALL_PHONE},   //request specific permission from user
                            10);
                    return;
                }else {     //have got permission
                    try{
                        startActivity(callIntent);  //call activity and make phone call
                    }
                    catch (ActivityNotFoundException ex){
                        Toast.makeText(getApplicationContext(),"yourActivity is not founded",Toast.LENGTH_SHORT).show();
                    }
                }
                    startActivity(callIntent);
                }

        } else if (result_message.indexOf("recent") != -1) {
                speak("Right away.");
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                // BoD con't: CONTENT_TYPE instead of CONTENT_ITEM_TYPE
                intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                startActivityForResult(intent, 1);

                if (intent != null) {
                    Uri uri = intent.getData();

                    if (uri != null) {
                        Cursor c = null;
                        try {
                            c = getContentResolver().query(uri, new String[]{
                                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                                            ContactsContract.CommonDataKinds.Phone.TYPE},
                                    null, null, null);

                            if (c != null && c.moveToFirst()) {
                                String number = c.getString(0);
                                int type = c.getInt(1);
                                showSelectedNumber(type, number);
                            }
                        } finally {
                            if (c != null) {
                                c.close();
                            }
                        }
                    }
                }

        }else if (result_message.indexOf("calculate") != -1) {

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
            startActivityForResult(intent, 10);

            Intent intent1 = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent1.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent1.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
            startActivityForResult(intent1, 20);

            Intent intent2 = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent2.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent2.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
            startActivityForResult(intent2, 30);


        } else if (result_message.indexOf("browser") != -1){
            speak("Right away");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com"));
            startActivity(intent);
        }
    }

    private void showSelectedNumber(int type, String number) {
        Toast.makeText(this, type + ": " + number, Toast.LENGTH_LONG).show();
    }

    private void initializeTextToSpeech() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (tts.getEngines().size() == 0 ){
                    Toast.makeText(MainActivity.this, getString(R.string.tts_no_engines),Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    tts.setLanguage(Locale.US);
                    speak("I'm Gideon. How can I help you.");
                }
            }
        });
    }

    private void setSearchViewVisible(boolean visible) {

        if (searchView.isIconified() == visible) {
            searchView.setIconified(!visible);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(visible);
        }
    }

    private void speak(String message) {
        if(Build.VERSION.SDK_INT >= 21){
            tts.speak(message,TextToSpeech.QUEUE_FLUSH,null,null);
        } else {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH,null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        tts.shutdown();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        Reinitialize the recognizer and tts engines upon resuming from background such as after openning the browser
        initializeSpeechRecognizer();
        initializeTextToSpeech();
    }

}
