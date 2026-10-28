#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);

    float brightness = dot(
        original.rgb,
        vec3(0.2126, 0.7152, 0.0722)
    );

    fragColor = vec4(
        brightness * 0.08,
        brightness * 0.35,
        brightness * 1.0,
        original.a
    );
}