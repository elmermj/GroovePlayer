package com.aethelsoft.grooveplayer.utils.theme.shader

const val FROST_SHADER = """
uniform shader input;
uniform float2 resolution;
uniform float strength;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / resolution;

    // multi-sample blur (soft diffusion, not gaussian)
    half4 c = half4(0.0);
    c += input.eval(fragCoord + float2(-2.0, -2.0));
    c += input.eval(fragCoord + float2( 2.0, -2.0));
    c += input.eval(fragCoord + float2(-2.0,  2.0));
    c += input.eval(fragCoord + float2( 2.0,  2.0));
    c *= 0.25;

    // frost whitening
    c.rgb = mix(c.rgb, half3(1.0), strength);

    return c;
}
"""