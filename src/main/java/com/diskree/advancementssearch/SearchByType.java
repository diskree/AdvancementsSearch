package com.diskree.advancementssearch;

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
            if (searchByType != EVERYWHERE && query.split(QUERY_SEPARATOR)[0].equalsIgnoreCase(searchByType.name())) {
                return searchByType;
            }
        }
        return EVERYWHERE;
    }

    public static String getQueryWithoutMask(String query) {
        return findByMask(query) == EVERYWHERE ? query : query.split(QUERY_SEPARATOR)[1];
    }

    public static String addMaskToQuery(String query, SearchByType searchByType) {
        if (searchByType == EVERYWHERE) {
            return query;
        }
        return searchByType.name().toLowerCase(Locale.ROOT) + QUERY_SEPARATOR + query;
    }
}
