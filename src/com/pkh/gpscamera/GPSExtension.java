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
    float cardHeight = mapSize + (padding * 1.3f);

    float left = (w - cardWidth) / 2f;
    float top = h - cardHeight - (15 * dp);

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
   Path cardPath = new Path();
float r = 9 * dp;

// MULAI dari kiri atas (setelah radius)
cardPath.moveTo(left + r, top);

// ATAS ke kanan (tanpa lengkung di kanan atas)
cardPath.lineTo(left + cardWidth, top);

// TURUN kanan
cardPath.lineTo(left + cardWidth, top + cardHeight - r);

// LENGKUNG kanan bawah
cardPath.quadTo(
        left + cardWidth, top + cardHeight,
        left + cardWidth - r, top + cardHeight
);

// BAWAH ke kiri
cardPath.lineTo(left + r, top + cardHeight);

// LENGKUNG kiri bawah
cardPath.quadTo(
        left, top + cardHeight,
        left, top + cardHeight - r
);

// NAIK kiri
cardPath.lineTo(left, top + r);

// LENGKUNG kiri atas
cardPath.quadTo(
        left, top,
        left + r, top
);

cardPath.close();

canvas.drawPath(cardPath, bg);

            // ================= HEADER FLOATING (TERPISAH) =================
String headerTxt = "GPS Map Camera";

// TEXT PAINT
TextPaint hText = new TextPaint(Paint.ANTI_ALIAS_FLAG);
hText.setColor(Color.WHITE);
hText.setTextSize(11 * dp);
hText.setTypeface(Typeface.DEFAULT_BOLD);
hText.setShadowLayer(3, 0, 0, Color.BLACK);

// HITUNG UKURAN
float textW = hText.measureText(headerTxt);
float hPadding = 10 * dp;
float hHeight = 20 * dp;
float hWidth = textW + (hPadding * 2) + (20 * dp); // tambah ruang untuk icon

// POSISI (NEMPEL DI ATAS CARD, RATA KANAN)
float hLeft = (left + cardWidth) - hWidth;
float hTop = top - hHeight;

// BACKGROUND HEADER
Paint hBg = new Paint(Paint.ANTI_ALIAS_FLAG);
hBg.setColor(Color.parseColor("#99000000"));
Path headerPath = new Path();

float radius = 8 * dp;

// Mulai dari kiri bawah
headerPath.moveTo(hLeft, hTop + hHeight);

// Garis bawah ke kanan (FLAT)
headerPath.lineTo(hLeft + hWidth, hTop + hHeight);

// Naik ke kanan atas (sebelum lengkung)
headerPath.lineTo(hLeft + hWidth, hTop + radius);

// Lengkung kanan atas
headerPath.quadTo(
        hLeft + hWidth, hTop,
        hLeft + hWidth - radius, hTop
);

// Garis atas ke kiri
headerPath.lineTo(hLeft + radius, hTop);

// Lengkung kiri atas
headerPath.quadTo(
        hLeft, hTop,
        hLeft, hTop + radius
);

// Turun ke kiri bawah (FLAT)
headerPath.lineTo(hLeft, hTop + hHeight);

// Tutup path
headerPath.close();

// DRAW
canvas.drawPath(headerPath, hBg);
            

// === ICON CAMERA ===
float iconSize = 14 * dp;
float headerIconX = hLeft + hPadding;
float headerIconY = hTop + (hHeight - iconSize) / 2;

try {
    Bitmap icon = BitmapFactory.decodeStream(form.openAsset("camera_icon.png"));
    if (icon != null) {
        canvas.drawBitmap(icon, null,
                new RectF(headerIconX, headerIconY, headerIconX + iconSize, headerIconY + iconSize),
                null);
    }
} catch (Exception e) {
    e.printStackTrace();
}

// === TEXT HEADER ===
float headerTextX = headerIconX + iconSize + (6 * dp);

// 🔥 CENTER VERTICAL PERFECT
Paint.FontMetrics fm = hText.getFontMetrics();
float headerTextY = hTop + (hHeight / 2) - ((fm.ascent + fm.descent) / 2);

canvas.drawText(headerTxt, headerTextX, headerTextY, hText);

            
           
    // === MAP (ROUNDED) ===
    
RectF mapRect = new RectF(
        left + padding,
        top + padding,
        left + padding + mapSize,
        top + padding + mapSize
);

try {
    URL url = new URL("https://static-maps.yandex.ru/1.x/?ll=" + lon + "," + lat + "&z=17&l=sat&size=300,300");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "GPSCameraPKH");
conn.setConnectTimeout(5000);
conn.setReadTimeout(5000);

Bitmap map = BitmapFactory.decodeStream(conn.getInputStream());

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
                                cx - pinSize / 2,          // Kiri (Tetap)
        cy - pinSize - (10 * dp),  // ATAS (Dikurangi agar naik)
        cx + pinSize / 2,          // Kanan (Tetap)
        cy - (10 * dp)             // BAWAH (Dikurangi agar naik)
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
    float y = top + padding + (6 * dp);

 // HEADER (sebenarnya tidak dipakai lagi di dalam card)
// jadi tidak perlu drawText di sini

// === TITLE ===
TextPaint title = new TextPaint(Paint.ANTI_ALIAS_FLAG);
title.setColor(Color.WHITE);
title.setTextSize(14 * dp);
title.setTypeface(fontMedium);

// 🔥 POSISI LEBIH ATAS (BIAR GA NUMPUK)
float titleY = top + padding - (13 * dp);

StaticLayout layout;

if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
    layout = StaticLayout.Builder
            .obtain(getWilayahIndonesia(addr), 0, getWilayahIndonesia(addr).length(), title, (int)(cardWidth - mapSize - 40*dp))
            .build();
} else {
    layout = new StaticLayout(
            getWilayahIndonesia(addr),
            title,
            (int)(cardWidth - mapSize - 40*dp),
            Layout.Alignment.ALIGN_NORMAL,
            1.0f,
            0.0f,
            false
    );
}

canvas.save();
canvas.translate(textX, titleY);
layout.draw(canvas);
canvas.restore();

// === BODY ===
// === BODY (GABUNGAN ALAMAT) ===
TextPaint body = new TextPaint(Paint.ANTI_ALIAS_FLAG);
body.setColor(Color.WHITE);
body.setTextSize(9 * dp);
body.setTypeface(fontRegular);

// Menggabungkan semua bagian alamat menjadi satu teks panjang
String[] parts = addr.split(",");
StringBuilder fullAddrBuilder = new StringBuilder();
for (int i = 0; i < parts.length; i++) {
    fullAddrBuilder.append(parts[i].trim());
    if (i < parts.length - 1) fullAddrBuilder.append(", ");
}
String fullAddress = fullAddrBuilder.toString();

// Menggunakan StaticLayout agar alamat otomatis turun baris jika kepanjangan
StaticLayout addrLayout;
int addrWidth = (int)(cardWidth - mapSize - 40 * dp);

if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
    addrLayout = StaticLayout.Builder.obtain(fullAddress, 0, fullAddress.length(), body, addrWidth).build();
} else {
    addrLayout = new StaticLayout(fullAddress, body, addrWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
}

// Jarak dari Title ke Alamat (12 dp agar renggang)
y = titleY + layout.getHeight() + (2 * dp);

canvas.save();
canvas.translate(textX, y);
addrLayout.draw(canvas);
canvas.restore();

// Posisi Lat Long dihitung dari tinggi alamat yang baru
y += addrLayout.getHeight() + (8 * dp);
canvas.drawText("Lat " + lat + " | Long " + lon, textX, y, body);

// Posisi Tanggal
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

   private String fetchAddress(String lat, String lon) {
    try {
        URL url = new URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lon);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "GPSCameraPKH");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
            
        Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
        String result = scanner.hasNext() ? scanner.next() : "";

        // 🔥 PARSE JSON
        org.json.JSONObject json = new org.json.JSONObject(result);
        org.json.JSONObject address = json.getJSONObject("address");

      String desa = address.optString("hamlet",
             address.optString("village",
             address.optString("suburb",
             address.optString("neighbourhood", ""))));

String kecamatan = address.optString("subdistrict",
                   address.optString("town", ""));

String kabupaten = address.optString("county",
                  address.optString("city", ""));

String provinsi = address.optString("state", "");
   // 🔥 PROVINSI DIGANTI FIX
        String provinsi = "Jawa Timur";
// 🔥 TARUH DI SINI (SEBELUM RETURN)
if (kecamatan.equals("") || kecamatan.equals(kabupaten)) {
    kecamatan = kabupaten;
}

return desa + "," + kecamatan + "," + kabupaten + "," + provinsi;

    } catch (Exception e) {
        return "Lokasi tidak ditemukan";
    }
}

    

        // 🔥 TARUH DI SINI (SETELAH fetchAddress)
private String getWilayahIndonesia(String addr) {
    try {

            // 🔥 TARUH DI SINI (PALING ATAS)
        if (!addr.contains(",")) {
            return addr;
        }
        String[] parts = addr.split(",");

        String desa = parts.length > 0 ? parts[0].trim() : "";
        String kecamatan = parts.length > 1 ? parts[1].trim() : "";
        String kabupaten = parts.length > 2 ? parts[2].trim() : "";
        String provinsi = parts.length > 3 ? parts[3].trim() : "";

        // 🔥 TARUH DI SINI
        if (kabupaten.equals("")) {
            kabupaten = kecamatan;
        }

       return desa +
       ", Kab. " + kabupaten +
       ", " + provinsi + ", Indonesia 🇮🇩";

    } catch (Exception e) {
        return "Indonesia 🇮🇩";
    }
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
