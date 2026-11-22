// @vitest-environment jsdom
import { fireEvent, render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { PageHero } from "./PageHero";

describe("PageHero image", () => {
  it("renders only the configured image and hides it if loading fails", () => {
    const { container } = render(
      <PageHero title="Journal" seed="journal" imageUrl="/broken-hero.jpg" />
    );
    const image = container.querySelector<HTMLImageElement>(
      'img[src="/broken-hero.jpg"]'
    );
    if (!image) throw new Error("Remote hero image not found");

    fireEvent.error(image);

    expect(image.getAttribute("src")).toBe("/broken-hero.jpg");
    expect(image.style.display).toBe("none");
  });

  it("does not render a stock image when the page has no image", () => {
    const { container } = render(<PageHero title="About" seed="about-hero" />);
    expect(container.querySelector("img")).toBeNull();
  });
});
