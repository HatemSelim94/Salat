import QtQuick

// Fajr → next Fajr line with a dot per obligatory prayer and a marker for now.
Canvas {
    property real progress: 0
    property var dots: []
    property color track: Theme.line
    property color fill: Theme.accent
    height: 14
    onProgressChanged: requestPaint()
    onDotsChanged: requestPaint()
    onTrackChanged: requestPaint()
    onFillChanged: requestPaint()
    onPaint: {
        const ctx = getContext("2d"); const w = width, y = height / 2, pad = 6;
        ctx.reset(); ctx.clearRect(0, 0, width, height);
        ctx.lineCap = "round"; ctx.lineWidth = 3;
        function x(f) { return pad + f * (w - 2 * pad); }
        function disc(px, r, col) { ctx.fillStyle = col; ctx.beginPath(); ctx.arc(px, y, r, 0, Math.PI * 2); ctx.fill(); }
        ctx.strokeStyle = track; ctx.beginPath(); ctx.moveTo(x(0), y); ctx.lineTo(x(1), y); ctx.stroke();
        if (progress > 0) { ctx.strokeStyle = fill; ctx.beginPath(); ctx.moveTo(x(0), y); ctx.lineTo(x(progress), y); ctx.stroke(); }
        for (const d of dots) disc(x(d.frac), 4, d.passed ? fill : track);
        disc(x(progress), 6, fill); disc(x(progress), 2.5, "#ffffff");
    }
}
