// One lamp drawn additively into the light buffer: game falloff, cone, wall and furniture shadows.
// Soft shadows after Inigo Quilez, "Soft shadows in raymarched SDFs", iquilezles.org/articles/rmshadows.
#ifdef GL_ES
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
#endif

uniform sampler2D u_texture;

uniform vec2 u_scroll;
uniform vec2 u_lightPx;
uniform float u_radiusPx;
uniform float u_tile;
uniform vec3 u_color;
uniform vec2 u_dir;
uniform float u_cone;
uniform float u_intensity;
uniform vec2 u_sdfSize;
uniform float u_k;

vec2 sdfTexel(vec2 uv) {
    vec4 s = texture2D(u_texture, uv);
    return vec2((s.r + s.g / 255.0) * 32.0, s.b * 8.0);
}

vec2 sdf(vec2 p) {
    vec2 uv = (p + u_scroll) / u_sdfSize;
    vec2 f = uv * u_sdfSize - 0.5;
    vec2 i = floor(f);
    vec2 a = f - i;
    vec2 t0 = (i + 0.5) / u_sdfSize, t1 = (i + 1.5) / u_sdfSize;
    return mix(mix(sdfTexel(t0), sdfTexel(vec2(t1.x, t0.y)), a.x),
               mix(sdfTexel(vec2(t0.x, t1.y)), sdfTexel(t1), a.x), a.y);
}

float furnitureShadow(vec2 px, vec2 rd, float dist) {
    if (sdfTexel((px + u_scroll) / u_sdfSize).y < 0.5) {
        return 1.0;
    }
    float occ = 0.0, t = 0.75;
    for (int i = 0; i < 28; i++) {
        if (t >= dist || t > 2.0 * u_tile) break;
        float h = sdf(px + rd * t).y;
        occ = max(occ, (1.0 - smoothstep(0.0, 1.0, h)) * 0.6 * (1.0 - t / (2.0 * u_tile)));
        t += max(h, 1.0);
    }
    return 1.0 - occ;
}

void main() {
    vec2 px = gl_FragCoord.xy;
    vec2 toL = u_lightPx - px;
    float dist = length(toL);
    vec2 rd = toL / max(dist, 0.0001);
    float cone = 1.0;
    if (dot(u_dir, u_dir) > 0.0) {
        vec2 rel = -toL / u_tile;
        float along = dot(rel, u_dir), across = dot(rel, vec2(u_dir.y, -u_dir.x));
        vec2 half_ = max(vec2(along - 0.5, along + 0.5) / u_cone, 0.0);
        vec2 cover = clamp((min(vec2(0.75), half_ - across) - max(vec2(-0.75), -half_ - across)) / 1.5, 0.0, 1.0);
        cone = 0.5 * (cover.x + cover.y);
        cone = smoothstep(0.2, 0.8, cone);
    }

    vec2 a = abs(toL) / u_tile;
    float d = pow(pow(a.x, 2.5) + pow(a.y, 2.5), 0.4);
    float r = u_radiusPx / u_tile;
    float atten = clamp((r - d) * (1.0 - smoothstep(r - 1.0, r - 0.5, d)), 0.0, 7.0) / 7.0;
    float t = 1.0, res = 1.0;
    for (int i = 0; i < 48; i++) {
        if (t >= dist) break;
        float h = sdf(px + rd * t).x;
        if (h < 0.5) { res = 0.0; break; }
        res = min(res, u_k * h / t);
        t += max(h, 0.75);
    }
    res = smoothstep(0.0, 1.0, res) * furnitureShadow(px, rd, dist);
    gl_FragColor = vec4(u_color * (u_intensity * atten * cone * res), 1.0);
}
