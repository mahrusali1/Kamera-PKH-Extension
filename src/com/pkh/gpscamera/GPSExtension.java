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
    float mapSize = 100 * dp;
    float spacing = 10 * dp;

    float cardWidth = w * 0.94f;
    float cardHeight = mapSize + (padding * 2);

    float left = (w - cardWidth) / 2f;
    float top = h - cardHeight - (20 * dp);

    // === BACKGROUND ===
    Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    bg.setColor(Color.parseColor("#99000000"));
    canvas.drawRoundRect(
            new RectF(left, top, left + cardWidth, top + cardHeight),
            25 * dp, 25 * dp, bg
    );

    // === MAP ===
    RectF mapRect = new RectF(
            left + padding,
            top + padding,
            left + padding + mapSize,
            top + padding + mapSize
    );

    try {
        URL url = new URL("https://static-maps.yandex.ru/1.x/?ll=" + lon + "," + lat + "&z=17&l=sat&size=300,300");
        Bitmap map = BitmapFactory.decodeStream(url.openConnection().getInputStream());
        canvas.drawBitmap(map, null, mapRect, null);
    } catch (Exception e) {}

    // === GOOGLE TEXT ===
    Paint google = new Paint(Paint.ANTI_ALIAS_FLAG);
    google.setColor(Color.WHITE);
    google.setTextSize(14 * dp);
    google.setFakeBoldText(true);
    canvas.drawText("Google", mapRect.left + 10 * dp, mapRect.bottom - 10 * dp, google);

    // === TEXT AREA ===
    float textX = mapRect.right + spacing;
    float y = top + padding + (15 * dp);

    // Header kecil
    TextPaint header = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    header.setColor(Color.WHITE);
    header.setAlpha(160);
    header.setTextSize(12 * dp);
    canvas.drawText("GPS Map Camera", textX, y, header);

    // TITLE BESAR
    TextPaint title = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    title.setColor(Color.WHITE);
    title.setTextSize(18 * dp);
    title.setTypeface(Typeface.DEFAULT_BOLD);

    y += 20 * dp;
    canvas.drawText(extractMainLocation(addr) + ", Indonesia 🇮🇩", textX, y, title);

    // ADDRESS
    TextPaint body = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    body.setColor(Color.WHITE);
    body.setTextSize(12 * dp);

    y += 10 * dp;

    StaticLayout sl = StaticLayout.Builder
            .obtain(addr, 0, addr.length(), body, (int)(cardWidth - mapSize - 50))
            .build();

    canvas.save();
    canvas.translate(textX, y);
    sl.draw(canvas);
    canvas.restore();

    y += sl.getHeight() + 10 * dp;

    // PLUS CODE (dummy dulu)
    canvas.drawText("3Q8V+JC8", textX, y, body);

    y += 15 * dp;

    // LAT LONG
    canvas.drawText("Lat " + lat + " | Long " + lon, textX, y, body);

    y += 15 * dp;

    // DATE (FORMAT MIRIP ASLI)
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
