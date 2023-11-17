package com.Madhav.solaire;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class recipeActivity extends AppCompatActivity {

    SearchView recipeQuery;
    TextView recipeInstructionText;
    OkHttpClient client = new OkHttpClient();
    JSONObject sendJson = new JSONObject();

    private static final String USE_CASE_ID = "3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle("Recipes");

        recipeQuery = findViewById(R.id.recipeQuery);
        recipeInstructionText = findViewById(R.id.recipeInstructionText);
        recipeInstructionText.setMovementMethod(new ScrollingMovementMethod()); //make textview scrollable

        recipeQuery.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    callApi(query);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

        });;

        try {
            sendJson.put("useCaseId", USE_CASE_ID);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void getInstructionsfromId(int id) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.spoonacular.com/recipes/" + String.valueOf(id) + "/information").newBuilder();
        urlBuilder.addQueryParameter("apiKey", "a0dd084e61154919814e6295120045bf"); //api key
        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                //.post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                System.out.println("Failed to load response due to " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //ArrayList<Recipe> allRecipes = new ArrayList<>();
                try{
                    String jsonData = response.body().string(); //Raw Data
                    if(response.isSuccessful()){
                        String displayText = "Ingredients:\n";
                        JSONObject information = new JSONObject(jsonData); //Base level json object
                        //Extract ingredients
                        JSONArray ingredients = information.getJSONArray("extendedIngredients"); //Array for each ingredient
                        JSONObject sendIngredients = new JSONObject();
                        JSONArray ingredientNames = new JSONArray();
                        JSONArray ingredientAmounts = new JSONArray();
                        for (int i = 0; i < ingredients.length(); i++) {
                            JSONObject ingredient = ingredients.getJSONObject(i);
                            String ingredientName = ingredient.getString("name");
                            ingredientNames.put(ingredientName);

                            JSONObject measure = ingredient.getJSONObject("measures").getJSONObject("us");
                            System.out.println(measure.getDouble("amount") + " " + measure.getString("unitShort") + " " + ingredientName);
                            String amount = String.valueOf(measure.getDouble("amount")) + " " + measure.getString("unitShort");
                            ingredientAmounts.put(amount);
                            displayText += amount + " " + ingredientName + "\n";
                        }
                        sendIngredients.put("Names", ingredientNames);
                        sendIngredients.put("Amounts", ingredientAmounts);
                        sendJson.put("Ingredients", sendIngredients);
                        displayText += "\n\nInstructions:\n";

                        //Extract instructions
                        JSONArray recipeInstructionsJSON = information.getJSONArray("analyzedInstructions").getJSONObject(0).getJSONArray("steps"); //take the steps list
                        JSONArray sendInstructions = new JSONArray();

                        for (int i = 0; i < recipeInstructionsJSON.length(); i++) {
                            JSONObject step = recipeInstructionsJSON.getJSONObject(i);
                            String instructions = step.getString("step");
                            sendInstructions.put(instructions);
                            System.out.println("Step " + i + ": " + instructions);
                            displayText += String.valueOf(i) + ". " + instructions + "\n";
                        }
                        sendJson.put("Instructions", sendInstructions);
                        String finalDisplayText = displayText;
                        recipeActivity.this.runOnUiThread(() -> {
                            recipeInstructionText.setText(finalDisplayText);
                        });

                        sendData();
                    }

                }catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void callApi(String query) throws IOException {

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.spoonacular.com/recipes/complexSearch").newBuilder();
        urlBuilder.addQueryParameter("apiKey", "a0dd084e61154919814e6295120045bf"); //api key
        urlBuilder.addQueryParameter("query", query); //input query
        urlBuilder.addQueryParameter("instructionsRequired", "true");
        urlBuilder.addQueryParameter("number", "1"); //change how many results
        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                //.post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                System.out.println("Failed to load response due to " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //ArrayList<Recipe> allRecipes = new ArrayList<>();
                try{
                    String jsonData = response.body().string();
                    if(response.isSuccessful()){
                        JSONObject recipeCatalogJSON = new JSONObject(jsonData);
                        JSONObject recipeSearchResultJSON = recipeCatalogJSON.getJSONArray("results").getJSONObject(0); //take only the first recipe in the search results
                        String title = recipeSearchResultJSON.getString("title");
                        int id = recipeSearchResultJSON.getInt("id");
                        System.out.println(title + id);
                        getInstructionsfromId(id);
                    }

                }catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
                Intent intent = new Intent(recipeActivity.this, MainActivity.class);
                startActivity(intent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void sendData() {
        try {
            sendJson.put("time", String.valueOf(Calendar.getInstance().getTimeInMillis() % (24 * 60 * 60 * 1000))); //long
        } catch (JSONException e) {
            e.printStackTrace();
        }
        System.out.println(sendJson.toString());
        if (sendJson.toString().length() > 200) {
            for (short i = 0; i < sendJson.toString().length(); i += 200) {
                if (i + 200 < sendJson.toString().length()) {
                    BLEHelper.getInstance(this).write(/*"\1" +*/ sendJson.toString().substring(i, i+200));
                }
                else {
                    BLEHelper.getInstance(this).write(/*"\1" +*/ sendJson.toString().substring(i) + "\0");
                }

            }
        }

    }
}