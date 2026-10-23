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
    CATALOG_HERO_DARK("hero-dark"),
    WORKSHOP_HERO("hero"),
    WORKSHOP_HERO_DARK("hero-dark"),
    SUPPORT_HERO("hero"),
    SUPPORT_HERO_DARK("hero-dark"),
    CHECKOUT_HERO("hero"),
    CHECKOUT_HERO_DARK("hero-dark"),
    SITE_LOGO("logo"),
    SITE_LOGO_DARK("logo-dark"),
    SITE_CHAT_ICON("chat-icon"),
    SITE_ZALO_ICON("zalo-icon"),
    SITE_FACEBOOK_ICON("facebook-icon"),
    SITE_TRUST_1_ICON("trust-1-icon"),
    SITE_TRUST_2_ICON("trust-2-icon"),
    SITE_TRUST_3_ICON("trust-3-icon");

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
        if (name().startsWith("SITE_")) return "site/brand/" + pathSegment + "/";
        return "site/" + name().substring(0, name().indexOf('_')).toLowerCase(Locale.ROOT) + "/" + pathSegment + "/";
    }
}
