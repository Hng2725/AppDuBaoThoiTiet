package com.example.dubaothoitiet;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Hiển thị AlertDialog
        new AlertDialog.Builder(this)
                .setTitle("Giới thiệu")
                .setMessage("Đây là ứng dụng dự báo thời tiết, giúp bạn cập nhật thông tin thời tiết một cách nhanh chóng và chính xác.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_weather) {
            // Chuyển về MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_about) {
            // Đã ở trong AboutActivity, không cần làm gì
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
