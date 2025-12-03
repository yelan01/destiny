package com.example.destiny;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SecondActivity extends AppCompatActivity {

    private Button btnSwitchToThird;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        // 找到 UI 元件
        btnSwitchToThird = findViewById(R.id.btn_switch_to_third);

        // 設定「切換到第三頁」按鈕的點擊事件
        btnSwitchToThird.setOnClickListener(v -> {
            // 建立一個 Intent 來啟動 ThirdActivity
            Intent intent = new Intent(SecondActivity.this, ThirdActivity.class);
            startActivity(intent);
        });
    }
}
