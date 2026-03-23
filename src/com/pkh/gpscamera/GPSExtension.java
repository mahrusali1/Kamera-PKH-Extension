package com.pkh.gpscamera;

import android.graphics.*;
import android.content.Context;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.ComponentCategory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

@DesignerComponent(version = 1,
    description = "Ekstensi GPS Map Camera PKH - Custom Template & Manual Input.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "images/extension.png")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET, android.permission.WRITE_EXTERNAL_STORAGE")

public class GPSExtension extends AndroidNonvisibleComponent {
    public GPSExtension(ComponentContainer container) {
        super(container.$form());
    }

    @SimpleFunction(description = "Generate Watermark sesuai Template 1, 2, atau 3.")
    public void GenerateWatermark(final String imagePath, final String inputLat, final String inputLong, 
                                 final String inputDateTime, final String saveLocation, 
                                 final String fileName, final int templateType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1. Reverse Geocoding (OSM) - Variabel dibuat FINAL agar bisa diakses inner class
                    final String finalAddress = fetchAddress(inputLat, inputLong);

                    // 2. Load Bitmap
                    Bitmap original = BitmapFactory.decodeFile(imagePath);
                    Bitmap mutable = original.copy(Bitmap.Config.ARGB_8888, true);
                    android.graphics.Canvas canvas = new android.graphics.Canvas(mutable);

                    // 3. Draw Berdasarkan Template Pilihann
                    drawByTemplate(canvas, templateType, finalAddress, inputLat, inputLong, inputDateTime, mutable.getWidth(), mutable.getHeight());

                    // 4. Save Final Image
                    java.io.File dir = new java.io.File(saveLocation);
                    if (!dir.exists()) dir.mkdirs();
                    
                    java.io.File outFile = new java.io.File(dir, fileName);
                    FileOutputStream out = new FileOutputStream(outFile);
                    mutable.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    out.close();

                    final String finalPath = outFile.getAbsolutePath();
                    form.runOnUiThread(new Runnable() {
                        @Override
                        public void run() { OnAddressFound(finalAddress, finalPath); }
                    });

                } catch (final Exception e) {
                    final String err = e.getMessage();
                    form.runOnUiThread(new Runnable() {
                        @Override
                        public void run() { OnError(err); }
                    });
                }
            }
        }).start();
    }

    private void drawByTemplate(android.graphics.Canvas canvas, int type, String addr, String lat, String lon, String time, int w, int h) {
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.WHITE);
        p.setTextSize(w / 25f);
        p.setShadowLayer(5f, 0f, 0f, Color.BLACK);

        Paint bg = new Paint();
        bg.setColor(Color.BLACK);
        bg.setAlpha(160);

        if (type == 1) {
            canvas.drawRect(0, h - (h/4f), w, h, bg);
            canvas.drawText(addr, 40, h - (h/6f), p);
            canvas.drawText("Lat/Long: " + lat + ", " + lon, 40, h - (h/10f), p);
            canvas.drawText("Waktu: " + time, 40, h - (h/20f), p);
        } 
        else if (type == 2) {
            canvas.drawRect(w/2f, h - (h/6f), w, h, bg);
            canvas.drawText(lat + ", " + lon, w/2f + 20, h - (h/10f), p);
            canvas.drawText(time, w/2f + 20, h - (h/20f), p);
        }
        else {
            canvas.drawRect(0, h - (h/5f), w, h, bg);
            p.setTextSize(w/30f);
            canvas.drawText(addr, 20, h - (h/8f), p);
            canvas.drawText("GPS: " + lat + "," + lon + " | " + time, 20, h - (h/15f), p);
        }
    }

    private String fetchAddress(String lat, String lon) {
        try {
            URL url = new URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lon);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestProperty("User-Agent", "PKH-App");
            Scanner s = new Scanner(c.getInputStream()).useDelimiter("\\A");
            String res = s.hasNext() ? s.next() : "";
            if (res.contains("display_name")) {
                int start = res.indexOf("display_name") + 15;
                return res.substring(start, res.indexOf("\"", start));
            }
        } catch (Exception e) {}
        return "Alamat tidak ditemukan";
    }

    @SimpleEvent(description = "Event saat alamat ditemukan dan proses selesai.") 
    public void OnAddressFound(String address, String resultPath) {
        EventDispatcher.dispatchEvent(this, "OnAddressFound", address, resultPath);
    }

    @SimpleEvent(description = "Event saat terjadi error pada proses.") 
    public void OnError(String message) {
        EventDispatcher.dispatchEvent(this, "OnError", message);
    }
}
