package com.pkh.gpscamera;

import android.graphics.*;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.ComponentCategory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout;

@DesignerComponent(
        version = 4,
        description = "GPS Map Camera PKH - FINAL STABLE + FORMAT INDONESIA",
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    final String finalAddress = fetchAddress(inputLat, inputLong);

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;

                    Bitmap original = BitmapFactory.decodeFile(imagePath, options);

                    if (original == null) {
                        throw new Exception("Foto tidak ditemukan");
                    }

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
                        public void run() {
                            OnAddressFound(finalAddress, path);
                        }
                    });

                } catch (final Exception e) {
                    final String err = e.getMessage();
                    form.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            OnError(err);
                        }
                    });
                }
            }
        }).start();
    }

    private void drawByTemplate(android.graphics.Canvas canvas, String addr, String lat, String lon,
                            String time, int w, int h) {

    float padding = w * 0.04f;
    float cardWidth = w * 0.92f;
    float cardHeight = (h > w) ? h * 0.26f : h * 0.34f;

    float left = (w - cardWidth) / 2f;
    float top = h - cardHeight - (h * 0.04f);

    // ================= BACKGROUND CARD =================
    Paint bg = new Paint();
    bg.setColor(Color.parseColor("#4A4A4A"));
    bg.setAlpha(235);
    bg.setAntiAlias(true);

    RectF rect = new RectF(left, top, left + cardWidth, top + cardHeight);
    canvas.drawRoundRect(rect, 35, 35, bg);

    // ================= MAP =================
    float mapSize = cardHeight * 0.75f;
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

            Path path = new Path();
            path.addRoundRect(mapRect, 25, 25, Path.Direction.CW);

            canvas.save();
            canvas.clipPath(path);
            canvas.drawBitmap(map, null, mapRect, null);
            canvas.restore();
        }
    } catch (Exception ignored) {}

    // ================= HEADER MINI (KANAN ATAS) =================
    float headerWidth = cardWidth * 0.42f;
    float headerHeight = cardHeight * 0.22f;

    float headerLeft = left + cardWidth - headerWidth - padding * 0.3f;
    float headerTop = top - headerHeight * 0.6f;

    Paint headerBg = new Paint();
    headerBg.setColor(Color.parseColor("#6A6A6A"));
    headerBg.setAlpha(240);
    headerBg.setAntiAlias(true);

    RectF headerRect = new RectF(headerLeft, headerTop,
            headerLeft + headerWidth, headerTop + headerHeight);

    canvas.drawRoundRect(headerRect, 25, 25, headerBg);

    Paint headerText = new Paint();
    headerText.setColor(Color.WHITE);
    headerText.setTextSize(w / 36f);
    headerText.setAntiAlias(true);

    canvas.drawText("📷 GPS Map Camera ✏️",
            headerLeft + padding * 0.5f,
            headerTop + headerHeight * 0.65f,
            headerText);

    // ================= TEXT AREA =================
    float textLeft = mapLeft + mapSize + padding;
    float textWidth = cardWidth - mapSize - (padding * 2);

    TextPaint titlePaint = new TextPaint();
    titlePaint.setColor(Color.WHITE);
    titlePaint.setAntiAlias(true);
    titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
    titlePaint.setTextSize(w / 20f);

    TextPaint normalPaint = new TextPaint();
    normalPaint.setColor(Color.WHITE);
    normalPaint.setAntiAlias(true);
    normalPaint.setTextSize(w / 32f);

    // ================= JUDUL =================
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
    canvas.translate(textLeft, top + cardHeight * 0.20f);
    titleLayout.draw(canvas);
    canvas.restore();

    // ================= ALAMAT =================
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
    canvas.translate(textLeft, top + cardHeight * 0.45f);
    addrLayout.draw(canvas);
    canvas.restore();

    // ================= KOORDINAT =================
    float coordY = top + cardHeight * 0.78f;
    canvas.drawText("Lat " + lat + "°  Long " + lon + "°",
            textLeft, coordY, normalPaint);

    // ================= WAKTU =================
    String waktuIndo = formatTanggalIndonesia(time);

    canvas.drawText(waktuIndo,
            textLeft, coordY + (cardHeight * 0.12f),
            normalPaint);
}

    // ========================= FORMAT TANGGAL =========================
    private String formatTanggalIndonesia(String input) {
        try {
            java.text.SimpleDateFormat inputFormat =
                    new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");

            inputFormat.setLenient(true); // biar fleksibel

            java.util.Date date = inputFormat.parse(input);

            String[] hari = {"Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"};
            String[] bulan = {"Januari", "Februari", "Maret", "April", "Mei", "Juni",
                    "Juli", "Agustus", "September", "Oktober", "November", "Desember"};

            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(date);

            String namaHari = hari[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1];
            int tgl = cal.get(java.util.Calendar.DAY_OF_MONTH);
            String namaBulan = bulan[cal.get(java.util.Calendar.MONTH)];
            int tahun = cal.get(java.util.Calendar.YEAR);

            int jam = cal.get(java.util.Calendar.HOUR_OF_DAY);
            int menit = cal.get(java.util.Calendar.MINUTE);

            return namaHari + ", " + tgl + " " + namaBulan + " " + tahun +
                    String.format(" %02d:%02d WIB", jam, menit);

        } catch (Exception e) {
            return input;
        }
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
