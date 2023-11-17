package com.Madhav.solaire;

import androidx.annotation.NonNull;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.OkHttpClient;

public class TranslateActivity extends AppCompatActivity {
    TranslatorOptions options;
    Translator translator;
    ImageButton translationButton;
    boolean translationRunning = false;

    SpeechRecognizer speechRecognizer;
    TextView translationText;
    static int TRANSCRIPTION_IDLE_TIME = 15000; //How long the recognizer waits for no recognizable speech before completing

    private String sendString = "";
    private int charactersPerLine = 18;
    private int previousNumberofLines = 1;
    private int currentNumberofLines = 0;
    private String previousText = "";

    private static final String USE_CASE_ID = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle("Translation");

        translationButton = findViewById(R.id.translationButton);
        translationText = findViewById(R.id.translationText);

        // Create an English-German translator:
        options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.SPANISH)
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build();

        translator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(
                        new OnSuccessListener() {
                            @Override
                            public void onSuccess(Object o) {
                                // Model downloaded successfully. Okay to start translating.
                                // (Set a flag, unhide the translation UI, etc.)
                                System.out.println("Downloaded model!");
                            }

                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Model couldn’t be downloaded or other internal error.
                                // ...
                                System.out.println("ERROR DOWNLOADING");
                                e.printStackTrace();
                            }
                        });

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
                translationRunning = true;
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
            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle bundle) {
                System.out.println("Fired!");
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (data != null) {
                    translator.translate(data.get(0))
                            .addOnSuccessListener(
                                    new OnSuccessListener() {
                                        @Override
                                        public void onSuccess(Object o) {
                                            System.out.println(o.toString());
                                            translationText.setText(o.toString());
                                            sendData(USE_CASE_ID, o.toString());
                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Error.
                                            // ...
                                        }
                                    });
                    //transcriptHistory += data.get(0) + ". ";
                }
                else {

                    //transcriptHistory += lastResult + ". ";
                    //transcriptText.setText(transcriptHistory);
                }
                translationButton.setImageResource(R.drawable.ic_baseline_mic);
                translationRunning = false;
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (data.size() > 0) { //Handle for case of transcription started but no speech

                    //lastResult = data.get(0);
                    System.out.println(data.get(0));

                    translator.translate(data.get(0))
                            .addOnSuccessListener(
                                    new OnSuccessListener() {
                                        @Override
                                        public void onSuccess(Object o) {
                                            System.out.println(o.toString());
                                            translationText.setText(o.toString());
                                            sendData(USE_CASE_ID, o.toString());
                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Error.
                                            // ...
                                        }
                                    });


                }
                else {
                    translationButton.setImageResource(R.drawable.ic_baseline_mic);
                    translationRunning = false;
                }
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        translationButton.setOnClickListener((v)->{
            if (!translationRunning) {
                translationButton.setImageResource(R.drawable.ic_baseline_wait);
                speechRecognizer.startListening(speechRecognizerIntent);
            }
            else {
                translationButton.setImageResource(R.drawable.ic_baseline_mic);
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
                Intent intent = new Intent(TranslateActivity.this, MainActivity.class);
                startActivity(intent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void sendData(String useCase, String data) {
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