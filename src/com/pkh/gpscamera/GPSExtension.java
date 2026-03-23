private void drawByTemplate(android.graphics.Canvas canvas, String addr, String lat, String lon,
                            String time, int w, int h) {

    // 1. PENYESUAIAN SKALA BERDASARKAN XML (7sp & 10sp)
    float density = canvas.getDensity() / 160f;
    if (density <= 0) density = 1.0f; 

    float fontSizeTitle = 10 * density; // Sesuai advance_template.xml
    float fontSizeDetail = 7 * density;  // Sesuai advance_template.xml
    float padding = 8 * density;

    float cardWidth = w * 0.94f;
    float cardHeight = (h > w) ? h * 0.24f : h * 0.32f; 
    float left = (w - cardWidth) / 2f;
    float top = h - cardHeight - (h * 0.04f);

    // 2. BACKGROUND HITAM TRANSPARAN (CC = 80% Opacity)
    Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    bg.setColor(Color.BLACK);
    bg.setAlpha(204); 
    canvas.drawRoundRect(new RectF(left, top, left + cardWidth, top + cardHeight), 20, 20, bg);

    // 3. PETA SATELIT (SISI KIRI)
    float mapSize = cardHeight * 0.85f;
    float mapLeft = left + padding;
    float mapTop = top + (cardHeight - mapSize) / 2f;

    try {
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

    // 4. HEADER "GPS MAP CAMERA" (RATA KANAN SEMPURNA)
    String headerTxt = "📷 GPS Map Camera ✏️";
    Paint hTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    hTextPaint.setColor(Color.WHITE);
    hTextPaint.setTextSize(fontSizeDetail);
    hTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

    float hWidth = hTextPaint.measureText(headerTxt) + (padding * 2);
    float hHeight = fontSizeTitle * 1.6f;
    float hLeft = (left + cardWidth) - hWidth - 10; 
    float hTop = top - (hHeight * 0.65f);

    Paint hBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    hBg.setColor(Color.parseColor("#333333"));
    hBg.setAlpha(255); // Kotak kecil lebih pekat agar terlihat terpisah
    canvas.drawRoundRect(new RectF(hLeft, hTop, hLeft + hWidth, hTop + hHeight), 12, 12, hBg);
    canvas.drawText(headerTxt, hLeft + padding, hTop + (hHeight * 0.72f), hTextPaint);

    // 5. AREA DATA (JUDUL DIAMBIL DARI KECAMATAN & KABUPATEN)
    float textLeft = mapLeft + mapSize + (padding * 1.2f);
    float textWidth = (left + cardWidth) - textLeft - padding;

    // Logika Judul: Jika variabel addr mengandung koma, kita ambil bagian spesifiknya
    String[] parts = addr.split(",");
    String kec = "Kecamatan";
    String kab = "Kabupaten";

    // Biasanya dalam Geocoder: [0]Jalan, [1]Desa, [2]Kecamatan, [3]Kabupaten
    if (parts.length >= 4) {
        kec = parts[2].trim();
        kab = parts[3].trim();
    } else if (parts.length >= 2) {
        kec = parts[0].trim();
        kab = parts[1].trim();
    }
    
    String judulFix = kec + ", " + kab + ", Indonesia 🇮🇩";

    TextPaint tpTitle = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    tpTitle.setColor(Color.WHITE);
    tpTitle.setTextSize(fontSizeTitle);
    tpTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

    TextPaint tpDetail = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    tpDetail.setColor(Color.WHITE);
    tpDetail.setTextSize(fontSizeDetail);

    canvas.save();
    // Translate ke posisi awal teks (atas)
    canvas.translate(textLeft, top + padding + (fontSizeTitle * 0.7f));

    // Gambar Judul (Kecamatan, Kabupaten)
    StaticLayout slTitle = new StaticLayout(judulFix, tpTitle, (int) textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
    slTitle.draw(canvas);
    
    // Geser ke bawah berdasarkan tinggi judul
    canvas.translate(0, slTitle.getHeight() + (1 * density));

    // Gambar Alamat Lengkap
    StaticLayout slAddr = new StaticLayout(addr, tpDetail, (int) textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
    slAddr.draw(canvas);
    
    // Geser lagi untuk koordinat
    canvas.translate(0, slAddr.getHeight() + (4 * density));

    // Gambar Lat Long
    canvas.drawText("Lat " + lat + "° Long " + lon + "°", 0, 0, tpDetail);
    
    // Gambar Waktu
    canvas.drawText(formatTanggalIndonesia(time), 0, fontSizeDetail * 1.5f, tpDetail);

    canvas.restore();
}
