package com.Madhav.solaire;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class chatActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TextView welcomeTextView;
    EditText messageEditText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;
    SpeechRecognizer speechRecognizer;
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final String USE_CASE_ID = "2";

    private String sendString = "";
    private int charactersPerLine = 18;
    private int previousNumberofLines = 1;
    private int currentNumberofLines = 0;
    private String previousText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle("chatGPT");

        messageList = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view);
        welcomeTextView = findViewById(R.id.welcome_text);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);

        //setup recycler view
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
                addToChat("Listening...",Message.SENT_BY_ME);
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle bundle) {
                messageList.remove(messageList.size()-1);
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                addToChat((data.get(0)),Message.SENT_BY_ME);
                sendButton.setImageResource(R.drawable.ic_baseline_mic);
                callAPI(data.get(0));
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        sendButton.setOnClickListener((v)->{
            //String question = messageEditText.getText().toString().trim();
            //addToChat(question,Message.SENT_BY_ME);
            //messageEditText.setText("");
            sendButton.setImageResource(R.drawable.ic_baseline_wait);
            speechRecognizer.startListening(speechRecognizerIntent);
            //callAPI(question);
            welcomeTextView.setVisibility(View.GONE);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_home:
                // User chose the "Home" item, show the app settings UI...
                Intent intent = new Intent(chatActivity.this, MainActivity.class);
                startActivity(intent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    void addToChat(String message,String sentBy){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message,sentBy));
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
            }
        });
    }

    void addResponse(String response){
        messageList.remove(messageList.size()-1);
        addToChat(response,Message.SENT_BY_BOT);
    }

    void callAPI(String question) {
        //okhttp
        messageList.add(new Message("Typing... ", Message.SENT_BY_BOT));

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "text-davinci-003");
            jsonBody.put("prompt", question);
            jsonBody.put("max_tokens", 4000);
            jsonBody.put("temperature", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/completions")
                .header("Authorization", "Bearer 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")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getString("text");
                        addResponse(result.trim());
                        BLEHelper.getInstance(chatActivity.this).write("\1" + "\22" + "\7");
                        sendData(USE_CASE_ID, result.trim());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                } else {
                    addResponse("Failed to load response due to " + response.body().toString());
                }
            }
        });

    }

    //ASR Word Wrapping Function
    public void sendData(String useCase, String data){
        if (data != "") {
            JSONObject result = new JSONObject();
            try {
                result.put("useCaseId", useCase);
                result.put("data", data);
                result.put("time", String.valueOf(Calendar.getInstance().getTimeInMillis() % (24 * 60 * 60 * 1000))); //long
            } catch (JSONException e) {
                e.printStackTrace();
            }
            System.out.println(result.toString());
            BLEHelper.getInstance(this).write(/*"\1" +*/ result.toString() + "\0");
        }
    }

    //ASR Word Wrapping Function
    public void sendDataOld(String transcript, boolean isFinal){
        Log.d("Raw String: ", transcript);
        sendString = WordUtils.wrap(transcript, charactersPerLine, "\n", true);
        if (!sendString.endsWith("\n")){
            sendString += "\n"; //ONLY ADDING IF sendString doesn't end with \n (to account for 18 characters exactly, wrapUtils creates a last empty line. last \n is helper for FW - clearing line, setting cursor, getting oled.row etc
        }
        currentNumberofLines = StringUtils.countMatches(sendString, "\n"); //keep in mind last \n added boosts count by +1
        for (int i = 0; i < Math.min((currentNumberofLines - previousNumberofLines), 5); i++) { //added bug fix = capping number of new lines added to 5 (since we only have 5 lines on display)
            sendString += "\2";
        }
        if (currentNumberofLines > 5){
            sendString = sendString.substring(StringUtils.ordinalIndexOf(sendString, "\n", currentNumberofLines-5) +1);//+1 bc we want to move past new line
        }
        if (currentNumberofLines > previousNumberofLines) { //added bug fix - if 5 lines, then 4, then 5 again, don't want to send \2
            previousNumberofLines = currentNumberofLines;
        }
        //Log.d("Number of new lines: ", String.valueOf(StringUtils.countMatches(WordUtils.wrap(formattedTranscript.toString(), charactersPerLine), "\n")));
        if(isFinal){
            previousText += transcript;
            if (currentNumberofLines < previousNumberofLines) { //Important bug fix! (If the final result has less lines than the previous interim, what would have happened is that the isFinal command would have been sent and the display would have scrolled an extra line - but we don't want it to scroll an extra line if the isFinal text has fewer lines than the previous interim)
                for (int i = 0; i < previousNumberofLines - currentNumberofLines; i++) {
                    sendString += "\n";
                }
                if (currentNumberofLines > 4){ //If we did have to add an empty line at the end, then only send the last 4 lines and the 5th empty line (IE, move 1 line ahead in the transcript)
                    sendString = sendString.substring(StringUtils.ordinalIndexOf(sendString, "\n", 1) +1);//+1 bc we want to move past new line
                }
            }
            sendString += "\3";
            previousNumberofLines = 1;
            try { //Bug fix: BLE device not sending final result even though mobile app has “sent” it
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d("Scroll characters: ", String.valueOf(StringUtils.countMatches(sendString, '\2')));
        Log.d("Final: ", String.valueOf(StringUtils.countMatches(sendString, '\3')));
        Log.d("Sending to BT: ", sendString);
        BLEHelper.getInstance(this).write("\1" + sendString + "\0");
        //BLEreadyToReceive = false;

    }
}