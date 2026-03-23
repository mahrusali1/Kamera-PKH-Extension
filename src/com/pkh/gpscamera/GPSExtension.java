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
        String logoName = "logo_pkh.png"; 
        String pinName = "map_pin.png";   

        float padding = w * 0.035f;
        float cardWidth = w * 0.94f;
        float cardHeight = (h > w) ? h * 0.24f : h * 0.34f;
        float left = (w - cardWidth) / 2f;
        float top = h - cardHeight - (h * 0.03f);

        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.BLACK);
        bg.setAlpha(190); 
        canvas.drawRoundRect(new RectF(left, top, left + cardWidth, top + cardHeight), 25, 25, bg);

        float mapSize = cardHeight - (padding * 2);
        try {
            URL url = new URL("https://static-maps.yandex.ru/1.x/?ll=" + lon + "," + lat + "&z=17&l=sat&size=300,300");
            Bitmap map = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            if (map != null) {
                RectF mapRect = new RectF(left + padding, top + padding, left + padding + mapSize, top + padding + mapSize);
                Path path = new Path();
                path.addRoundRect(mapRect, 15, 15, Path.Direction.CW);
                canvas.save();
                canvas.clipPath(path);
                canvas.drawBitmap(map, null, mapRect, new Paint(Paint.ANTI_ALIAS_FLAG));
                canvas.restore();
            }
        } catch (Exception ignored) {}

        float textLeft = left + mapSize + (padding * 1.8f);
        float textWidth = cardWidth - mapSize - (padding * 3f);
        
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(Color.WHITE);
        tp.setTextSize(w / 42f);
        tp.setAlpha(160);
        canvas.drawText("GPS Map Camera", textLeft, top + padding * 1.5f, tp);

        tp.setAlpha(255);
        tp.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        tp.setTextSize(w / 24f);
        canvas.drawText(extractMainLocation(addr), textLeft, top + padding * 3.0f, tp);

        tp.setTypeface(Typeface.DEFAULT);
        tp.setTextSize(w / 38f);
        StaticLayout sl = new StaticLayout(addr, tp, (int) textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
        canvas.save();
        canvas.translate(textLeft, top + padding * 3.8f);
        sl.draw(canvas);
        canvas.restore();

        float coordY = top + padding * 4.5f + sl.getHeight();
        
        // Ikon Pin
        try {
            Bitmap pinBmp = BitmapFactory.decodeStream(form.openAsset(pinName));
            if (pinBmp != null) {
                float pinSize = w / 32f;
                canvas.drawBitmap(pinBmp, null, new RectF(textLeft, coordY - pinSize, textLeft + pinSize, coordY), new Paint(Paint.ANTI_ALIAS_FLAG));
                textLeft += pinSize + (padding * 0.3f);
            }
        } catch (Exception ignored) {}

        tp.setColor(Color.parseColor("#009688"));
        tp.setTextSize(w / 36f);
        canvas.drawText("Lat " + lat + " | Long " + lon, textLeft, coordY, tp);
        
        tp.setColor(Color.WHITE);
        canvas.drawText(formatTanggalIndonesia(time), textLeft, coordY + padding * 1.2f, tp);
    }

    private String formatTanggalIndonesia(String input) {
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
            java.util.Date date = inputFormat.parse(input
