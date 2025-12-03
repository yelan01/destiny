package com.example.destiny;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private TextView tvRandomNumber;
    private Button btnRandom;
    private Button btnSwitchToSecond;
    // 天氣相關的 UI 元件
    private TextView tvWeatherInfo; // 第一個 Label
    private TextView tvWeatherStatus; // 第二個 Label
    private ListView lvForecast;

    // API Key (使用您提供的)
    private final String apiKey = "a40a9237704b1b486feb03c7cc60c32d";
    private RequestQueue queue;

    // 暫存來自兩個 API 的資料
    private String currentCityName;
    private String currentDescription;
    private Double currentTemp;
    private Integer currentRainPercentage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        queue = Volley.newRequestQueue(this);

        // 找到 UI 元件
        tvRandomNumber = findViewById(R.id.tv_random_number);
        btnRandom = findViewById(R.id.btn_random);
        btnSwitchToSecond = findViewById(R.id.btn_switch_to_second);
        tvWeatherInfo = findViewById(R.id.tv_weather_info); // 對應 XML 的新 ID
        tvWeatherStatus = findViewById(R.id.tv_weather_status); // 對應 XML 的新 ID
        lvForecast = findViewById(R.id.lv_forecast);

        setupButtonListeners();
        fetchCurrentWeatherData();
        fetchForecastData();
    }

    private void setupButtonListeners() {
        btnRandom.setOnClickListener(v -> {
            int randomNumber = new Random().nextInt(1000);
            tvRandomNumber.setText(String.valueOf(randomNumber));
        });

        btnSwitchToSecond.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
            startActivity(intent);
        });
    }

    private void fetchCurrentWeatherData() {
        String city = "Taipei";
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + apiKey + "&units=metric&lang=zh_tw";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        currentDescription = response.getJSONArray("weather").getJSONObject(0).getString("description");
                        currentTemp = response.getJSONObject("main").getDouble("temp");
                        currentCityName = response.getString("name");

                        updateMainWeatherLabel();
                    } catch (JSONException e) {
                        tvWeatherInfo.setText("解析目前天氣失敗");
                        Log.e("WeatherApp", "Current weather JSON parsing error: " + e.getMessage());
                    }
                },
                error -> {
                    tvWeatherInfo.setText("無法取得目前天氣");
                    Log.e("WeatherApp", "Current weather Volley error: " + error.toString());
                });

        queue.add(request);
    }

    private void fetchForecastData() {
        String city = "Taipei";
        String url = "https://api.openweathermap.org/data/2.5/forecast?q=" + city + "&appid=" + apiKey + "&units=metric&lang=zh_tw";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        ArrayList<String> forecastList = new ArrayList<>();
                        JSONArray list = response.getJSONArray("list");

                        if (list.length() > 0) {
                            JSONObject firstForecast = list.getJSONObject(0);
                            double pop = firstForecast.getDouble("pop");
                            currentRainPercentage = (int) (pop * 100);

                            updateMainWeatherLabel();
                        }

                        // --- ListView 的部分維持不變 ---
                        int forecastCount = Math.min(list.length(), 5);
                        for (int i = 0; i < forecastCount; i++) {
                            JSONObject forecast = list.getJSONObject(i);
                            int hoursLater = (i + 1) * 3;
                            long dt = forecast.getLong("dt");
                            String time = formatTime(dt);
                            String description = forecast.getJSONArray("weather").getJSONObject(0).getString("description");
                            double temp = forecast.getJSONObject("main").getDouble("temp");
                            String formattedTemp = String.format("%.0f°C", temp);
                            double pop = forecast.getDouble("pop");
                            int rainPercentage = (int) (pop * 100);
                            String weatherType;
                            if (description.contains("雨") || rainPercentage > 40) {
                                weatherType = "雨天";
                            } else if (description.contains("雲") || description.contains("陰")) {
                                weatherType = "陰天";
                            } else {
                                weatherType = "晴天";
                            }
                            String forecastInfo = hoursLater + " 小時後 (" + time + ") | " + weatherType + " | " + formattedTemp + " | " + rainPercentage + "%";
                            forecastList.add(forecastInfo);
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, forecastList);
                        lvForecast.setAdapter(adapter);

                    } catch (JSONException e) {
                        Toast.makeText(this, "解析預報資料失敗", Toast.LENGTH_SHORT).show();
                        Log.e("WeatherApp", "Forecast JSON parsing error: " + e.getMessage());
                    }
                },
                error -> {
                    Toast.makeText(this, "無法取得預報資料", Toast.LENGTH_SHORT).show();
                    Log.e("WeatherApp", "Forecast Volley error: " + error.toString());
                });

        queue.add(request);
    }

    // 更新最上方的天氣 Label
    private void updateMainWeatherLabel() {
        if (currentCityName != null && currentDescription != null && currentTemp != null && currentRainPercentage != null) {
            String currentTime = new SimpleDateFormat("HH:mm", Locale.TAIWAN).format(new Date());

            // *** 修改開始：分別組合並設定兩個 Label 的文字 ***

            // 1. 組合第一個 Label 的文字 (地區:現在時間|氣溫|降雨機率)
            String infoString = currentCityName + " : " + currentTime + " | " +
                    String.format("%.1f", currentTemp) + "°C | " +
                    currentRainPercentage + "%";

            // 2. 第二個 Label 的文字直接就是天氣描述
            String statusString = currentDescription;

            // 3. 更新 UI
            tvWeatherInfo.setText(infoString);
            tvWeatherStatus.setText(statusString);

            // *** 修改結束 ***
        }
    }

    private String formatTime(long unixSeconds) {
        Date date = new Date(unixSeconds * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.TAIWAN);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));
        return sdf.format(date);
    }
}
