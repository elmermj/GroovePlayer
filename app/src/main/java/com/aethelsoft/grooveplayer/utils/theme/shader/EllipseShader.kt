package com.aethelsoft.grooveplayer.utils.theme.shader

const val ELLIPSE_SHADER = """
uniform float2 resolution;
uniform float2 center;
uniform float radiusX;
uniform float radiusY;
uniform float p;
uniform float q;
uniform float scale;
uniform float pulse;
uniform float colorMix;
uniform float failMix;

half4 main(float2 fragCoord) {
    float2 pos = fragCoord - center;

    if (pos.x > 0.0) {
        return half4(0.0);
    }

    float s = scale * pulse;
    float a = radiusX * p * s;
    float b = radiusY * q * s;

    float d = sqrt(
        (pos.x * pos.x) / (a * a) +
        (pos.y * pos.y) / (b * b)
    );

    if (d > 1.0) {
        return half4(0.0);
    }

    float t = smoothstep(1.0, 0.0, d);

    half4 bc1 = half4(0.357, 0.639, 0.816, 1);
    half4 bc2 = half4(0.529, 0.808, 0.922, 0.55);
    half4 bc3 = half4(0.357, 0.639, 0.816, 0.45);
    half4 bc4 = half4(0.180, 0.490, 0.710, 0.30);

    half4 gc1 = half4(0.22, 0.75, 0.35, 1);
    half4 gc2 = half4(0.35, 0.85, 0.45, 0.55);
    half4 gc3 = half4(0.22, 0.75, 0.35, 0.45);
    half4 gc4 = half4(0.12, 0.55, 0.28, 0.30);

    half4 rc1 = half4(0.92, 0.22, 0.22, 1);
    half4 rc2 = half4(0.95, 0.35, 0.35, 0.55);
    half4 rc3 = half4(0.90, 0.25, 0.25, 0.45);
    half4 rc4 = half4(0.75, 0.15, 0.15, 0.30);

    half4 c1 = mix(mix(bc1, gc1, colorMix), rc1, failMix);
    half4 c2 = mix(mix(bc2, gc2, colorMix), rc2, failMix);
    half4 c3 = mix(mix(bc3, gc3, colorMix), rc3, failMix);
    half4 c4 = mix(mix(bc4, gc4, colorMix), rc4, failMix);

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