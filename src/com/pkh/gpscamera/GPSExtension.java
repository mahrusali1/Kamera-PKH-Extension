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

    // 1. KONFIGURASI SKALA (Agar ukuran sp/dp sesuai di semua HP)
    float density = canvas.getDensity() / 160f;
    if (density == 0) density = 1.0f; 

    // Ambil nilai dari dimens.xml (8sp dan 10sp)
    float fontSizeTitle = 10 * density; 
    float fontSizeDetail = 8 * density;
    float padding = 8 * density;

    // Dimensi Card
    float cardWidth = w * 0.94f;
    float cardHeight = (h > w) ? h * 0.22f : h * 0.30f;
    float left = (w - cardWidth) / 2f;
    float top = h - cardHeight - (h * 0.04f);

    // 2. BACKGROUND (HITAM TRANSPARAN #CC000000)
    Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    bg.setColor(Color.BLACK);
    bg.setAlpha(204); // Ini adalah Hex CC (80% opacity)
    canvas.drawRoundRect(new RectF(left, top, left + cardWidth, top + cardHeight), 20, 20, bg);

    // 3. PETA (SISI KIRI)
    float mapSize = cardHeight - (padding * 2);
    float mapLeft = left + padding;
    float mapTop = top + padding;

    try {
        String mapUrl = "https://static-maps.yandex.ru/1.x/?ll=" + lon + "," + lat + "&z=16&l=sat&size=300,300";
        Bitmap map = BitmapFactory.decodeStream(new URL(mapUrl).openConnection().getInputStream());
        if (map != null) {
            RectF mapRect = new RectF(mapLeft, mapTop, mapLeft + mapSize, mapTop + mapSize);
            Path path = new Path();
            path.addRoundRect(mapRect, 10, 10, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(path);
            canvas.drawBitmap(map, null, mapRect, null);
            canvas.restore();
        }
    } catch (Exception ignored) {}

    // 4. HEADER "GPS MAP CAMERA" (RATA KANAN)
    String headerTxt = "📷 GPS Map Camera ✏️";
    Paint hTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    hTextPaint.setColor(Color.WHITE);
    hTextPaint.setTextSize(fontSizeDetail);
    hTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

    float hWidth = hTextPaint.measureText(headerTxt) + (padding * 2);
    float hHeight = fontSizeTitle * 1.8f;
    float hLeft = (left + cardWidth) - hWidth - 5; // Kunci Rata Kanan
    float hTop = top - (hHeight * 0.6f);

    Paint hBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    hBg.setColor(Color.parseColor("#333333"));
    hBg.setAlpha(230);
    canvas.drawRoundRect(new RectF(hLeft, hTop, hLeft + hWidth, hTop + hHeight), 10, 10, hBg);
    canvas.drawText(headerTxt, hLeft + padding, hTop + (hHeight * 0.7f), hTextPaint);

    // 5. AREA TEKS (URUTAN: KECAMATAN, KABUPATEN, NEGARA)
    float textLeft = mapLeft + mapSize + padding;
    float textWidth = (left + cardWidth) - textLeft - padding;

    // Logika Pemecah Alamat (Sesuaikan index split jika urutan alamat berbeda)
    String[] parts = addr.split(",");
    String kec = (parts.length > 2) ? parts[2].trim() : "Kecamatan";
    String kab = (parts.length > 3) ? parts[3].trim() : "Kabupaten";
    String judulFix = kec + ", " + kab + ", Indonesia 🇮🇩";

    // --- Paint Judul ---
    TextPaint tpTitle = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    tpTitle.setColor(Color.WHITE);
    tpTitle.setTextSize(fontSizeTitle);
    tpTitle.setTypeface(Typeface.DEFAULT_BOLD);

    // --- Paint Detail ---
    TextPaint tpDetail = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    tpDetail.setColor(Color.WHITE);
    tpDetail.setTextSize(fontSizeDetail);

    canvas.save();
    canvas.translate(textLeft, top + padding + (fontSizeTitle * 0.8f));

    // Gambar Judul
    StaticLayout slTitle = new StaticLayout(judulFix, tpTitle, (int) textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
    slTitle.draw(canvas);
    canvas.translate(0, slTitle.getHeight() + 2);

    // Gambar Alamat
    StaticLayout slAddr = new StaticLayout(addr, tpDetail, (int) textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
    slAddr.draw(canvas);
    canvas.translate(0, slAddr.getHeight() + 4);

    // Gambar Lat/Long
    canvas.drawText("Lat " + lat + "° Long " + lon + "°", 0, 0, tpDetail);
    
    // Gambar Waktu
    canvas.drawText(formatTanggalIndonesia(time), 0, fontSizeDetail * 1.4f, tpDetail);

    canvas.restore();
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
