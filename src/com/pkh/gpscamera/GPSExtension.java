private void drawByTemplate(android.graphics.Canvas canvas, String addr, String lat, String lon,
                                String time, int w, int h) {

        // 1. PENYESUAIAN SKALA (Menggunakan standar 7sp & 10sp dari xml)
        float density = canvas.getDensity() / 160f;
        if (density <= 0) density = 1.0f; 

        float fontSizeTitle = 10 * density; 
        float fontSizeDetail = 7 * density;
        float padding = 8 * density;

        float cardWidth = w * 0.94f;
        float cardHeight = (h > w) ? h * 0.24f : h * 0.32f; 
        float left = (w - cardWidth) / 2f;
        float top = h - cardHeight - (h * 0.04f);

        // 2. BACKGROUND HITAM TRANSPARAN (80% Opacity)
        android.graphics.Paint bg = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        bg.setColor(android.graphics.Color.BLACK);
        bg.setAlpha(204); 
        canvas.drawRoundRect(new android.graphics.RectF(left, top, left + cardWidth, top + cardHeight), 20, 20, bg);

        // 3. PETA SATELIT (SISI KIRI)
        float mapSize = cardHeight * 0.85f;
        float mapLeft = left + padding;
        float mapTop = top + (cardHeight - mapSize) / 2f;

        try {
            String mapUrl = "https://static-maps.yandex.ru/1.x/?ll=" + lon + "," + lat + "&z=17&l=sat&size=400,400";
            java.net.URL url = new java.net.URL(mapUrl);
            android.graphics.Bitmap map = android.graphics.BitmapFactory.decodeStream(url.openConnection().getInputStream());
            if (map != null) {
                android.graphics.RectF mapRect = new android.graphics.RectF(mapLeft, mapTop, mapLeft + mapSize, mapTop + mapSize);
                android.graphics.Path path = new android.graphics.Path();
                path.addRoundRect(mapRect, 12, 12, android.graphics.Path.Direction.CW);
                canvas.save();
                canvas.clipPath(path);
                canvas.drawBitmap(map, null, mapRect, null);
                canvas.restore();
            }
        } catch (Exception ignored) {}

        // 4. HEADER "GPS MAP CAMERA" (RATA KANAN)
        String headerTxt = "📷 GPS Map Camera ✏️";
        android.graphics.Paint hTextPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        hTextPaint.setColor(android.graphics.Color.WHITE);
        hTextPaint.setTextSize(fontSizeDetail);
        hTextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        float hWidth = hTextPaint.measureText(headerTxt) + (padding * 2);
        float hHeight = fontSizeTitle * 1.6f;
        float hLeft = (left + cardWidth) - hWidth - 10; 
        float hTop = top - (hHeight * 0.65f);

        android.graphics.Paint hBg = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        hBg.setColor(android.graphics.Color.parseColor("#333333"));
        hBg.setAlpha(255);
        canvas.drawRoundRect(new android.graphics.RectF(hLeft, hTop, hLeft + hWidth, hTop + hHeight), 12, 12, hBg);
        canvas.drawText(headerTxt, hLeft + padding, hTop + (hHeight * 0.72f), hTextPaint);

        // 5. AREA TEKS (JUDUL KECAMATAN & KABUPATEN)
        float textLeft = mapLeft + mapSize + (padding * 1.2f);
        float textWidth = (left + cardWidth) - textLeft - padding;

        // Memecah alamat untuk mencari Kecamatan dan Kabupaten secara dinamis
        String[] parts = addr.split(",");
        String kec = "";
        String kab = "";

        if (parts.length >= 4) {
            // Biasanya urutan Geocoder dari belakang: [Negara], [Provinsi], [Kabupaten], [Kecamatan]
            kec = parts[parts.length - 4].trim(); 
            kab = parts[parts.length - 3].trim(); 
        } else if (parts.length >= 2) {
            kec = parts[0].trim();
            kab = parts[1].trim();
        } else {
            kec = addr;
        }
        
        String judulFix = kec + (kab.isEmpty() ? "" : ", " + kab) + ", Indonesia 🇮🇩";

        android.text.TextPaint tpTitle = new android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        tpTitle.setColor(android.graphics.Color.WHITE);
        tpTitle.setTextSize(fontSizeTitle);
        tpTitle.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));

        android.text.Text
