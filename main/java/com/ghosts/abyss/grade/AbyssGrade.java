
package com.ghosts.abyss.grade;

import java.util.Locale;

public enum AbyssGrade {
    UNGRADED("Ungraded", 1),
    GRADE_4("Grade 4", 2),
    GRADE_3("Grade 3", 4),
    SEMI_GRADE_2("Semi-Grade 2", 7),
    GRADE_2("Grade 2", 9),
    SEMI_GRADE_1("Semi-Grade 1", 11),
    GRADE_1("Grade 1", 13),
    SPECIAL_GRADE("Special Grade", 20);

    private final String displayName;
    private final int jcLevel;

    AbyssGrade(String displayName, int jcLevel) {
        this.displayName = displayName;
        this.jcLevel = jcLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getJcLevel() {
        return jcLevel;
    }

    public static AbyssGrade fromName(String name) {
        if (name == null) return UNGRADED;

        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return UNGRADED;
        }
    }
}
