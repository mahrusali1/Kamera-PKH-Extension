package com.pkh.gpscamera;

import android.graphics.*;
import android.graphics.BitmapFactory; // TAMBAHKAN INI
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
        description = "GPS Map Camera PKH - FINAL STABLE",
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

    // ... (Fungsi GenerateWatermark tetap sama) ...

    private void drawByTemplate(android.graphics.Canvas canvas, String addr, String lat, String lon,
                                String time, int w, int h) {

        String logoName = "logo_pkh.png"; 
        String pinName = "map_pin.png";   

        float padding = w * 0.035f;
        float cardWidth = w * 0.94f;
        float cardHeight = (h > w) ? h * 0.24f : h * 0.34f;

        float left = (w - cardWidth) / 2f;
        float top = h - cardHeight - (h * 0.03f);

        // 1. BACKGROUND KARTU
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.BLACK);
        bg.setAlpha(190); 
        canvas.drawRoundRect(new RectF(left, top, left + cardWidth, top + cardHeight), 25, 25, bg);

        // 2. PETA (Yandex Satelit)
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

        // 3. LOGO (Pojok Kanan Atas)
        try {
            Bitmap logoBmp = BitmapFactory.decodeStream(form.openAsset(logoName));
            if (logoBmp != null) {
                float logoH = cardHeight * 0.22f;
                float logoW = logoH * ((float) logoBmp.getWidth() / logoBmp.getHeight());
                canvas.drawBitmap(logoBmp, null, new RectF((left + cardWidth) - logoW - padding, top + padding, (left + cardWidth) - padding, top + padding + logoH), new Paint(Paint.ANTI_ALIAS_FLAG));
            }
        } catch (Exception ignored) {}

        // 4. AREA TEKS
        float textLeft = (left + padding) + mapSize + (padding * 0.8f);
        float textWidth = cardWidth - mapSize - (padding * 3f);
        float currentY = top + (padding * 1.1f);

        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(Color.WHITE);
        tp.setTextSize(w / 42f);
        tp.setAlpha(160);
        canvas.drawText("GPS Map Camera", textLeft, currentY, tp);

        currentY += padding * 1.3f;
        tp.setAlpha(255);
        tp.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        tp.setTextSize(w / 24f);
        canvas.drawText(extractMainLocation(addr), textLeft, currentY, tp);

        currentY += padding * 0.8f;
        tp.setTypeface(Typeface.DEFAULT);
        tp.setTextSize(w / 38f);
        StaticLayout sl = new StaticLayout(addr, tp, (int) textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
        canvas.save(); canvas.translate(textLeft, currentY); sl.draw(canvas); canvas.restore();

        currentY += sl.getHeight() + (padding * 0.6f);

        // 5. PIN & KOORDINAT (Aksen Teal #009688)
        float iconSize = w / 32f;
        try {
            Bitmap pinBmp = BitmapFactory.decodeStream(form.openAsset(pinName));
            if (pinBmp != null) {
                canvas.drawBitmap(pinBmp, null, new RectF(textLeft, currentY - iconSize, textLeft + iconSize, currentY), new Paint(Paint.ANTI_ALIAS_FLAG));
            }
        } catch (Exception ignored) {}

        tp.setColor(Color.parseColor("#009688"));
        tp.setTextSize(w / 36f);
        canvas.drawText("Lat " + lat + "° Long " + lon + "°", textLeft + iconSize + (padding * 0.3f), currentY, tp);

        // 6. WAKTU
        currentY += padding * 1.1f;
        tp.setColor(Color.WHITE);
        tp.setTextSize(w / 38f);
        canvas.drawText(formatTanggalIndonesia(time), textLeft, currentY, tp);
    }

    // ... (Fungsi formatTanggalIndonesia, fetchAddress, dll tetap sama) ...
}
