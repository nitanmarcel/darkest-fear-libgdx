// One lamp added to the direction buffer: which way its light arrives at each pixel, weighted by how much arrives.
#ifdef GL_ES
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
#endif

uniform vec2 u_lightPx;
uniform float u_radiusPx;
uniform float u_tile;
uniform vec2 u_dir;
uniform float u_cone;
uniform float u_intensity;
uniform float u_scale;

void main() {
    vec2 toL = u_lightPx - gl_FragCoord.xy * u_scale;
    float cone = 1.0;
    if (dot(u_dir, u_dir) > 0.0) {
        vec2 rel = -toL / u_tile;
        float along = dot(rel, u_dir), across = dot(rel, vec2(u_dir.y, -u_dir.x));
        vec2 half_ = max(vec2(along - 0.5, along + 0.5) / u_cone, 0.0);
        vec2 cover = clamp((min(vec2(0.75), half_ - across) - max(vec2(-0.75), -half_ - across)) / 1.5, 0.0, 1.0);
        cone = smoothstep(0.2, 0.8, 0.5 * (cover.x + cover.y));
    }
    vec2 a = abs(toL) / u_tile;
    float d = pow(pow(a.x, 2.5) + pow(a.y, 2.5), 0.4);
    float r = u_radiusPx / u_tile;
    float atten = clamp((r - d) * (1.0 - smoothstep(r - 1.0, r - 0.5, d)), 0.0, 7.0) / 7.0;
    vec2 arrive = toL / sqrt(dot(toL, toL) + u_tile * u_tile) * (2.0 * u_intensity * atten * cone);
    gl_FragColor = vec4(max(arrive.x, 0.0), max(-arrive.x, 0.0), max(arrive.y, 0.0), max(-arrive.y, 0.0));
}
