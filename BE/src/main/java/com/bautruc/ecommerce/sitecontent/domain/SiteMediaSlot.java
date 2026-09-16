package com.bautruc.ecommerce.sitecontent.domain;

public enum SiteMediaSlot {
    HOME_HERO("hero"),
    HOME_STORY("story"),
    HOME_SOCIAL_1("social-1"),
    HOME_SOCIAL_2("social-2"),
    HOME_SOCIAL_3("social-3"),
    HOME_SOCIAL_4("social-4"),
    HOME_SOCIAL_5("social-5"),
    HOME_SOCIAL_6("social-6");

    private final String pathSegment;

    SiteMediaSlot(String pathSegment) {
        this.pathSegment = pathSegment;
    }

    public String objectKeyPrefix() {
        return "site/home/" + pathSegment + "/";
    }
}
