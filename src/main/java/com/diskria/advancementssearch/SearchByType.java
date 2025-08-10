package com.diskria.advancementssearch;

import java.util.Locale;

public enum SearchByType {

    EVERYWHERE,
    TITLE,
    DESCRIPTION,
    ICON;

    private static final String QUERY_SEPARATOR = ":";

    public static SearchByType map(String suggestion) {
        for (SearchByType searchByType : SearchByType.values()) {
            if (searchByType.name().equalsIgnoreCase(suggestion)) {
                return searchByType;
            }
        }
        return EVERYWHERE;
    }

    public static SearchByType findByMask(String query) {
        for (SearchByType searchByType : SearchByType.values()) {
            if (searchByType != EVERYWHERE) {
                String prefix = searchByType.name() + QUERY_SEPARATOR;
                if (query.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                    return searchByType;
                }
            }
        }
        return EVERYWHERE;
    }

    public static String getQueryWithoutMask(String query) {
        SearchByType searchByType = findByMask(query);
        if (searchByType == EVERYWHERE) {
            return query;
        }
        String prefix = searchByType.name() + QUERY_SEPARATOR;
        return query.substring(prefix.length());
    }

    public static String addMaskToQuery(String query, SearchByType searchByType) {
        if (searchByType == EVERYWHERE) {
            return query;
        }
        return searchByType.name().toLowerCase(Locale.ROOT) + QUERY_SEPARATOR + query;
    }
}
