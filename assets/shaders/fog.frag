// Darkness that closes in over the edges of the level.
#ifdef GL_ES
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
#endif
uniform vec4 u_scene;
uniform vec4 u_sides;
uniform float u_unit;
uniform float u_time;
uniform float u_amount;

const float REACH = 11.0;
const float RAGGED = 7.0;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    mat2 rot = mat2(0.80, 0.60, -0.60, 0.80);
    for (int i = 0; i < 4; i++) {
        sum += amp * noise(p);
        p = rot * p * 2.0;
        amp *= 0.5;
    }
    return sum;
}

void main() {
    vec2 p = (floor(gl_FragCoord.xy / u_unit) + 0.5) * u_unit;
    vec2 lo = u_scene.xy;
    vec2 hi = u_scene.xy + u_scene.zw;
    float field;
    if (p.x >= lo.x && p.x < hi.x && p.y >= lo.y && p.y < hi.y) {
        float left = mix(1e4, p.x - lo.x, u_sides.x);
        float right = mix(1e4, hi.x - p.x, u_sides.y);
        float top = mix(1e4, hi.y - p.y, u_sides.z);
        float bottom = mix(1e4, p.y - lo.y, u_sides.w);
        field = -min(min(left, right), min(top, bottom)) / u_unit;
        if (field < -(REACH + RAGGED)) {
            discard;
        }
    } else {
        vec2 near = clamp(p, lo, hi);
        vec2 gap = abs(p - near);
        bool open = gap.x > 0.0 ? (p.x < lo.x ? u_sides.x > 0.5 : u_sides.y > 0.5) : (p.y < lo.y ? u_sides.w > 0.5 : u_sides.z > 0.5);
        field = length(gap) / u_unit;
        if (!open || field > RAGGED) {
            discard;
        }
    }

    vec2 q = p / u_unit / 46.0;
    vec2 warp = vec2(fbm(q + vec2(0.0, u_time * 0.040)), fbm(q + vec2(5.2, u_time * 0.031 + 1.7)));
    float cloud = fbm(q + (warp - 0.5) * 1.8 + vec2(u_time * 0.015, 0.0));
    float dark = smoothstep(-REACH, 0.0, field + (cloud - 0.5) * 2.0 * RAGGED);
    gl_FragColor = vec4(0.0, 0.0, 0.0, dark * u_amount);
}
