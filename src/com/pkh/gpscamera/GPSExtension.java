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
        description = "GPS Map Camera PKH - STABLE & OPTIMIZED",
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
                                  final String fileName, final int templateType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap original = null;
                Bitmap bitmap = null;
                try {
                    // 1. Fetch data dari network dulu sebelum proses gambar
                    final String finalAddress = fetchAddress(inputLat, inputLong);
                    
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;
                    original = BitmapFactory.decodeFile(imagePath, options);
                    
                    if (original == null) throw new Exception("File foto tidak terbaca");

                    bitmap = original.copy(Bitmap.Config.ARGB_8888, true);
                    // Bebaskan memory original segera
                    original.recycle(); 
                    original = null;

                    android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

                    // 2. Draw watermark
                    drawByTemplate(canvas, finalAddress, inputLat, inputLong, inputDateTime,
                            bitmap.getWidth(), bitmap.getHeight());

                    // 3. Simpan File
                    File dir = new File(saveLocation);
                    if (!dir.exists()) dir.mkdirs();

                    File outFile = new File(dir, fileName);
                    FileOutputStream out = new FileOutputStream(outFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out); // 90% sudah cukup jernih
                    out.close();

                    final String path = outFile.getAbsolutePath();
                    form.runOnUiThread(new Runnable() {
                        @Override
                        public void run() { OnAddressFound(finalAddress, path); }
                    });

                } catch (final Exception e) {
                    if (original != null) original.recycle();
                    if (bitmap != null) bitmap.recycle();
                    form.runOnUiThread(new Runnable() {
                        @Override
                        public void run() { OnError(e.getMessage()); }
                    });
                }
            }
        }).start();
    }

    private void loadFonts() {
        if (fontMedium != null) return; // Sudah dimuat
        try {
            fontMedium = Typeface.createFromAsset(form.getAssets(), "sfdisplay_medium.TTF");
            fontRegular = Typeface.createFromAsset(form.getAssets(), "sfuitext_regular.otf");
        } catch (Exception e) {
            // Fallback ke default jika font aset tidak ada
            fontMedium = Typeface.DEFAULT_BOLD;
            fontRegular = Typeface.DEFAULT;
        }
    }

    private void drawByTemplate(android.graphics.Canvas canvas, String addr, String lat, String lon,
                            String time, int w, int h) {
        
        loadFonts();
        float dp = w / 360f; // Skala responsif berdasarkan lebar gambar
        float padding = 12 * dp;
        float mapSize = 65 * dp;
        float spacing = 10 * dp;

        float cardWidth = w * 0.94f;
        float cardHeight = mapSize + (padding * 1.5f);
        float left = (w - cardWidth) / 2f;
        float top = h - cardHeight - (10 * dp);

        // === BACKGROUND CARD ===
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.parseColor("#AA000000")); // Sedikit lebih transparan
        canvas.drawRoundRect(new RectF(left, top, left + cardWidth, top + cardHeight), 8 * dp, 8 * dp, bg);

        // === DRAW STATIC MAP ===
        RectF mapRect = new RectF(left + padding, top + padding, left + padding + mapSize, top + padding + mapSize);
        drawMap(canvas, mapRect, lat, lon, dp);

        // === TEXT LOGIC ===
        float textX = mapRect.right + spacing;
        float maxWidth = (left + cardWidth) - textX - padding;

        // Title (Wilayah)
        TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(13 * dp);
        titlePaint.setTypeface(fontMedium);

        String fullWilayah = getWilayahIndonesia(addr);
        StaticLayout titleLayout = StaticLayout.Builder.obtain(fullWilayah, 0, fullWilayah.length(), titlePaint, (int)maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build();

        canvas.save();
        canvas.translate(textX, top + padding);
        titleLayout.draw(canvas);
        canvas.restore();

        // Body Text
        TextPaint bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.WHITE);
        bodyPaint.setTextSize(9 * dp);
        bodyPaint.setTypeface(fontRegular);

        float currentY = top + padding + titleLayout.getHeight() + (4 * dp);
        
        // Alamat Detail (Baris 1 & 2)
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
            // Jika gagal load map, biarkan kosong atau beri warna abu-abu
            Paint placeholder = new Paint();
            placeholder.setColor(Color.DKGRAY);
            canvas.drawRoundRect(rect, 8 * dp, 8 * dp, placeholder);
        }
    }

    private String getWilayahIndonesia(String addr) {
        if (addr == null || !addr.contains(",")) return "Lokasi Indonesia 🇮🇩";
