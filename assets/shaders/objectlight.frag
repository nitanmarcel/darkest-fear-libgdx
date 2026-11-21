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
uniform float u_dangerBody;
uniform vec3 u_cueColor;
uniform sampler2D u_shape;
uniform vec3 u_shapeLight;

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

float glow(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

bool is(vec3 c, vec3 key) {
    return all(lessThan(abs(c - key), vec3(0.01)));
}

void main() {
    vec4 col = texture2D(u_texture, v_texCoords);
    col.a *= v_alpha;
    if (v_cls > 5.5 && v_cls < 6.5) {
        gl_FragColor = vec4(u_cueColor, col.a);
        return;
    }
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
    bool player = v_cls > 4.5 && v_cls < 5.5;
    if ((player || (v_cls > 2.5 && v_cls < 3.5)) && u_actorPixel > 0.0) {
        vec3 under = 2.0 * texture2D(u_light, v_puv).rgb + texture2D(u_bounce, v_puv).rgb * u_bounceGain;
        L = mix(L, max(L, under), u_actorPixel);
    }
    float rim = 0.0;
    if (player || (v_cls > 2.5 && v_cls < 3.5)) {
        vec4 surface = texture2D(u_shape, v_texCoords);
        vec2 slope = surface.rg * 2.0 - 1.0;
        slope *= step(0.02, length(slope));
        vec2 rise = vec2(
            glow(texture2D(u_bounce, v_puv + vec2(v_lim.x, 0.0)).rgb) - glow(texture2D(u_bounce, v_puv - vec2(v_lim.x, 0.0)).rgb),
            glow(texture2D(u_bounce, v_puv + vec2(0.0, v_lim.y)).rgb) - glow(texture2D(u_bounce, v_puv - vec2(0.0, v_lim.y)).rgb));
        float steep = length(rise);
        float directed = smoothstep(0.004, 0.03, steep);
        vec2 toward = rise / max(steep, 0.0001);
        vec3 lamp = vec3(toward * 0.94, 0.34);
        vec3 normal = vec3(slope, sqrt(max(1.0 - dot(slope, slope), 0.0)));
        float turn = directed * (max(dot(normal, lamp), 0.0) - lamp.z * normal.z);
        L *= 1.0 + 2.0 * u_shapeLight.x * max(floor(turn * 3.0 + 0.5) / 3.0, 0.0);
        vec2 outward = slope / max(length(slope), 0.0001);
        float facing = step(0.5, dot(outward, toward)) * step(outward.y, 0.4);
        rim = step(0.5, directed) * u_shapeLight.y * facing * step(surface.b, u_shapeLight.z) * surface.a;
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

    rgb += L * rim * (0.35 + 0.65 * col.rgb);

    if (player) {
        rgb = max(rgb, structRGB * (u_dangerBody / u_structFloor));
    }

    if (u_debug > 0.5) {
        rgb = v_cls < 1.5 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0);
        if (dot(L, vec3(1.0)) <= 0.0) rgb = vec3(1.0, 0.0, 1.0);
        if (cap) rgb = vec3(0.5);
    }
    gl_FragColor = vec4(rgb, col.a);
}
