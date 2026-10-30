
#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

// Small amount of thermal-camera grain.
// No scanlines or vignette.
float thermalNoise(vec2 position) {
    return fract(
        sin(dot(
            position,
            vec2(12.9898, 78.233)
        )) * 43758.5453
    );
}

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);

    vec3 color = original.rgb;

    // Same luminance weights used by
    // Vision Goggles' thermal shader.
    float luminance = dot(
        color,
        vec3(0.30, 0.59, 0.11)
    );

    // Dark blue thermal background.
    // Adapted from Vision Goggles.
    vec3 coldBackground = vec3(
        0.0,
        luminance * 0.15,
        luminance * 0.45
    );

    // Vision Goggles' thermal brightness.
    vec3 thermalColor = coldBackground * 1.6;

    // Keep a minimum amount of blue visible,
    // including in dark areas.
    thermalColor = max(
        thermalColor,
        vec3(0.0, 0.05, 0.15)
    );

    // Subtle noise, without scanlines.
    float grain = (
        thermalNoise(texCoord * 1024.0) - 0.5
    ) * 0.025;

    thermalColor += vec3(grain);

    thermalColor = clamp(
        thermalColor,
        0.0,
        1.0
    );

    fragColor = vec4(
        thermalColor,
        original.a
    );
}
