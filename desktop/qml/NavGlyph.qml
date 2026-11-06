import QtQuick

// Bottom-row icons: calendar, compass, gear — drawn, no assets.
Canvas {
    property string icon: "calendar"
    property color color: Theme.muted
    width: 18; height: 18
    onColorChanged: requestPaint()
    onPaint: {
        const ctx = getContext("2d"); const w = width, h = height;
        ctx.reset(); ctx.clearRect(0, 0, w, h);
        ctx.strokeStyle = color; ctx.fillStyle = color; ctx.lineWidth = w * 0.09; ctx.lineCap = "round";
        function line(x1, y1, x2, y2) { ctx.beginPath(); ctx.moveTo(x1, y1); ctx.lineTo(x2, y2); ctx.stroke(); }
        if (icon === "calendar") {
            const l = w * 0.12, t = h * 0.18, r = w * 0.88, b = h * 0.9, rad = w * 0.1;
            ctx.beginPath(); ctx.moveTo(l + rad, t); ctx.lineTo(r - rad, t); ctx.arcTo(r, t, r, t + rad, rad); ctx.lineTo(r, b - rad); ctx.arcTo(r, b, r - rad, b, rad);
            ctx.lineTo(l + rad, b); ctx.arcTo(l, b, l, b - rad, rad); ctx.lineTo(l, t + rad); ctx.arcTo(l, t, l + rad, t, rad); ctx.stroke();
            line(l, h * 0.38, r, h * 0.38);
            for (let i = 0; i < 3; i++) for (let j = 0; j < 2; j++) { ctx.beginPath(); ctx.arc(l + w * 0.2 + i * w * 0.18, h * 0.55 + j * h * 0.18, w * 0.045, 0, Math.PI * 2); ctx.fill(); }
            line(w * 0.32, h * 0.08, w * 0.32, h * 0.26); line(w * 0.68, h * 0.08, w * 0.68, h * 0.26);
        } else if (icon === "compass") {
            ctx.beginPath(); ctx.arc(w / 2, h / 2, w * 0.4, 0, Math.PI * 2); ctx.stroke();
            ctx.beginPath(); ctx.moveTo(w * 0.62, h * 0.38); ctx.lineTo(w * 0.44, h * 0.44); ctx.lineTo(w * 0.38, h * 0.62); ctx.lineTo(w * 0.56, h * 0.56); ctx.closePath(); ctx.fill();
        } else {
            ctx.beginPath(); ctx.arc(w / 2, h / 2, w * 0.14, 0, Math.PI * 2); ctx.stroke();
            ctx.beginPath(); ctx.arc(w / 2, h / 2, w * 0.26, 0, Math.PI * 2); ctx.stroke();
            for (let i = 0; i < 8; i++) { const a = i * Math.PI / 4, dx = Math.cos(a), dy = Math.sin(a); line(w / 2 + dx * w * 0.26, h / 2 + dy * h * 0.26, w / 2 + dx * w * 0.4, h / 2 + dy * h * 0.4); }
        }
    }
}
