package com.pkh.gpscamera;

import android.graphics.*;
import android.media.ExifInterface;
import android.content.Context;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.ComponentCategory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

@DesignerComponent(version = 1,
    description = "Ekstensi GPS Map Camera PKH - Geotag & Watermark Otomatis.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "images/extension.png")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET, android.permission.READ_EXTERNAL_STORAGE, android.permission.WRITE_EXTERNAL_STORAGE, android.permission.ACCESS_FINE_LOCATION")

public class GPSExtension extends AndroidNonvisibleComponent {
    private Context context;

    public GPSExtension(ComponentContainer container) {
        super(container.$form());
        this.context = container.$context();
    }

    @SimpleFunction(description = "Proses Watermark Foto PKH.")
    public void ProcessImage(final String imagePath, final String inputLat, final String inputLong, final String savePath, final String fileName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String finalLat = (inputLat == null || inputLat.isEmpty()) ? "" : inputLat;
                    String finalLong = (inputLong == null || inputLong.isEmpty()) ? "" : inputLong;
                    String finalDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

                    // 1. PRIORITY LOGIC
                    if (finalLat.isEmpty() || finalLong.isEmpty()) {
                        ExifInterface exif = new ExifInterface(imagePath);
                        float[] latLong = new float[2];
                        if (exif.getLatLong(latLong)) {
                            finalLat = String.valueOf(latLong[0]);
                            finalLong = String.valueOf(latLong[1]);
                        }
                    }

                    // 2. REVERSE GEOCODING
                    String address = fetchAddress(finalLat, finalLong);

                    // 3. IMAGE PROCESSING
                    Bitmap originalBitmap = BitmapFactory.decodeFile(imagePath);
                    Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    
                    // Gunakan spesifik android.graphics.Canvas
                    android.graphics.Canvas canvas = new android.graphics.Canvas(mutableBitmap);

                    // Draw Watermark
                    drawWatermark(canvas, address, finalLat, finalLong, finalDate, mutableBitmap.getWidth(), mutableBitmap.getHeight());

                    // 4. SAVE FILE - Gunakan spesifik java.io.File
                    java.io.File dir = new java.io.File(savePath);
                    if (!dir.exists()) dir.mkdirs();
                    
                    java.io.File outFile = new java.io.File(dir, fileName);
                    FileOutputStream out = new FileOutputStream(outFile);
                    mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    out.flush();
                    out.close();

                    final String finalPath = outFile.getAbsolutePath();
                    form.runOnUiThread(new Runnable() {
                        @Override
                        public void run() { AfterProcess(finalPath); }
                    });

                } catch (Exception e) {
                    final String msg = e.getMessage();
                    form.runOnUiThread(new Runnable() {
                        @Override
                        public void run() { OnError(msg); }
                    });
                }
            }
        }).start();
    }

    private void drawWatermark(android.graphics.Canvas canvas, String addr, String lat, String lon, String date, int width, int height) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(width / 25); // Ukuran teks proporsional
        paint.setAntiAlias(true);
        paint.setShadowLayer(5f, 0f, 0f, Color.BLACK);

        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.BLACK);
        rectPaint.setAlpha(150); 
        
        float rectHeight = height / 5f;
        canvas.drawRect(0, height - rectHeight, width, height, rectPaint);

        float xPos = 40;
        float yBase = height - (rectHeight / 1.5f);
        
        canvas.drawText("Lokasi: " + addr, xPos, yBase, paint);
        canvas.drawText("GPS: " + lat + ", " + lon, xPos, yBase + (paint.getTextSize() * 1.2f), paint);
        canvas.drawText("Waktu: " + date + " | Petugas PKH", xPos, yBase + (paint.getTextSize() * 2.4f), paint);
    }

    private String fetchAddress(String lat, String lon) {
        if (lat.isEmpty() || lon.isEmpty()) return "Koordinat tidak ditemukan";
        try {
            URL url = new URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lon);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "AppPKH");
            Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
            String res = s.hasNext() ? s.next() : "";
            if (res.contains("display_name")) {
                int start = res.indexOf("display_name") + 15;
                int end = res.indexOf("\"", start);
                return res.substring(start, end);
            }
        } catch (Exception e) { return "Alamat gagal dimuat"; }
        return "Lokasi: " + lat + ", " + lon;
    }

    @SimpleEvent public void AfterProcess(String resultPath) { EventDispatcher.dispatchEvent(this, "AfterProcess", resultPath); }
    @SimpleEvent public void OnError(String message) { EventDispatcher.dispatchEvent(this, "OnError", message); }
}
