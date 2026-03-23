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

    float cardPadding = 12 * dp;
    float mapSize = 100 * dp;
    float spacing = 8 * dp;

    float cardWidth = w * 0.92f;
    float cardHeight = mapSize + (cardPadding * 2);

    float left = (w - cardWidth) / 2f;
    float top = h - cardHeight - (20 * dp);

    // Background (classic_template_bg)
    Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    bg.setColor(Color.parseColor("#99000000"));
    canvas.drawRoundRect(new RectF(left, top, left + cardWidth, top + cardHeight),
            18 * dp, 18 * dp, bg);

    // === MAP ===
    RectF mapRect = new RectF(
            left + cardPadding,
            top + cardPadding,
            left + cardPadding + mapSize,
            top + cardPadding + mapSize
    );

    try {
        Bitmap map = BitmapFactory.decodeFile("/mnt/data/map.png");
        canvas.drawBitmap(map, null, mapRect, null);
    } catch (Exception e) {}

    // === TEXT AREA ===
    float textX = mapRect.right + spacing;
    float y = top + cardPadding + (20 * dp);

    TextPaint title = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    title.setColor(Color.WHITE);
    title.setTextSize(14 * dp);
    title.setTypeface(Typeface.DEFAULT_BOLD);

    canvas.drawText(extractMainLocation(addr), textX, y, title);

    // ADDRESS
    TextPaint body = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    body.setColor(Color.WHITE);
    body.setTextSize(10 * dp);

    StaticLayout sl = StaticLayout.Builder
            .obtain(addr, 0, addr.length(), body, (int)(cardWidth - mapSize - 40))
            .build();

    canvas.save();
    canvas.translate(textX, y + (15 * dp));
    sl.draw(canvas);
    canvas.restore();

    float textBottom = y + (15 * dp) + sl.getHeight();

    // LAT LONG
    canvas.drawText("Lat " + lat + " | Long " + lon,
            textX, textBottom + (15 * dp), body);

    // DATE
    canvas.drawText(formatTanggalIndonesia(time),
            textX, textBottom + (30 * dp), body);
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
