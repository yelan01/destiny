package com.example.destiny;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ThirdActivity extends AppCompatActivity {

    private Button btnRestart;
    private Button btnFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third);

        // 找到 UI 元件
        btnRestart = findViewById(R.id.btn_restart);
        btnFinish = findViewById(R.id.btn_finish);

        // 設定「重啟」按鈕的點擊事件
        btnRestart.setOnClickListener(v -> {
            // 建立一個 Intent 來啟動 MainActivity
            Intent intent = new Intent(ThirdActivity.this, MainActivity.class);
            // 加上這兩個 Flag 可以清除掉中間的 Activity (第二、三頁)，讓使用者按下返回鍵時不會回到舊的頁面
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // 關閉當前的第三頁
        });

        // 設定「結束」按鈕的點擊事件
        btnFinish.setOnClickListener(v -> {
            // 關閉 App
            finishAffinity();
        });
    }
}
