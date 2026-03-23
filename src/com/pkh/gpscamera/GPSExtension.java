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

@DesignerComponent(
        version = 2,
        description = "GPS Map Camera PKH - Final UI Mirip Asli (Auto Wrap + Responsive)",
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

    // ========================= MAIN FUNCTION =========================
    @SimpleFunction(description = "Generate Watermark GPS Camera")
    public void GenerateWatermark(final String imagePath,
                                  final String inputLat,
                                  final String inputLong,
                                  final String inputDateTime,
                                  final String saveLocation,
                                  final String fileName,
                                  final int templateType) {

        new Thread(() -> {
            try {

                final String finalAddress = fetchAddress(inputLat, inputLong);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;

                Bitmap original = BitmapFactory.decodeFile(imagePath, options);

                if (original == null) {
                    throw new Exception("Foto tidak ditemukan");
                }

                Bitmap bitmap = original.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(bitmap);

                drawByTemplate(canvas, finalAddress, inputLat, inputLong, inputDateTime,
                        bitmap.getWidth(), bitmap.getHeight());

                File dir = new File(saveLocation);
                if (!dir.exists()) dir.mkdirs();

                File outFile = new File(dir, fileName);
                FileOutputStream out = new FileOutputStream(outFile);

                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
                out.close();

                final String path = outFile.getAbsolutePath();

                form.runOnUiThread(() ->
                        OnAddressFound(finalAddress, path)
                );

            } catch (Exception e) {
                String err = e.getMessage();
                form.runOnUiThread(() ->
                        OnError(err)
                );
            }
        }).start();
    }

    // ========================= DRAW UI =========================
    private void drawByTemplate(Canvas canvas, String addr, String lat, String lon,
                                String time, int w, int h) {

        float padding = w * 0.04f;
        float cardWidth = w * 0.92f;

        float cardHeight = (h > w) ? h * 0.22f : h * 0.32f;

        float left = (w - cardWidth) / 2f;
        float top = h - cardHeight - (h * 0.04f);

        // === BACKGROUND ===
        Paint bg = new Paint();
        bg.setColor(Color.parseColor("#4A4A4A"));
        bg.setAlpha(230);
        bg.setAntiAlias(true);

        RectF rect = new RectF(left, top, left + cardWidth, top + cardHeight);
        canvas.drawRoundRect(rect, 35, 35, bg);

        // === MAP ===
        float mapSize = cardHeight * 0.78f;
        float mapLeft = left + padding * 0.6f;
        float mapTop = top + (cardHeight - mapSize) / 2f;

        try {
            String mapUrl = "https://static-maps.yandex.ru/1.x/?ll="
                    + lon + "," + lat + "&z=17&l=sat&size=300,300";

            URL url = new URL(mapUrl);
            Bitmap map = BitmapFactory.decodeStream(url.openConnection().getInputStream());

            if (map != null) {
                RectF mapRect = new RectF(mapLeft, mapTop,
                        mapLeft + mapSize, mapTop + mapSize);

                Paint mapPaint = new Paint();
                mapPaint.setAntiAlias(true);

                Path path = new Path();
                path.addRoundRect(mapRect, 25, 25, Path.Direction.CW);

                canvas.save();
                canvas.clipPath(path);
                canvas.drawBitmap(map, null, mapRect, mapPaint);
                canvas.restore();
            }

        } catch (Exception ignored) {}

        // === TEXT AREA ===
        float textLeft = mapLeft + mapSize + padding;
        float textWidth = cardWidth - mapSize - (padding * 2);

        TextPaint titlePaint = new TextPaint();
        titlePaint.setColor(Color.WHITE);
        titlePaint.setAntiAlias(true);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        titlePaint.setTextSize(w / 22f);

        TextPaint normalPaint = new TextPaint();
        normalPaint.setColor(Color.WHITE);
        normalPaint.setAntiAlias(true);
        normalPaint.setTextSize(w / 32f);

        // === HEADER ===
        normalPaint.setTextSize(w / 36f);
        canvas.drawText("GPS Map Camera", textLeft,
                top + (cardHeight * 0.18f), normalPaint);

        // === TITLE ===
        String mainLoc = extractMainLocation(addr);

        StaticLayout titleLayout = new StaticLayout(
                mainLoc + ", Jawa Timur, Indonesia 🇮🇩",
                titlePaint,
                (int) textWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1.1f,
                0,
                false
        );

        canvas.save();
        canvas.translate(textLeft, top + (cardHeight * 0.28f));
        titleLayout.draw(canvas);
        canvas.restore();

        // === ADDRESS ===
        StaticLayout addrLayout = new StaticLayout(
                addr,
                normalPaint,
                (int) textWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1.1f,
                0,
                false
        );

        canvas.save();
        canvas.translate(textLeft, top + (cardHeight * 0.50f));
        addrLayout.draw(canvas);
        canvas.restore();

        // === COORDINATE ===
        float infoY = top + (cardHeight * 0.80f);
        normalPaint.setTextSize(w / 34f);

        canvas.drawText("Lat " + lat + "°   Long " + lon + "°",
                textLeft, infoY, normalPaint);

        // === TIME ===
        canvas.drawText(time + " GMT +07:00",
                textLeft, infoY + (cardHeight * 0.12f), normalPaint);
    }

    // ========================= EXTRACT LOCATION =========================
    private String extractMainLocation(String addr) {
        String[] parts = addr.split(",");

        for (String p : parts) {
            String lower = p.toLowerCase();
            if (lower.contains("kecamatan") || lower.contains("kec")) {
                return p.trim();
            }
        }

        return parts.length > 0 ? parts[0] : "Lokasi";
    }

    // ========================= GET ADDRESS =========================
    private String fetchAddress(String lat, String lon) {
        try {
            URL url = new URL(
                    "https://nominatim.openstreetmap.org/reverse?format=json&lat="
                            + lat + "&lon=" + lon
            );

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "GPSCameraPKH");

            Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
            String result = scanner.hasNext() ? scanner.next() : "";

            if (result.contains("display_name")) {
                int start = result.indexOf("display_name") + 15;
                return result.substring(start, result.indexOf("\"", start));
            }

        } catch (Exception ignored) {}

        return "Alamat tidak ditemukan";
    }

    // ========================= EVENTS =========================
    @SimpleEvent(description = "Berhasil")
    public void OnAddressFound(String address, String path) {
        EventDispatcher.dispatchEvent(this, "OnAddressFound", address, path);
    }

    @SimpleEvent(description = "Error")
    public void OnError(String message) {
        EventDispatcher.dispatchEvent(this, "OnError", message);
    }
}
