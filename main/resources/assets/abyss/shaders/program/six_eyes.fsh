
#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

float noise(vec2 co) {
    return fract(
        sin(dot(co, vec2(12.9898, 78.233)))
        * 43758.5453
    );
}

void main() {
    vec4 original = texture(
        DiffuseSampler,
        texCoord
    );

    float lum = dot(
        original.rgb,
        vec3(0.30, 0.59, 0.11)
    );

    // Vision Goggles-style cold background.
// Slightly brighter cyan-blue thermal background.
// Inspired by #8AFFFF, but much darker.
vec3 coldBackground = vec3(
    lum * 0.12,
    lum * 0.35,
    lum * 0.48
);

vec3 thermalColor = coldBackground * 1.6;

// Minimum blue/cyan brightness.
thermalColor = max(
    thermalColor,
    vec3(0.025, 0.075, 0.12)
);

    // Subtle thermal grain.
    float grain =
        (noise(texCoord * 1024.0) - 0.5)
        * 0.025;

    thermalColor += vec3(grain);

    fragColor = vec4(
        clamp(thermalColor, 0.0, 1.0),
        1.0
    );
}
