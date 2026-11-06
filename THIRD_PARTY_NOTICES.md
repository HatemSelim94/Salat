# Third-party notices

Salat's own code is proprietary (see LICENSE). It includes or links the following components under their own licences, which continue to apply to those parts.

## adhan (batoulapps) — MIT
- `core/` depends on the npm package `adhan` (adhan-js).
- `desktop/src/adhan/` and `android/app/src/main/java/.../core/Adhan.kt` are ports of adhan-js's algorithm.

```
MIT License

Copyright (c) 2020 Batoul Apps

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Qt 6 — LGPL v3
The desktop app links the Qt libraries dynamically (they ship as separate shared libraries next to the executable, in the AppImage, the macOS bundle and the Windows zip). Under the LGPL you may replace those Qt libraries with your own builds. Qt source: https://code.qt.io — licence text: https://www.gnu.org/licenses/lgpl-3.0.html

## AndroidX / Jetpack Compose — Apache 2.0
Standard Android libraries, bundled in the APK. https://www.apache.org/licenses/LICENSE-2.0

The icon set and the chime are original works by the copyright holder.
