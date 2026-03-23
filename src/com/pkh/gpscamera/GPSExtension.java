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
    description = "Ekstensi GPS Map Camera PKH - Versi Final Auto Wrap & Smart Location.",
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
                    final String finalAddress = fetchAddress(inputLat, inputLong);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;
                    Bitmap original = BitmapFactory.decodeFile(imagePath, options);
                    
                    if (original == null) {
                        throw new Exception("File foto tidak ditemukan");
                    }

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
            float cardHeight = (h > w) ? h * 0.20f : h * 0.30f; 
            float margin = (w - cardWidth) / 2f;
            float cardTop = h - cardHeight - (h * 0.03f);

            Paint bg = new Paint();
            bg.setColor(Color.parseColor("#4D4D4D"));
            bg.setAlpha(225);
            bg.setAntiAlias(true);
            RectF rect = new RectF(margin, cardTop, margin + cardWidth, cardTop + cardHeight);
            canvas.drawRoundRect(rect, 30, 30, bg);

            float mapSize = cardHeight * 0.82f;
            float mapMargin = (cardHeight - mapSize) / 2f;
            try {
                String mapUrl = "https://static-maps.yandex.ru/1.x/?ll=" + lon + "," + lat + "&z=16&l=sat&size=300,300";
                URL url = new URL(mapUrl);
                Bitmap map = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                if (map != null) {
                    RectF mRect = new RectF(margin + mapMargin, cardTop + mapMargin, margin + mapMargin + mapSize, cardTop + mapMargin + mapSize);
                    canvas.drawBitmap(map, null, mRect, null);
                }
            } catch (Exception e) {
                Paint mapBg = new Paint();
                mapBg.setColor(Color.GRAY);
                canvas.drawRect(margin + mapMargin, cardTop + mapMargin, margin + mapMargin + mapSize, cardTop + mapMargin + mapSize, mapBg);
            }

            String kecName = "Kecamatan";
            String[] parts = addr.split(",");
            for (String p : parts) {
                if (p.toLowerCase().contains("kecamatan") || p.toLowerCase().contains("kec.")) {
                    kecName = p.trim();
                    break;
                }
            }

            TextPaint tp = new TextPaint();
            tp.setAntiAlias(true);
            tp.setColor(Color.WHITE);
            float textLeft = margin + mapSize + (mapMargin * 2);
            float availableWidth = cardWidth - mapSize - (mapMargin * 3);

            tp.setTextSize(w / 26f);
            tp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(kecName + ", Jawa Timur 🇮🇩", textLeft, cardTop + (cardHeight * 0.22f), tp);

            tp.setTextSize(w / 38f);
            tp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            StaticLayout sl = new StaticLayout(addr, tp, (int)availableWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            canvas.save();
            canvas.translate(textLeft, cardTop + (cardHeight * 0.32f));
            sl.draw(canvas);
            canvas.restore();

            float infoTop = cardTop + (cardHeight * 0.78f);
            canvas.drawText("Lat " + lat + "°  Long " + lon + "°", textLeft, infoTop, tp);
            canvas.drawText(time + " GMT +07:00", textLeft, infoTop + (cardHeight * 0.12f), tp);

        } else {
            Paint p = new Paint();
            p.setColor(Color.WHITE);
            p.setAntiAlias(true);
            p.setTextSize(w / 30f);
            canvas.drawText(lat + ", " + lon + " | " + time, 20, h - 50, p);
        }
    }

    private String fetchAddress(String lat, String lon) {
        try {
            URL url = new URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lon);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestProperty("User-Agent", "SKP-Click-App");
            Scanner s = new Scanner(c.getInputStream()).useDelimiter("\\A");
            String res = s.hasNext() ? s.next() : "";
            if (res.contains("display_name")) {
                int start = res.indexOf("display_name") + 15;
                return res.substring(start, res.indexOf("\"", start));
            }
        } catch (Exception e) {}
        return "Alamat tidak ditemukan";
    }

    @SimpleEvent(description = "Proses Berhasil.") 
    public void OnAddressFound(String address, String resultPath) {
        EventDispatcher.dispatchEvent(this, "OnAddressFound", address, resultPath);
    }

    @SimpleEvent(description = "Proses Error.") 
    public void OnError(String message) {
        EventDispatcher.dispatchEvent(this, "OnError", message);
    }
}
