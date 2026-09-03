package com.sosehl.curtis.platform.realtime.domain;

public enum LiveEventType {
    SESSIONS_CHANGED("sessions-changed"),
    RESULTS_CHANGED("results-changed"),
    ROSTER_CHANGED("roster-changed"),
    QUIZZES_CHANGED("quizzes-changed");

    private final String wireName;

    LiveEventType(String wireName) { this.wireName = wireName; }
    public String wireName() { return wireName; }
}
