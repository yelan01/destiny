package com.example.destiny;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView tvRandomNumber;
    private Button btnRandom;
    private Button btnSwitchToSecond;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 確保使用正確的佈局檔案

        // 找到 UI 元件
        tvRandomNumber = findViewById(R.id.tv_random_number);
        btnRandom = findViewById(R.id.btn_random);
        btnSwitchToSecond = findViewById(R.id.btn_switch_to_second);

        // 設定「隨機」按鈕的點擊事件
        btnRandom.setOnClickListener(v -> {
            // 產生一個 0 到 999 的隨機數字
            int randomNumber = new Random().nextInt(1000);
            // 將數字設定到 TextView 上
            tvRandomNumber.setText(String.valueOf(randomNumber));
        });

        // 設定「切換」按鈕的點擊事件
        btnSwitchToSecond.setOnClickListener(v -> {
            // 建立一個 Intent 來啟動 SecondActivity
            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
            startActivity(intent);
        });
    }
}
