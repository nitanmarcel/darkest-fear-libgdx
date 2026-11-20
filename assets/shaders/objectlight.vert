// Decodes each sprite's light sample point and class from its vertex colour.
attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
uniform mat4 u_projTrans;
uniform vec2 u_screen;
uniform vec2 u_lightShift;
varying vec2 v_texCoords;
varying vec2 v_luv;
varying vec2 v_slide;
varying vec2 v_lim;
varying vec2 v_puv;
varying float v_cls;
varying float v_alpha;

void main() {
    vec3 c = floor(a_color.rgb * 255.0 + 0.5);
    v_cls = floor(c.b / 32.0);
    float low = c.b - v_cls * 32.0;
    float yHigh = floor(low / 4.0);
    v_luv = (vec2(c.r, c.g + 256.0 * yHigh) + u_lightShift) / u_screen;

    float along = low - yHigh * 4.0;
    v_slide = vec2(along < 1.5 ? 1.0 : 0.0, along > 2.5 || along < 0.5 ? 1.0 : 0.0);

    v_lim = 7.5 / u_screen;
    v_puv = (a_position.xy + u_lightShift) / u_screen;
    v_alpha = a_color.a * (255.0 / 254.0);
    v_texCoords = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}
