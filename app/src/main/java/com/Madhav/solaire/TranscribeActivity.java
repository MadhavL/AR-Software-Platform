package com.Madhav.solaire;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class TranscribeActivity extends AppCompatActivity {

    ImageButton start_transcription;
    SpeechRecognizer speechRecognizer;
    TextView transcriptText;
    static int TRANSCRIPTION_IDLE_TIME = 15000; //How long the recognizer waits for no recognizable speech before completing
    boolean transcriptionRunning = false;
    String transcriptHistory = "";
    String lastResult = "";

    private String sendString = "";
    private int charactersPerLine = 18;
    private int previousNumberofLines = 1;
    private int currentNumberofLines = 0;
    private String previousText = "";

    private static final String USE_CASE_ID = "0";

    boolean lastIsFinal = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transcribe);

        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle("Transcription");

        start_transcription = findViewById(R.id.start_transcription_button);
        transcriptText = findViewById(R.id.transcriptResultTextView);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, TRANSCRIPTION_IDLE_TIME);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
                transcriptionRunning = true;
                lastIsFinal = false;
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {
                System.out.println("END");
                lastIsFinal = true;
                //sendData(lastResult, true);
            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle bundle) {
                System.out.println("Fired!");
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (data != null) {
                    transcriptText.setText(transcriptHistory + data.get(0));
                    sendData(USE_CASE_ID, data.get(0));
                    transcriptHistory += data.get(0) + ". ";
                }
                else {
                    transcriptHistory += lastResult + ". ";
                    transcriptText.setText(transcriptHistory);
                    sendData(USE_CASE_ID, lastResult);
                }
                start_transcription.setImageResource(R.drawable.ic_baseline_mic);
                transcriptionRunning = false;
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (data.size() > 0) { //Handle for case of transcription started but no speech
                    transcriptText.setText(transcriptHistory + data.get(0));
                    sendData(USE_CASE_ID, data.get(0));
                    lastResult = data.get(0);
                    System.out.println(data.get(0));
                }
                else {
                    start_transcription.setImageResource(R.drawable.ic_baseline_mic);
                    transcriptionRunning = false;
                }
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });


        start_transcription.setOnClickListener((v)->{
            if (!transcriptionRunning) {
                start_transcription.setImageResource(R.drawable.ic_baseline_wait);
                speechRecognizer.startListening(speechRecognizerIntent);
            }
            else {
                start_transcription.setImageResource(R.drawable.ic_baseline_mic);
                speechRecognizer.stopListening();
            }

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
                Intent intent = new Intent(TranscribeActivity.this, MainActivity.class);
                startActivity(intent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

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
}