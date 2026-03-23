package com.pkh.gpscamera;

import android.graphics.*;
import android.graphics.BitmapFactory;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.ComponentCategory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

@DesignerComponent(
        version = 4,
        description = "GPS Map Camera PKH - FINAL STABLE BUILD",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "images/extension.png"
)
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET, android.permission.WRITE_EXTERNAL_STORAGE")
public class GPSExtension extends AndroidNonvisibleComponent {

    public GPSExtension(ComponentContainer container) {
        super(container.$form());
    }

    @SimpleFunction(description = "Generate Watermark GPS Camera")
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
                    if (original == null) throw new Exception("Foto tidak ditemukan");

                    Bitmap bitmap = original.copy(Bitmap.Config.ARGB_8888, true);
                    android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

                    drawByTemplate(canvas, finalAddress, inputLat, inputLong, inputDateTime,
                            bitmap.getWidth(), bitmap.getHeight());

                    java.io.File dir = new java.io.File(saveLocation);
                    if (!dir.exists()) dir.mkdirs();

                    java.io.File outFile = new java.io.File(dir, fileName);
                    java.io.FileOutputStream out = new java.io.FileOutputStream(outFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
                    out.close();

                    final String path = outFile.getAbsolutePath();
                    form.runOnUiThread(new Runnable() {
                        @Override
                        public void run() { OnAddressFound(finalAddress, path); }
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

    private void drawByTemplate(android.graphics.Canvas canvas, String addr, String lat, String lon,
                            String time, int w, int h) {

    float dp = w / 360f;

    float padding = 12 * dp;
    float mapSize = 60 * dp;
    float spacing = 10 * dp;

    float cardWidth = w * 0.94f;
    float cardHeight = mapSize + (padding * 1.5f);

    float left = (w - cardWidth) / 2f;
    float top = h - cardHeight - (20 * dp);

    // === LOAD FONT (HANYA SEKALI) ===
    // === LOAD FONT AMAN (ANTI ERROR APP INVENTOR) ===
Typeface fontMedium = Typeface.DEFAULT_BOLD;
Typeface fontRegular = Typeface.DEFAULT;

try {
    java.io.InputStream is1 = form.openAsset("sfdisplay_medium.TTF");
    java.io.InputStream is2 = form.openAsset("sfuitext_regular.otf");

    java.io.File file1 = java.io.File.createTempFile("font1", ".ttf");
    java.io.File file2 = java.io.File.createTempFile("font2", ".otf");

    java.io.FileOutputStream os1 = new java.io.FileOutputStream(file1);
    java.io.FileOutputStream os2 = new java.io.FileOutputStream(file2);

    byte[] buffer = new byte[1024];
    int len;

    while ((len = is1.read(buffer)) > 0) os1.write(buffer, 0, len);
    while ((len = is2.read(buffer)) > 0) os2.write(buffer, 0, len);

    os1.close();
    os2.close();
    is1.close();
    is2.close();

    fontMedium = Typeface.createFromFile(file1);
    fontRegular = Typeface.createFromFile(file2);

} catch (Exception e) {
    e.printStackTrace();
}

    // === BACKGROUND ===
    Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    bg.setColor(Color.parseColor("#99000000"));
    canvas.drawRoundRect(
            new RectF(left, top, left + cardWidth, top + cardHeight),
            12 * dp, 12 * dp, bg
    );
// === HEADER ATAS (GPS MAP CAMERA) ===
TextPaint topText = new TextPaint(Paint.ANTI_ALIAS_FLAG);
topText.setColor(Color.WHITE);
topText.setTextSize(11 * dp);
topText.setTypeface(Typeface.DEFAULT_BOLD);
topText.setShadowLayer(4, 0, 0, Color.BLACK);
            
float margin = 16 * dp;
float textWidth = topText.measureText("GPS Map Camera");

float xText = w - textWidth - margin;
float yText = top + (2 * dp);

            // 🔥 ICON DULU
try {
    Bitmap icon = BitmapFactory.decodeStream(form.openAsset("camera_icon.png"));
    if (icon != null) {
        float iconSize = 18 * dp;

        float iconX = xText - iconSize - (6 * dp);
        float iconY = yText - iconSize + (6 * dp);

        canvas.drawBitmap(icon, null,
                new RectF(iconX, iconY, iconX + iconSize, iconY + iconSize),
                null);
    }
} catch (Exception e) {
    e.printStackTrace();
}
canvas.drawText("GPS Map Camera", xText, yText, topText);
            

            
    // === MAP (ROUNDED) ===
    
RectF mapRect = new RectF(
        left + padding,
        top + padding,
        left + padding + mapSize,
        top + padding + mapSize
);

try {
    URL url = new URL("https://static-maps.yandex.ru/1.x/?ll=" + lon + "," + lat + "&z=17&l=sat&size=300,300");
    Bitmap map = BitmapFactory.decodeStream(url.openConnection().getInputStream());

    if (map != null) {
        // === DRAW MAP ROUNDED ===
        Path path = new Path();
        path.addRoundRect(mapRect, 12 * dp, 12 * dp, Path.Direction.CW);

        canvas.save();
        canvas.clipPath(path);
        canvas.drawBitmap(map, null, mapRect, null);
        canvas.restore();

        // === DRAW PIN ===
        try {
            Bitmap pin = BitmapFactory.decodeStream(form.openAsset("map_pin.png"));
            if (pin != null) {
                float pinSize = mapSize * 0.35f;

                float cx = mapRect.centerX();
                float cy = mapRect.centerY();

                canvas.drawBitmap(
                        pin,
                        null,
                        new RectF(
                                cx - pinSize / 2,
                                cy - pinSize,
                                cx + pinSize / 2,
                                cy
                        ),
                        null
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

} catch (Exception e) {
    e.printStackTrace();
}

    // === GOOGLE TEXT ===
    Paint google = new Paint(Paint.ANTI_ALIAS_FLAG);
    google.setColor(Color.WHITE);
    google.setTextSize(12 * dp);
    google.setFakeBoldText(true);
    google.setShadowLayer(3, 1, 1, Color.BLACK);
    canvas.drawText("Google", mapRect.left + 10 * dp, mapRect.bottom - 10 * dp, google);

    // === AREA TEXT ===
    float textX = mapRect.right + spacing;
    float y = top + padding + (12 * dp);

 // HEADER (sebenarnya tidak dipakai lagi di dalam card)
// jadi tidak perlu drawText di sini

// === TITLE ===
TextPaint title = new TextPaint(Paint.ANTI_ALIAS_FLAG);
title.setColor(Color.WHITE);
title.setTextSize(11 * dp);
title.setTypeface(fontMedium);

// 🔥 TITLE JANGAN TERLALU TURUN
float titleY = top + padding + (18 * dp);
canvas.drawText(extractMainLocation(addr) + ", Indonesia 🇮🇩", textX, titleY, title);

// === BODY ===
TextPaint body = new TextPaint(Paint.ANTI_ALIAS_FLAG);
body.setColor(Color.WHITE);
body.setTextSize(10 * dp);
body.setTypeface(fontRegular);

// 🔥 ADDRESS MULAI SETELAH TITLE (INI KUNCI)
y = titleY + (12 * dp);

// SPLIT ADDRESS
String[] parts = addr.split(",");
String line1 = "";
String line2 = "";

if (parts.length >= 4) {
    line1 = parts[0] + "," + parts[1] + "," + parts[2];
    line2 = addr.replace(line1 + ",", "");
} else {
    line1 = addr;
}

// BARIS 1
canvas.drawText(line1.trim(), textX, y, body);

// BARIS 2
y += 11 * dp;
canvas.drawText(line2.trim(), textX, y, body);

// PLUS CODE
y += 13 * dp;
canvas.drawText("3Q8V+JC8", textX, y, body);

// LAT LONG
y += 11 * dp;
canvas.drawText("Lat " + lat + " | Long " + lon, textX, y, body);

// DATE
y += 11 * dp;
canvas.drawText(formatTanggalIndonesia(time) + " GMT +07:00", textX, y, body);
    }

   private String formatTanggalIndonesia(String input) {
    try {
        java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        java.util.Date date = inputFormat.parse(input); // ✅ FIX DI SINI

        String[] hari = {"Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"};
        String[] bulan = {"Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"};

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date);

        return hari[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1] + ", "
                + cal.get(java.util.Calendar.DAY_OF_MONTH) + " "
                + bulan[cal.get(java.util.Calendar.MONTH)] + " "
                + cal.get(java.util.Calendar.YEAR)
                + String.format(" %02d:%02d WIB",
                cal.get(java.util.Calendar.HOUR_OF_DAY),
                cal.get(java.util.Calendar.MINUTE));

    } catch (Exception e) {
        return input;
    }
}

    private String extractMainLocation(String addr) {
        String[] parts = addr.split(",");
        for (String p : parts) {
            if (p.toLowerCase().contains("kecamatan") || p.toLowerCase().contains("kec")) return p.trim();
        }
        return parts.length > 0 ? parts[0] : "Lokasi";
    }

    private String fetchAddress(String lat, String lon) {
        try {
            URL url = new URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lon);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "GPSCameraPKH");
            Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
            String result = scanner.hasNext() ? scanner.next() : "";
            if (result.contains("display_name")) {
                int start = result.indexOf("display_name") + 15;
                return result.substring(start, result.indexOf("\"", start)).replace("\\\"", "");
            }
        } catch (Exception ignored) {}
        return "Alamat tidak ditemukan";
    }

    @SimpleEvent(description = "Berhasil")
    public void OnAddressFound(String address, String path) {
        EventDispatcher.dispatchEvent(this, "OnAddressFound", address, path);
    }

    @SimpleEvent(description = "Error")
    public void OnError(String message) {
        EventDispatcher.dispatchEvent(this, "OnError", message);
    }
}
