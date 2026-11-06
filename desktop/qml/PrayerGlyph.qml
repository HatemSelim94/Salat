import QtQuick

// Drawn glyph per prayer: crescent, sunrise, high sun, low sun, setting sun, moon + star.
Canvas {
    id: c
    property string prayer: "dhuhr"
    property color color: Theme.fg
    width: 20; height: 20
    onPrayerChanged: requestPaint()
    onColorChanged: requestPaint()
    onPaint: {
        const ctx = getContext("2d"); const w = width, h = height;
        ctx.reset(); ctx.clearRect(0, 0, w, h);
        ctx.fillStyle = color; ctx.strokeStyle = color; ctx.lineWidth = w * 0.09; ctx.lineCap = "round";
        const cx = w / 2, cy = h / 2;
        function line(x1, y1, x2, y2) { ctx.beginPath(); ctx.moveTo(x1, y1); ctx.lineTo(x2, y2); ctx.stroke(); }
        function disc(x, y, r) { ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.fill(); }
        function half(x, y, r) { ctx.beginPath(); ctx.arc(x, y, r, Math.PI, Math.PI * 2); ctx.closePath(); ctx.fill(); }
        function rays(x, y, r1, r2, onlyTop) {
            const angles = onlyTop ? [200, 240, 270, 300, 340] : [0, 45, 90, 135, 180, 225, 270, 315];
            for (const a of angles) { const dx = Math.cos(a * Math.PI / 180), dy = Math.sin(a * Math.PI / 180); line(x + dx * r1, y + dy * r1, x + dx * r2, y + dy * r2); }
        }
        function crescent(x, y, r) {
            const d = Math.PI / 180;
            ctx.beginPath();
            ctx.arc(x, y, r, -53.7 * d, (-53.7 - 252.6) * d, true);
            ctx.arc(x + r * 0.45, y, r * 0.757, 82.4 * d, (82.4 + 195.2) * d, false);
            ctx.closePath(); ctx.fill();
        }
        switch (prayer) {
        case "fajr": crescent(cx - w * 0.04, cy - h * 0.04, w * 0.3); ctx.globalAlpha = 0.6; line(w * 0.15, h * 0.88, w * 0.85, h * 0.88); ctx.globalAlpha = 1; break;
        case "sunrise": line(w * 0.1, h * 0.68, w * 0.9, h * 0.68); half(cx, h * 0.68, w * 0.24); rays(cx, h * 0.68, w * 0.34, w * 0.44, true); break;
        case "dhuhr": disc(cx, cy, w * 0.2); rays(cx, cy, w * 0.3, w * 0.42, false); break;
        case "asr": disc(cx + w * 0.05, cy + h * 0.08, w * 0.17); rays(cx + w * 0.05, cy + h * 0.08, w * 0.25, w * 0.34, false); break;
        case "maghrib":
            line(w * 0.1, h * 0.62, w * 0.9, h * 0.62);
            ctx.save(); ctx.beginPath(); ctx.rect(0, 0, w, h * 0.62); ctx.clip(); disc(cx, h * 0.62 + w * 0.06, w * 0.2); ctx.restore();
            line(cx - w * 0.14, h * 0.8, cx + w * 0.14, h * 0.8); break;
        default: crescent(cx - w * 0.08, cy + h * 0.06, w * 0.3); disc(cx + w * 0.3, cy - h * 0.3, w * 0.05); break;
        }
    }
}
