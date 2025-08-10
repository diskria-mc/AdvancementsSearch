package com.diskria.advancementssearch;

public enum HighlightType {

    WIDGET,
    OBTAINED_STATUS;

    public static HighlightType map(String suggestion) {
        for (HighlightType searchByType : HighlightType.values()) {
            if (searchByType.name().equalsIgnoreCase(suggestion)) {
                return searchByType;
            }
        }
        return WIDGET;
    }
}
