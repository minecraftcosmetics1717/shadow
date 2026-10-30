
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

bool nearColor(vec3 actual, vec3 expected, float tolerance) {
    return distance(actual, expected) < tolerance;
}

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);
    vec3 color = original.rgb;

    // Cursed-energy outline colors.
    vec3 orange = vec3(1.0, 184.0 / 255.0, 46.0 / 255.0);
    vec3 red = vec3(1.0, 37.0 / 255.0, 58.0 / 255.0);
    vec3 pink = vec3(1.0, 103.0 / 255.0, 188.0 / 255.0);

    // Preserve colors close to our outline palette.
    // A small tolerance reduces accidental matches.
    bool preserveOutline =
        nearColor(color, orange, 0.045) ||
        nearColor(color, red, 0.045) ||
        nearColor(color, pink, 0.045);

    if (preserveOutline) {
        fragColor = original;
        return;
    }

    // Six Eyes thermal environment.
    float lum = dot(
        color,
        vec3(0.30, 0.59, 0.11)
    );

    // Cyan-blue appearance.
    vec3 coldBackground = vec3(
        lum * 0.12,
        lum * 0.35,
        lum * 0.48
    );

    vec3 thermalColor = coldBackground * 1.6;

    thermalColor = max(
        thermalColor,
        vec3(0.025, 0.075, 0.12)
    );

    // Subtle noise, no scanlines or vignette.
    float grain =
        (noise(texCoord * 1024.0) - 0.5)
        * 0.025;

    thermalColor += vec3(grain);

    fragColor = vec4(
        clamp(thermalColor, 0.0, 1.0),
        1.0
    );
}
