package com.bautruc.ecommerce.sitecontent.api.request;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record AboutContentRequest(
        @NotNull @Valid TextPair heroTitle,
        @NotNull @Valid TextPair heroSubtitle,
        @NotNull @Valid TextPair storyEyebrow,
        @NotNull @Valid TextPair storyTitle,
        @NotNull @Size(min = 1, max = 10)
        List<@NotNull @Valid TimelineEntry> timeline,
        @NotNull @Valid TextPair valuesEyebrow,
        @NotNull @Valid TextPair valuesTitle,
        @NotNull @Size(min = 1, max = 12)
        List<@NotNull @Valid ValueEntry> values,
        @NotNull @Valid TextPair heritageTitle,
        @NotNull @Valid TextPair heritageBody,
        @NotNull @Valid TextPair certificateEyebrow,
        @NotNull @Valid TextPair certificateTitle,
        @NotNull @Valid TextPair certificateBody,
        @NotNull @Valid TextPair certificateMockTitle,
        @NotNull @Valid TextPair certificateMockPiece,
        @NotNull @Valid TextPair certificateMockMaker,
        @NotNull @Valid TextPair certificateMockSlogan
) {
    public record TextPair(
            @NotBlank @Size(max = 5000) String vi,
            @NotBlank @Size(max = 5000) String en,
            @Pattern(regexp = "heritage|vietnamese|editorial|commerce|minimal|modern") String font,
            @Pattern(regexp = "#[0-9a-fA-F]{6}") String color,
            @Pattern(regexp = "heritage|vietnamese|editorial|commerce|minimal|modern") String darkFont,
            @Pattern(regexp = "#[0-9a-fA-F]{6}") String darkColor
    ) {}

    public record TimelineEntry(
            @NotBlank @Size(max = 20) String year,
            @NotNull @Valid TextPair text
    ) {}

    public record ValueEntry(
            @NotNull @Valid TextPair title,
            @NotNull @Valid TextPair description
    ) {}
}
