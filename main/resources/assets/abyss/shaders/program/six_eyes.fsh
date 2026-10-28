#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);

    // Calculate perceived brightness.
    float brightness = dot(
        original.rgb,
        vec3(0.2126, 0.7152, 0.0722)
    );

    // Preserve some fine detail from the original texture.
    float contrast = smoothstep(0.03, 0.97, brightness);

    // Thermal-inspired blue palette.
    vec3 darkBlue = vec3(0.012, 0.025, 0.16);
    vec3 mediumBlue = vec3(0.025, 0.18, 0.52);
    vec3 brightBlue = vec3(0.08, 0.58, 0.88);
    vec3 paleCyan = vec3(0.40, 0.86, 1.0);

    vec3 color;

    if (contrast < 0.35) {
        color = mix(
            darkBlue,
            mediumBlue,
            contrast / 0.35
        );
    } else if (contrast < 0.75) {
        color = mix(
            mediumBlue,
            brightBlue,
            (contrast - 0.35) / 0.40
        );
    } else {
        color = mix(
            brightBlue,
            paleCyan,
            (contrast - 0.75) / 0.25
        );
    }

    // Retain subtle color variation for terrain readability.
    float originalDetail =
        original.r - original.b;

    color += vec3(
        originalDetail * 0.015,
        originalDetail * 0.025,
        0.0
    );

    fragColor = vec4(
        clamp(color, 0.0, 1.0),
        original.a
    );
}