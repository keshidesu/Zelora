package com.zelora.app.ui;

import java.util.ArrayList;
import java.util.List;

// ui
public class CardRow {
    public static class Action {
        public final String label;
        public final boolean primary;
        public final Runnable run;

        Action(String label, boolean primary, Runnable run) {
            this.label = label;
            this.primary = primary;
            this.run = run;
        }
    }

    public String title, subtitle, body, badge, value;
    public final List<Action> actions = new ArrayList<>();

    public CardRow(String title) {
        this.title = title;
    }

    public CardRow subtitle(String s) { subtitle = s; return this; }
    public CardRow body(String s)     { body = s; return this; }
    public CardRow badge(String s)    { badge = s; return this; }
    public CardRow value(String s)    { value = s; return this; }

    public CardRow action(String label, boolean primary, Runnable run) {
        actions.add(new Action(label, primary, run));
        return this;
    }
}
