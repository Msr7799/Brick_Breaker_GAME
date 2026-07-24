#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
void main(){ vec4 c=texture2D(u_texture,v_texCoords)*v_color; float bloom=smoothstep(.55,1.0,max(c.r,max(c.g,c.b))); gl_FragColor=vec4(c.rgb+bloom*.16,c.a); }
