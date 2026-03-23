package com.pkh.gpscamera;

import android.graphics.*;
import android.media.ExifInterface;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.ComponentCategory;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@DesignerComponent(version = 1,
    description = "Ekstensi GPS Map Camera PKH - Watermark Otomatis",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "ai_images/extension.png")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET, android.permission.READ_EXTERNAL_STORAGE, android.permission.WRITE_EXTERNAL_STORAGE, android.permission.ACCESS_FINE_LOCATION")
public class GPSExtension extends AndroidNonvisibleComponent {

    public GPSExtension(ComponentContainer container) {
        super(container.$form());
    }

    @SimpleFunction(description = "Proses foto dengan watermark Alamat, GPS, dan Waktu.")
    public void ProsesFotoPKH(final String pathGambar, final String latInput, final String lonInput, final String folderSimpan, final String namaFile) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String finalLat = (latInput == null || latInput.isEmpty()) ? "" : latInput.trim();
                    String finalLon = (lonInput == null || lonInput.isEmpty()) ? "" : lonInput.trim();

                    if (finalLat.isEmpty() || finalLon.isEmpty()) {
                        ExifInterface exif = new ExifInterface(pathGambar);
                        float[] latLong = new float[2];
                        if (exif.getLatLong(latLong)) {
                            finalLat = String.valueOf(latLong[0]);
                            finalLon = String.valueOf(latLong[1]);
                        }
                    }

                    String alamat = ambilAlamat(finalLat, finalLon);
                    String waktu = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

                    Bitmap bitmapAsli = BitmapFactory.decodeFile(pathGambar);
                    Bitmap mutableBitmap = bitmapAsli.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(mutableBitmap);

                    Paint pTeks = new Paint();
                    pTeks.setColor(Color.WHITE);
                    pTeks.setTextSize(bitmapAsli.getWidth() / 28);
                    pTeks.setShadowLayer(5f, 0f, 0f, Color.BLACK);
                    pTeks.setAntiAlias(true);

                    Paint pBox = new Paint();
                    pBox.setColor(Color.BLACK);
                    pBox.setAlpha(140);
                    float boxHeight = bitmapAsli.getHeight() / 5.5f;
                    canvas.drawRect(0, mutableBitmap.getHeight() - boxHeight, mutableBitmap.getWidth(), mutableBitmap.getHeight(), pBox);

                    float xPos = 60;
                    float yBase = mutableBitmap.getHeight() - (boxHeight * 0.75f);
                    canvas.drawText("Alamat: " + alamat, xPos, yBase, pTeks);
                    canvas.drawText("GPS: " + finalLat + ", " + finalLon, xPos, yBase + (pTeks.getTextSize() * 1.4f), pTeks);
                    canvas.drawText("Waktu: " + waktu + " | Petugas PKH", xPos, yBase + (pTeks.getTextSize() * 2.8f), pTeks);

                    // PERBAIKAN: Menggunakan java.io.File secara eksplisit
                    java.io.File direktori = new java.io.File(folderSimpan);
                    if (!direktori.exists()) direktori.mkdirs();
                    java.io.File fileHasil = new java.io.File(direktori, namaFile);
                    
                    FileOutputStream out = new FileOutputStream(fileHasil);
                    mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    out.close();

                    final String pathHasil = fileHasil.getAbsolutePath();
                    
                    form.runOnUiThread(new Runnable() {
                        @Override
                        public void run() { Selesai(pathHasil); }
                    });

                } catch (Exception e) {
                    final String err = e.getMessage();
                    form.runOnUiThread(new Runnable() {
                        @Override
                        public void run() { Error(err); }
                    });
                }
            }
        }).start();
    }

    private String ambilAlamat(String lat, String lon) {
        if (lat.isEmpty() || lon.isEmpty()) return "Koordinat tidak tersedia";
        try {
            URL url = new URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lon);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "AppPKH");
            Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
            String res = s.hasNext() ? s.next() : "";
            if (res.contains("display_name")) {
                int start = res.indexOf("display_name") + 15;
                int end = res.indexOf("\"", start);
                return res.substring(start, end);
            }
            return "Lokasi: " + lat + ", " + lon;
        } catch (Exception e) { return "Alamat gagal dimuat"; }
    }

    @SimpleEvent public void Selesai(String path) { EventDispatcher.dispatchEvent(this, "Selesai", path); }
    @SimpleEvent public void Error(String pesan) { EventDispatcher.dispatchEvent(this, "Error", pesan); }
}
