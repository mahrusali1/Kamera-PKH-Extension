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
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout;

@DesignerComponent(version = 1,
    description = "Ekstensi GPS Map Camera PKH - Fix Map & Auto Wrap Text.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "images/extension.png")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET, android.permission.WRITE_EXTERNAL_STORAGE")

public class GPSExtension extends AndroidNonvisibleComponent {
    public GPSExtension(ComponentContainer container) {
        super(container.$form());
    }

    @SimpleFunction(description = "Generate Watermark sesuai Template 1 dengan Auto Wrap Text.")
    public void GenerateWatermark(final String imagePath, final String inputLat, final String inputLong, 
                                 final String inputDateTime, final String saveLocation, 
                                 final String fileName, final int templateType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String finalAddress = fetchAddress(inputLat, inputLong);
                    Bitmap original = BitmapFactory.decodeFile(imagePath);
                    Bitmap mutable = original.copy(Bitmap.Config.ARGB_8888, true);
                    android.graphics.Canvas canvas = new android.graphics.Canvas(mutable);

                    drawByTemplate(canvas, templateType, finalAddress, inputLat, inputLong, inputDateTime, mutable.getWidth(), mutable.getHeight());

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
        if (type == 1) {
            float cardWidth = w * 0.96f;
            float cardHeight = h * 0.30f; // Tinggi ditambah sedikit untuk menampung teks wrap
            float margin = (w - cardWidth) / 2f;
            float cardTop = h - cardHeight - (h * 0.03f);

            // 1. Gambar Background Card
            Paint bg = new Paint();
            bg.setColor(Color.parseColor("#4D4D4D"));
            bg.setAlpha(225);
            bg.setAntiAlias(true);
            RectF rect = new RectF(margin, cardTop, margin + cardWidth, cardTop + cardHeight);
            canvas.drawRoundRect(rect, 35, 35, bg);

            // 2. Ambil Peta Satelit
            float mapSize = cardHeight * 0.85f;
            float mapMargin = (cardHeight - mapSize) / 2f;
            try {
                String mapUrl = "https://static-maps.yandex.ru/1.x/?ll=" + lon + "," + lat + "&z=16&l=sat&size=350,350";
                URL url = new URL(mapUrl);
                Bitmap map = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                if (map != null) {
                    RectF mRect = new RectF(margin + mapMargin, cardTop + mapMargin, margin + mapMargin + mapSize, cardTop + mapMargin + mapSize);
                    canvas.drawBitmap(map, null, mRect, null);
                }
            } catch (Exception e) {}

            // 3. Persiapan Teks
            TextPaint tp = new TextPaint();
            tp.setAntiAlias(true);
            tp.setColor(Color.WHITE);
            float textLeft = margin + mapSize + (mapMargin * 2);
            float availableWidth = cardWidth - mapSize - (mapMargin * 3);

            // --- BARIS 1: KECAMATAN & PROVINSI (BOLD) ---
            tp.setTextSize(w / 24f);
            tp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            String kecName = "Kecamatan " + (addr.contains(",") ? addr.split(",")[addr.split(",").length-4].trim() : "Lokasi");
            canvas.drawText(kecName + ", Jawa Timur 🇮🇩", textLeft, cardTop + (cardHeight * 0.22f), tp);

            // --- BARIS 2: ALAMAT LENGKAP (AUTO WRAP) ---
            tp.setTextSize(w / 35f);
            tp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            StaticLayout sl = new StaticLayout(addr, tp, (int)availableWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            canvas.save();
            canvas.translate(textLeft, cardTop + (cardHeight * 0.32f));
            sl.draw(canvas);
            canvas.restore();

            // --- BARIS 3: LAT LONG & WAKTU (DI BAWAH TEKS WRAP) ---
            float textBottom = cardTop + (cardHeight * 0.75f);
            canvas.drawText("Lat " + lat + "°  Long " + lon + "°", textLeft, textBottom, tp);
            canvas.drawText(time + " GMT +07:00", textLeft, textBottom + (cardHeight * 0.15f), tp);
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

    @SimpleEvent(description = "Event Selesai.") 
    public void OnAddressFound(String address, String resultPath) {
        EventDispatcher.dispatchEvent(this, "OnAddressFound", address, resultPath);
    }

    @SimpleEvent(description = "Event Error.") 
    public void OnError(String message) {
        EventDispatcher.dispatchEvent(this, "OnError", message);
    }
}
