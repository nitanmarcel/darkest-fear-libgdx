// Dual Kawase blur, downsample pass.
// After Marius Bjorge, "Bandwidth-Efficient Rendering", SIGGRAPH 2015.
#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2 u_half;

void main() {
    vec3 c = texture2D(u_texture, v_texCoords).rgb * 4.0;
    c += texture2D(u_texture, v_texCoords - u_half).rgb;
    c += texture2D(u_texture, v_texCoords + u_half).rgb;
    c += texture2D(u_texture, v_texCoords + vec2(u_half.x, -u_half.y)).rgb;
    c += texture2D(u_texture, v_texCoords - vec2(u_half.x, -u_half.y)).rgb;
    gl_FragColor = vec4(c / 8.0, 1.0);
}
