package com.pkh.gpscamera;

import android.graphics.*;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.ComponentCategory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

@DesignerComponent(
        version = 5,
        description = "GPS Map Camera PKH - STABLE BUILD",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "images/extension.png"
)
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET, android.permission.WRITE_EXTERNAL_STORAGE, android.permission.READ_EXTERNAL_STORAGE")
public class GPSExtension extends AndroidNonvisibleComponent {

    private Typeface fontMedium = null;
    private Typeface fontRegular = null;

    public GPSExtension(ComponentContainer container) {
        super(container.$form());
    }

    @SimpleFunction(description = "Generate Watermark GPS Camera")
    public void GenerateWatermark(final String imagePath, final String inputLat, final String inputLong,
                                  final String inputDateTime, final String saveLocation,
                                  final String fileName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap original = null;
                Bitmap bitmap = null;
                try {
                    final String finalAddress = fetchAddress(inputLat, inputLong);
                    
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;
                    original = BitmapFactory.decodeFile(imagePath, options);
                    
                    if (original == null) throw new Exception("File foto tidak terbaca");

                    bitmap = original.copy(Bitmap.Config.ARGB_8888, true);
                    original.recycle(); 
                    original = null;

                    android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                    drawByTemplate(canvas, finalAddress, inputLat, inputLong, inputDateTime,
                            bitmap.getWidth(), bitmap.getHeight());

                    File dir = new File(saveLocation);
                    if (!dir.exists()) dir.mkdirs();

                    File outFile = new File(dir, fileName);
                    FileOutputStream out = new FileOutputStream(outFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    out.close();

                    final String path = outFile.getAbsolutePath();
                    form.runOnUiThread(new Runnable() {
                        @Override
                        public void run() { OnAddressFound(finalAddress, path); }
                    });

                } catch (final Exception e) {
                    if (original != null) original.recycle();
                    if (bitmap != null) bitmap.recycle();
                    final String msg = e.getMessage();
                    form.runOnUiThread(new Runnable() {
                        @Override
                        public void run() { OnError(msg); }
                    });
                }
            }
        }).start();
    }

    private void loadFonts() {
        if (fontMedium != null) return;
        try {
            fontMedium = Typeface.createFromAsset(form.getAssets(), "sfdisplay_medium.TTF");
            fontRegular = Typeface.createFromAsset(form.getAssets(), "sfuitext_regular.otf");
        } catch (Exception e) {
            fontMedium = Typeface.DEFAULT_BOLD;
            fontRegular = Typeface.DEFAULT;
        }
    }

    private void drawByTemplate(android.graphics.Canvas canvas, String addr, String lat, String lon,
                            String time, int w, int h) {
        loadFonts();
        float dp = w / 360f;
        float padding = 12 * dp;
        float mapSize = 65 * dp;
        float spacing = 10 * dp;

        float cardWidth = w * 0.94f;
        float cardHeight = mapSize + (padding * 1.5f);
        float left = (w - cardWidth) / 2f;
        float top = h - cardHeight - (10 * dp);

        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.parseColor("#AA000000"));
        canvas.drawRoundRect(new RectF(left, top, left + cardWidth, top + cardHeight), 8 * dp, 8 * dp, bg);

        RectF mapRect = new RectF(left + padding, top + padding, left + padding + mapSize, top + padding + mapSize);
        drawMap(canvas, mapRect, lat, lon, dp);

        float textX = mapRect.right + spacing;
        float maxWidth = (left + cardWidth) - textX - padding;

        TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(13 * dp);
        titlePaint.setTypeface(fontMedium);

        String fullWilayah = getWilayahIndonesia(addr);
        StaticLayout titleLayout = StaticLayout.Builder.obtain(fullWilayah, 0, fullWilayah.length(), titlePaint, (int)maxWidth)
                .build();

        canvas.save();
        canvas.translate(textX, top + padding);
        titleLayout.draw(canvas);
        canvas.restore();

        TextPaint bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.WHITE);
        bodyPaint.setTextSize(9 * dp);
        bodyPaint.setTypeface(fontRegular);

        float currentY = top + padding + titleLayout.getHeight() + (4 * dp);
        String[] parts = addr.split(",");
        String detailAddr = (parts.length > 0 ? parts[0].trim() : "");
        canvas.drawText(detailAddr, textX, currentY, bodyPaint);
        
        currentY += 10 * dp;
        canvas.drawText("Lat " + lat + " | Long " + lon, textX, currentY, bodyPaint);
        
        currentY += 10 * dp;
        canvas.drawText(formatTanggalIndonesia(time) + " GMT+07:00", textX, currentY, bodyPaint);
    }

    private void drawMap(Canvas canvas, RectF rect, String lat, String lon, float dp) {
        try {
            URL url = new URL("https://static-maps.yandex.ru/1.x/?ll=" + lon + "," + lat + "&z=16&l=sat&size=250,250");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(4000);
            Bitmap mapBmp = BitmapFactory.decodeStream(conn.getInputStream());

            if (mapBmp != null) {
                Path clipPath = new Path();
                clipPath.addRoundRect(rect, 8 * dp, 8 * dp, Path.Direction.CW);
                canvas.save();
                canvas.clipPath(clipPath);
                canvas.drawBitmap(mapBmp, null, rect, null);
                canvas.restore();
                mapBmp.recycle();
            }
        } catch (Exception e) {
            Paint placeholder = new Paint();
            placeholder.setColor(Color.DKGRAY);
            canvas.drawRoundRect(rect, 8 * dp, 8 * dp, placeholder);
        }
    }

    private String getWilayahIndonesia(String addr) {
        if (addr == null || !addr.contains(",")) return "Lokasi Indonesia 🇮🇩";
        String[] p = addr.split(",");
        try {
            String desa = p[0].trim();
            String kec = p.length > 1 ? p[1].trim() : "";
            String kab = p.length > 2 ? p[2].trim() : "";
            return desa + ", Kec. " + kec + ", " + kab + " 🇮🇩";
        } catch (Exception e) {
            return "Lokasi Terdeteksi 🇮🇩";
        }
    }

    private String formatTanggalIndonesia(String input) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
            java.util.Date date = sdf.parse(input);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(date);
            String[] hari = {"Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"};
            String[] bulan = {"Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
            return hari[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1] + ", "
                    + cal.get(java.util.Calendar.DAY_OF_MONTH) + " "
                    + bulan[cal.get(java.util.Calendar.MONTH)] + " "
                    + cal.get(java.util.Calendar.YEAR) + " "
                    + String.format("%02d:%02d WIB", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE));
        } catch (Exception e) { return input; }
    }

    private String fetchAddress(String lat, String lon) {
        try {
            URL url = new URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lon);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
            String res = s.hasNext() ? s.next() : "";
            org.json.JSONObject j = new org.json.JSONObject(res).getJSONObject("address");
            String d = j.optString("village", j.optString("suburb", "Unknown"));
            String k = j.optString("subdistrict", j.optString("city_district", ""));
            String b = j.optString("county", j.optString("city", ""));
            String p = j.optString("state", "");
            return d + "," + k + "," + b + "," + p;
        } catch (Exception e) { return "Alamat tidak ditemukan, , , "; }
    }

    @SimpleEvent public void OnAddressFound(String address, String path) { EventDispatcher.dispatchEvent(this, "OnAddressFound", address, path); }
    @SimpleEvent public void OnError(String message) { EventDispatcher.dispatchEvent(this, "OnError", message); }
}
