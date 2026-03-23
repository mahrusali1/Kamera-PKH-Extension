package com.pkh.gpscamera;

import android.graphics.*;
import android.text.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GPSExtension {

    // Fungsi Utama untuk menggambar Geotag ke Foto
    public void drawByTemplate(Canvas canvas, String addr, String lat, String lon, String time, int w, int h) {

        // 1. PENYESUAIAN SKALA (Menggunakan standar 7sp & 10sp dari dimensi Bapak)
        float density = canvas.getDensity() / 160f;
        if (density <= 0) density = 1.0f; 

        float fontSizeTitle = 10 * density; // Judul
        float fontSizeDetail = 7 * density;  // Detail alamat & koordinat
        float padding = 8 * density;

        // Dimensi Kotak Hitam (Card)
        float cardWidth = w * 0.94f;
        float cardHeight = (h > w) ? h * 0.24f : h * 0.32f; 
        float left = (w - cardWidth) / 2f;
        float top = h - cardHeight - (h * 0.04f); // Jarak dari bawah foto

        // 2. BACKGROUND HITAM TRANSPARAN (80% Opacity)
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.BLACK);
        bg.setAlpha(204); 
        canvas.drawRoundRect(new RectF(left, top, left + cardWidth, top + cardHeight), 20, 20, bg);

        // 3. PETA SATELIT (SISI KIRI)
        float mapSize = cardHeight * 0.85f;
        float mapLeft = left + padding;
        float mapTop = top + (cardHeight - mapSize) / 2f;

        try {
            // Zoom 17 agar pas (tidak terlalu jauh)
            String mapUrl = "https://static-maps.yandex.ru/1.x/?ll=" + lon + "," + lat + "&z=17&l=sat&size=400,400";
            Bitmap map = BitmapFactory.decodeStream(new URL(mapUrl).openConnection().getInputStream());
            if (map != null) {
                RectF mapRect = new RectF(mapLeft, mapTop, mapLeft + mapSize, mapTop + mapSize);
                Path path = new Path();
                path.addRoundRect(mapRect, 12, 12, Path.Direction.CW);
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
        float hHeight = fontSizeTitle * 1.6f;
        float hLeft = (left + cardWidth) - hWidth - 10; // Kunci Posisi Kanan
        float hTop = top - (hHeight * 0.65f); // Menempel di atas garis box

        Paint hBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        hBg.setColor(Color.parseColor("#333333"));
        hBg.setAlpha(255);
        canvas.drawRoundRect(new RectF(hLeft, hTop, hLeft + hWidth, hTop + hHeight), 12, 12, hBg);
        canvas.drawText(headerTxt, hLeft + padding, hTop + (hHeight * 0.72f), hTextPaint);

        // 5. AREA DATA TEKS (JUDUL KECAMATAN & KABUPATEN)
        float textLeft = mapLeft + mapSize + (padding * 1.2f);
        float textWidth = (left + cardWidth) - textLeft - padding;

        // Logika memecah alamat untuk mendapatkan Kecamatan & Kabupaten
        String[] parts = addr.split(",");
        String kec = "";
        String kab = "";

        if (parts.length >= 4) {
            // Mengambil dari posisi belakang (biasanya format Google Geocoder)
            kec = parts[parts.length - 4].trim(); 
            kab = parts[parts.length - 3].trim(); 
        } else if (parts.length >= 2) {
            kec = parts[0].trim();
            kab = parts[1].trim();
        } else {
            kec = addr;
        }
        
        String judulFix = kec + (kab.isEmpty() ? "" : ", " + kab) + ", Indonesia 🇮🇩";

        TextPaint tpTitle = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tpTitle.setColor(Color.WHITE);
        tpTitle.setTextSize(fontSizeTitle);
        tpTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextPaint tpDetail = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tpDetail.setColor(Color.WHITE);
        tpDetail.setTextSize(fontSizeDetail);

        canvas.save();
        // Atur posisi awal penulisan teks
        canvas.translate(textLeft, top + padding + (fontSizeTitle * 0.7f));

        // Gambar Judul
        StaticLayout slTitle = new StaticLayout(judulFix, tpTitle, (int) textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
        slTitle.draw(canvas);
        
        // Geser ke bawah dinamis agar tidak menumpuk
        canvas.translate(0, slTitle.getHeight() + (2 * density));

        // Gambar Alamat Lengkap
        StaticLayout slAddr = new StaticLayout(addr, tpDetail, (int) textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
        slAddr.draw(canvas);
        
        // Geser lagi untuk baris koordinat
        canvas.translate(0, slAddr.getHeight() + (4 * density));

        // Gambar Lat Long
        canvas.drawText("Lat " + lat + "° Long " + lon + "°", 0, 0, tpDetail);
        
        // Gambar Waktu (di bawah Lat Long)
        canvas.drawText(formatTanggalIndonesia(time), 0, fontSizeDetail * 1.5f, tpDetail);

        canvas.restore();
    }

    // Fungsi pembantu untuk format tanggal
    private String formatTanggalIndonesia(String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", new Locale("id", "ID"));
            return sdf.format(new Date(Long.parseLong(time)));
        } catch (Exception e) {
            return time;
        }
    }
}
