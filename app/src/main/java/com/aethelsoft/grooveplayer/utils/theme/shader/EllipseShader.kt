package com.aethelsoft.grooveplayer.utils.theme.shader

const val ELLIPSE_SHADER = """
uniform float2 resolution;
uniform float2 center;
uniform float radiusX;
uniform float radiusY;
uniform float p;
uniform float q;
uniform float scale;

half4 main(float2 fragCoord) {
    float2 pos = fragCoord - center;

    // Left half only
    if (pos.x > 0.0) {
        return half4(0.0);
    }

    float a = radiusX * p * scale;
    float b = radiusY * q * scale;
    
    // Elliptical normalized distance
    float d = sqrt(
        (pos.x * pos.x) / (a * a) +
        (pos.y * pos.y) / (b * b)
    );

    if (d > 1.0) {
        return half4(0.0);
    }

    // Smooth falloff
    float t = smoothstep(1.0, 0.0, d);

    // ───── COLOR STOPS (INNER → OUTER) ─────
    half4 c1 = half4(0.357, 0.639, 0.816, 1); // #5BA3D0 @ 25%
    half4 c2 = half4(0.529, 0.808, 0.922, 0.55); // #87CEEB @ 55%
    half4 c3 = half4(0.357, 0.639, 0.816, 0.45); // #5BA3D0 @ 45%
    half4 c4 = half4(0.180, 0.490, 0.710, 0.30); // #2E7DB5 @ 30%

    half4 color;
    if (t > 0.75) {
        color = mix(c2, c1, (t - 0.75) / 0.25);
    } else if (t > 0.5) {
        color = mix(c3, c2, (t - 0.5) / 0.25);
    } else if (t > 0.25) {
        color = mix(c4, c3, (t - 0.25) / 0.25);
    } else {
        color = mix(half4(0.0), c4, t / 0.25);
    }

    return color;
}
"""