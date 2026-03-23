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
                Canvas canvas = new Canvas(bitmap);

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

            } catch (Exception e) {
                form.runOnUiThread(new Runnable() {
    @Override
    public void run() {
        OnError(e.getMessage());
    }
});
            }
        }).start();
    }

    private void drawByTemplate(Canvas canvas, String addr, String lat, String lon,
                                String time, int w, int h) {

        float dp = w / 360f;

        float padding = 12 * dp;
        float mapSize = 60 * dp;
        float spacing = 10 * dp;

        float cardWidth = w * 0.94f;
        float cardHeight = mapSize + (padding * 1.3f);

        float left = (w - cardWidth) / 2f;
        float top = h - cardHeight - (20 * dp);

        Typeface fontMedium = Typeface.DEFAULT_BOLD;
        Typeface fontRegular = Typeface.DEFAULT;

        // BACKGROUND
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.parseColor("#99000000"));
        canvas.drawRoundRect(new RectF(left, top, left + cardWidth, top + cardHeight),
                10 * dp, 10 * dp, bg);

        // MAP
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
                canvas.drawBitmap(map, null, mapRect, null);
            }
        } catch (Exception ignored) {}

        float textX = mapRect.right + spacing;

        // ===== TITLE =====
        TextPaint title = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        title.setColor(Color.WHITE);
        title.setTextSize(14 * dp);
        title.setTypeface(fontMedium);

        float titleY = top + (4 * dp);

        StaticLayout titleLayout;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            titleLayout = StaticLayout.Builder
                    .obtain(getWilayahIndonesia(addr), 0, getWilayahIndonesia(addr).length(),
                            title, (int)(cardWidth - mapSize - 40 * dp))
                    .build();
        } else {
            titleLayout = new StaticLayout(
                    getWilayahIndonesia(addr),
                    title,
                    (int)(cardWidth - mapSize - 40 * dp),
                    Layout.Alignment.ALIGN_NORMAL,
                    1f, 0f, false
            );
        }

        canvas.save();
        canvas.translate(textX, titleY);
        titleLayout.draw(canvas);
        canvas.restore();

        // ===== BODY =====
        TextPaint body = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        body.setColor(Color.WHITE);
        body.setTextSize(9 * dp);
        body.setTypeface(fontRegular);

        float y = titleY + titleLayout.getHeight() + (2 * dp);

        String[] parts = addr.split("\\|");

        String dusun = parts.length > 1 ? parts[1].trim() : "";
        String suburb = parts.length > 2 ? parts[2].trim() : "";
        String kabupaten = parts.length > 3 ? parts[3].trim() : "";
        String provinsi = parts.length > 4 ? parts[4].trim() : "";

        if (provinsi.equals("")) provinsi = "Jawa Timur";

        StringBuilder alamat = new StringBuilder();

        if (!suburb.equals("")) alamat.append(suburb);
        if (!dusun.equals("")) {
            if (alamat.length() > 0) alamat.append(", ");
            alamat.append(dusun);
        }

        if (alamat.length() > 0) alamat.append(", ");
        alamat.append("Kab. ").append(kabupaten).append(", ").append(provinsi);

        StaticLayout bodyLayout;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            bodyLayout = StaticLayout.Builder
                    .obtain(alamat.toString(), 0, alamat.length(), body,
                            (int)(cardWidth - mapSize - 40 * dp))
                    .build();
        } else {
            bodyLayout = new StaticLayout(
                    alamat.toString(),
                    body,
                    (int)(cardWidth - mapSize - 40 * dp),
                    Layout.Alignment.ALIGN_NORMAL,
                    1f, 0f, false
            );
        }

        canvas.save();
        canvas.translate(textX, y);
        bodyLayout.draw(canvas);
        canvas.restore();

        y += bodyLayout.getHeight() + (6 * dp);

        canvas.drawText("Lat " + lat + " | Long " + lon, textX, y, body);

        y += 8 * dp;
        canvas.drawText(formatTanggalIndonesia(time), textX, y, body);
    }

    private String formatTanggalIndonesia(String input) {
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
            java.util.Date date = inputFormat.parse(input);

            String[] hari = {"Minggu","Senin","Selasa","Rabu","Kamis","Jumat","Sabtu"};
            String[] bulan = {"Januari","Februari","Maret","April","Mei","Juni","Juli","Agustus","September","Oktober","November","Desember"};

            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(date);

            return hari[cal.get(java.util.Calendar.DAY_OF_WEEK)-1] + ", "
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

            org.json.JSONObject json = new org.json.JSONObject(result);
            org.json.JSONObject address = json.getJSONObject("address");

            String desa = address.optString("village", "");
            String dusun = address.optString("hamlet", "");
            String suburb = address.optString("suburb", "");

            String kabupaten = address.optString("county",
                    address.optString("city", ""));

            String provinsi = address.optString("state", "");

            if (provinsi.equals("")) provinsi = "Jawa Timur";

            return desa + "|" + dusun + "|" + suburb + "|" + kabupaten + "|" + provinsi;

        } catch (Exception e) {
            return "Lokasi tidak ditemukan";
        }
    }

    private String getWilayahIndonesia(String addr) {
        try {
            String[] parts = addr.split("\\|");

            String desa = parts.length > 0 ? parts[0].trim() : "";
            String kabupaten = parts.length > 3 ? parts[3].trim() : "";
            String provinsi = parts.length > 4 ? parts[4].trim() : "";

            if (provinsi.equals("")) provinsi = "Jawa Timur";

            return desa + ", Kab. " + kabupaten + ", " + provinsi + ", Indonesia 🇮🇩";

        } catch (Exception e) {
            return "Indonesia 🇮🇩";
        }
    }

    @SimpleEvent
    public void OnAddressFound(String address, String path) {
        EventDispatcher.dispatchEvent(this, "OnAddressFound", address, path);
    }

    @SimpleEvent
    public void OnError(String message) {
        EventDispatcher.dispatchEvent(this, "OnError", message);
    }
}
