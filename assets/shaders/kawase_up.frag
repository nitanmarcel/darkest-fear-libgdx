// Dual Kawase blur, upsample pass.
// After Marius Bjorge, "Bandwidth-Efficient Rendering", SIGGRAPH 2015.
#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2 u_half;

void main() {
    vec2 uv = v_texCoords;
    vec3 c = texture2D(u_texture, uv + vec2(-u_half.x * 2.0, 0.0)).rgb;
    c += texture2D(u_texture, uv + vec2(-u_half.x, u_half.y)).rgb * 2.0;
    c += texture2D(u_texture, uv + vec2(0.0, u_half.y * 2.0)).rgb;
    c += texture2D(u_texture, uv + vec2(u_half.x, u_half.y)).rgb * 2.0;
    c += texture2D(u_texture, uv + vec2(u_half.x * 2.0, 0.0)).rgb;
    c += texture2D(u_texture, uv + vec2(u_half.x, -u_half.y)).rgb * 2.0;
    c += texture2D(u_texture, uv + vec2(0.0, -u_half.y * 2.0)).rgb;
    c += texture2D(u_texture, uv + vec2(-u_half.x, -u_half.y)).rgb * 2.0;
    gl_FragColor = vec4(c / 12.0, 1.0);
}
