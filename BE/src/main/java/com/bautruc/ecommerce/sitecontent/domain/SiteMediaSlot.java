package com.bautruc.ecommerce.sitecontent.domain;

import java.util.Locale;

public enum SiteMediaSlot {
    HOME_HERO("hero"),
    HOME_STORY("story"),
    HOME_SOCIAL_1("social-1"),
    HOME_SOCIAL_2("social-2"),
    HOME_SOCIAL_3("social-3"),
    HOME_SOCIAL_4("social-4"),
    HOME_SOCIAL_5("social-5"),
    HOME_SOCIAL_6("social-6"),
    ABOUT_HERO("hero"),
    ABOUT_PORTRAIT("portrait"),
    ABOUT_CERTIFICATE("certificate"),
    CATALOG_HERO("hero"),
    WORKSHOP_HERO("hero"),
    SUPPORT_HERO("hero"),
    CHECKOUT_HERO("hero"),
    SITE_LOGO("logo");

    private final String pathSegment;

    SiteMediaSlot(String pathSegment) {
        this.pathSegment = pathSegment;
    }

    public boolean isAbout() {
        return name().startsWith("ABOUT_");
    }

    public boolean isHome() {
        return name().startsWith("HOME_");
    }

    public String objectKeyPrefix() {
        if (isHome()) return "site/home/" + pathSegment + "/";
        if (isAbout()) return "site/about/" + pathSegment + "/";
        if (this == SITE_LOGO) return "site/brand/logo/";
        return "site/" + name().substring(0, name().indexOf('_')).toLowerCase(Locale.ROOT) + "/" + pathSegment + "/";
    }
}
