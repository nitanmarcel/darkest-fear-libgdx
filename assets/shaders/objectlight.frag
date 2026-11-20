// Per-sprite lighting for walls, props, furniture and actors.
#ifdef GL_ES
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
#endif

varying vec2 v_texCoords;
varying vec2 v_luv;
varying vec2 v_puv;
varying vec2 v_slide;
varying vec2 v_lim;
varying float v_cls;
varying float v_alpha;
uniform sampler2D u_texture;
uniform sampler2D u_light;
uniform sampler2D u_bounce;
uniform float u_lit;
uniform vec3 u_ambient;
uniform vec3 u_bounceGain;
uniform vec3 u_capColor;
uniform float u_structFloor;
uniform float u_curveF[8];
uniform float u_actorPixel;
uniform float u_wallSoft;
uniform float u_debug;

vec3 shade(vec3 albedo, vec3 L) {
    vec3 col = albedo * (u_ambient + L);
    float lum = dot(L, vec3(0.299, 0.587, 0.114));
    vec3 cold = dot(col, vec3(0.299, 0.587, 0.114)) * vec3(0.55, 0.65, 1.0);
    return mix(col, cold, 1.0 - smoothstep(0.0, 0.3, lum));
}

float levelCurve(float lv) {
    float b = 0.0;
    for (int i = 0; i < 7; i++) {
        b += clamp(lv - float(i), 0.0, 1.0) * (u_curveF[i + 1] - u_curveF[i]);
    }
    return b;
}

vec3 lampLight(vec3 lamp) {
    float pk = max(lamp.r, max(lamp.g, lamp.b));
    vec3 chroma = mix(vec3(1.0), lamp / max(pk, 0.0001), smoothstep(0.0, 0.02, pk));
    return chroma * levelCurve(min(pk * 7.0, 7.0));
}

bool is(vec3 c, vec3 key) {
    return all(lessThan(abs(c - key), vec3(0.01)));
}

void main() {
    vec4 col = texture2D(u_texture, v_texCoords);
    col.a *= v_alpha;
    if (u_lit < 0.5 || v_cls > 6.5) {
        gl_FragColor = col;
        return;
    }
    vec2 luv = v_luv + v_slide * clamp(v_puv - v_luv, -v_lim, v_lim);
    vec3 direct = texture2D(u_light, luv).rgb, soft = texture2D(u_bounce, luv).rgb;
    if (v_cls < 1.5) {
        direct = mix(direct, soft, u_wallSoft);
    }
    vec3 L = 2.0 * direct + soft * u_bounceGain;
    if (v_cls > 2.5 && v_cls < 3.5 && u_actorPixel > 0.0) {
        vec3 under = 2.0 * texture2D(u_light, v_puv).rgb + texture2D(u_bounce, v_puv).rgb * u_bounceGain;
        L = mix(L, max(L, under), u_actorPixel);
    }
    L = lampLight(L);

    bool cap = v_cls < 1.5 && is(col.rgb, u_capColor);
    if (cap) {
        L = vec3(0.0);
    }
    vec3 rgb = shade(col.rgb, L);
    float luma = dot(col.rgb, vec3(0.3, 0.59, 0.11));
    vec3 structRGB = mix(vec3(luma), col.rgb, 0.3) * vec3(0.8, 0.9, 1.1) * u_structFloor;
    if (v_cls < 2.5) {
        rgb = max(rgb, structRGB);
    } else if (is(col.rgb, vec3(1.0, 0.988, 0.961)) || is(col.rgb, vec3(1.0, 0.824, 0.0))
            || is(col.rgb, vec3(1.0, 0.643, 0.0)) || is(col.rgb, vec3(0.890, 0.353, 0.004))) {
        rgb = col.rgb;
    }

    if (u_debug > 0.5) {
        rgb = v_cls < 1.5 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0);
        if (dot(L, vec3(1.0)) <= 0.0) rgb = vec3(1.0, 0.0, 1.0);
        if (cap) rgb = vec3(0.5);
    }
    gl_FragColor = vec4(rgb, col.a);
}
