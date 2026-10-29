
#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);
    vec3 color = original.rgb;

    // Preserve bright yellow/orange outlines.
    bool yellowOrange =
        color.r > 0.65 &&
        color.g > 0.35 &&
        color.b < 0.35 &&
        color.r > color.b * 2.0;

    // Preserve bright red outlines.
    bool red =
        color.r > 0.65 &&
        color.g < 0.35 &&
        color.b < 0.40;

    if (yellowOrange || red) {
        fragColor = original;
        return;
    }

    // Everything else receives the Six Eyes blue filter.
    float brightness = dot(
        color,
        vec3(0.2126, 0.7152, 0.0722)
    );

    fragColor = vec4(
        brightness * 0.08,
        brightness * 0.35,
        brightness,
        original.a
    );
}
