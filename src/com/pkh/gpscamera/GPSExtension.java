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

    // 1. KONFIGURASI DIMENSI & PADDING
    float padding = w * 0.04f;
    float cardWidth = w * 0.94f; // Sedikit lebih lebar agar elegan
    float cardHeight = (h > w) ? h * 0.28f : h * 0.38f; 

    float left = (w - cardWidth) / 2f;
    float top = h - cardHeight - (h * 0.04f); // Jarak dari bawah foto

    // 2. BACKGROUND UTAMA (HITAM TRANSPARAN)
    // Menggunakan warna hitam pekat dengan transparansi (Alpha)
    Paint bg = new Paint();
    bg.setColor(Color.BLACK);
    bg.setAlpha(165); // Nilai 0-255 (165 = sekitar 65% opacity)
    bg.setAntiAlias(true);

    RectF rect = new RectF(left, top, left + cardWidth, top + cardHeight);
    canvas.drawRoundRect(rect, 30, 30, bg);

    // 3. PETA (SISI KIRI)
    float mapSize = cardHeight * 0.85f; // Peta hampir setinggi kartu
    float mapLeft = left + (cardHeight * 0.075f);
    float mapTop = top + (cardHeight - mapSize) / 2f;

    try {
        // Menggunakan Yandex Static Maps (Satelit)
        String mapUrl = "https://static-maps.yandex.ru/1.x/?ll=" + lon + "," + lat + "&z=16&l=sat&size=450,450";
        URL url = new URL(mapUrl);
        Bitmap map = BitmapFactory.decodeStream(url.openConnection().getInputStream());

        if (map != null) {
            RectF mapRect = new RectF(mapLeft, mapTop, mapLeft + mapSize, mapTop + mapSize);
            Path path = new Path();
            path.addRoundRect(mapRect, 15, 15, Path.Direction.CW); // Radius peta

            canvas.save();
            canvas.clipPath(path);
            canvas.drawBitmap(map, null, mapRect, null);
            canvas.restore();
        }
    } catch (Exception ignored) {}

    // 4. HEADER "GPS MAP CAMERA" (RATA KANAN)
    // Bagian ini sekarang dikunci ke sisi kanan tabel data
    Paint headerBg = new Paint();
    headerBg.setColor(Color.parseColor("#333333")); // Hitam abu-abu gelap
    headerBg.setAlpha(220);
    headerBg.setAntiAlias(true);

    float headerWidth = w * 0.38f;
    float headerHeight = cardHeight * 0.18f;
    
    // Posisi: (Batas Kanan Card) - (Lebar Header) - (Margin sedikit)
    float headerLeft = (left + cardWidth) - headerWidth - 15; 
    float headerTop = top - (headerHeight * 0.75f); 

    RectF headerRect = new RectF(headerLeft, headerTop, headerLeft + headerWidth, headerTop + headerHeight);
    canvas.drawRoundRect(headerRect, 12, 12, headerBg);

    Paint hTextPaint = new Paint();
    hTextPaint.setColor(Color.WHITE);
    hTextPaint.setTextSize(w / 42f);
    hTextPaint.setAntiAlias(true);
    hTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

    // Menggambar teks di tengah kotak header
    String headerText = "📷 GPS Map Camera ✏️";
    float hTextX = headerRect.left + (headerRect.width() - hTextPaint.measureText(headerText)) / 2f;
    float hTextY = headerRect.centerY() + (hTextPaint.getTextSize() / 3f);
    canvas.drawText(headerText, hTextX, hTextY, hTextPaint);

    // 5. AREA TEKS & DATA GEOTAG (SISI KANAN PETA)
    float textLeft = mapLeft + mapSize + (padding * 0.8f);
    float textWidth = (left + cardWidth) - textLeft - (padding * 0.5f);

    // Paint untuk Judul (Besar & Bold)
    TextPaint titlePaint = new TextPaint();
    titlePaint.setColor(Color.WHITE);
    titlePaint.setAntiAlias(true);
    titlePaint.setTextSize(w / 19f); 
    titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

    // Paint untuk Detail (Normal)
    TextPaint normalPaint = new TextPaint();
    normalPaint.setColor(Color.WHITE);
    normalPaint.setAntiAlias(true);
    normalPaint.setTextSize(w / 36f); // Ukuran teks detail agar tidak bertumpuk

    // --- Menggambar Judul (Lokasi Utama) ---
    String fullTitle = extractMainLocation(addr) + ", Jawa Timur, Indonesia 🇮🇩";
    StaticLayout titleLayout = new StaticLayout(fullTitle, titlePaint, (int) textWidth, 
                               Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);

    canvas.save();
    canvas.translate(textLeft, top + (cardHeight * 0.12f));
    titleLayout.draw(canvas);
    
    // Geser ke bawah setelah judul untuk menggambar alamat
    canvas.translate(0, titleLayout.getHeight() + 5);
    
    // --- Menggambar Alamat Lengkap ---
    StaticLayout addrLayout = new StaticLayout(addr, normalPaint, (int) textWidth, 
                               Layout.Alignment.ALIGN_NORMAL, 1.1f, 0, false);
    addrLayout.draw(canvas);
    
    // Geser ke bawah lagi setelah alamat untuk koordinat & waktu
    canvas.translate(0, addrLayout.getHeight() + 8);
    
    // --- Menggambar Koordinat ---
    canvas.drawText("Lat " + lat + "° Long " + lon + "°", 0, 0, normalPaint);
    
    // --- Menggambar Waktu ---
    String waktuIndo = formatTanggalIndonesia(time);
    canvas.drawText(waktuIndo, 0, normalPaint.getTextSize() * 1.4f, normalPaint);
    
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
