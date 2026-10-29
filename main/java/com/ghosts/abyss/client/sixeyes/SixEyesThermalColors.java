
package com.ghosts.abyss.client.sixeyes;

public final class SixEyesThermalColors {

    private SixEyesThermalColors() {}

    public static float[] getColor(
            SixEyesEntityColors.ThermalType type,
            float heat
    ) {
        switch (type) {

            case NO_CURSED_ENERGY:
                return new float[]{0.015f, 0.015f, 0.025f, 1.0f};

            case REVERSE_CURSED_TECHNIQUE:
                return new float[]{1.0f, 0.06f, 0.10f, 1.0f};

            case NORMAL:
            default:
                float h = Math.max(0.0f, Math.min(1.0f, heat));

                // Green -> yellow -> orange
                if (h < 0.5f) {
                    float t = h * 2.0f;
                    return new float[]{
                            t,
                            0.9f,
                            0.08f,
                            1.0f
                    };
                }

                float t = (h - 0.5f) * 2.0f;

                return new float[]{
                        1.0f,
                        0.9f - t * 0.55f,
                        0.08f,
                        1.0f
                };
        }
    }
}
