// Per-pixel lighting for the floor and decal bands.
#ifdef GL_ES
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform sampler2D u_light;
uniform sampler2D u_bounce;
uniform sampler2D u_furniture;
uniform vec2 u_view;
uniform vec2 u_screen;
uniform vec2 u_scroll;
uniform vec2 u_lightShift;
uniform vec2 u_mapTiles;
uniform vec3 u_ambient;
uniform vec3 u_bounceGain;
uniform float u_curveF[8];
uniform float u_debug;
uniform vec4 u_edge;
uniform vec3 u_edgeArea;

float edgeLight(vec2 pos) {
    float d = min(min(mix(1e4, pos.x, u_edge.x), mix(1e4, u_edgeArea.x - pos.x, u_edge.y)),
                  min(mix(1e4, pos.y, u_edge.z), mix(1e4, u_edgeArea.y - pos.y, u_edge.w)));
    return smoothstep(0.0, u_edgeArea.z, d);
}

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

void main() {
    vec4 col = v_color * texture2D(u_texture, v_texCoords);
    vec2 sp = vec2(gl_FragCoord.x, u_view.x - gl_FragCoord.y) / u_view.y;
    vec2 uv = (sp + u_lightShift) / u_screen;
    vec2 mp = sp + u_scroll;
    vec3 L = 2.0 * texture2D(u_light, uv).rgb + texture2D(u_bounce, uv).rgb * u_bounceGain;
    L = lampLight(L) * edgeLight(sp);

    L *= 1.0 - 0.6 * texture2D(u_furniture, mp / (u_mapTiles * 16.0)).r;
    col.rgb = shade(col.rgb, L);
    if (u_debug > 0.5) {
        col.rgb = vec3(0.0, 0.3, 1.0);
    }
    gl_FragColor = col;
}
