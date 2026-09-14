import { cn } from "./utils";
import heroPottery from "./assets/hero-pottery.jpg";
import artisan from "./assets/artisan.jpg";
import portrait from "./assets/portrait.jpg";
import pVase from "./assets/p-vase.jpg";
import pTea from "./assets/p-tea.jpg";
import pApsara from "./assets/p-apsara.jpg";
import pJug from "./assets/p-jug.jpg";
import pPlate from "./assets/p-plate.jpg";
import pUnique from "./assets/p-unique.jpg";
import pKit from "./assets/p-kit.jpg";
import wsCouple from "./assets/ws-couple.jpg";
import wsFamily from "./assets/ws-family.jpg";
import wsTour from "./assets/ws-tour.jpg";

const LIBRARY: Record<string, string> = {
  "baotruc-hero-clay": heroPottery,
  "baotruc-artisan": artisan,
  "baotruc-vase": pVase,
  "baotruc-house": pTea,
  "baotruc-unique": pUnique,
  "baotruc-feed-1": artisan,
  "baotruc-feed-2": pVase,
  "baotruc-feed-3": pKit,
  "baotruc-feed-4": wsFamily,
  "baotruc-feed-5": pApsara,
  "baotruc-feed-6": wsTour,
  "sp-vase": pVase,
  "sp-tea": pTea,
  "sp-apsara": pApsara,
  "sp-jug": pJug,
  "sp-plate": pPlate,
  "sp-unique": pUnique,
  "sp-kit": pKit,
  "ws-couple": wsCouple,
  "ws-family": wsFamily,
  "ws-tour": wsTour,
  "products-hero": pVase,
  "experience-hero": wsCouple,
  "about-hero": artisan,
  "contact-hero": heroPottery,
  "about-portrait": portrait,
};

/** Warm-toned pottery imagery, resolved from the local asset library. */
export function ClayImage({
  seed,
  alt,
  w = 800,
  h = 800,
  className,
}: {
  seed: string;
  alt: string;
  w?: number;
  h?: number;
  className?: string;
}) {
  const src = LIBRARY[seed] ?? heroPottery;
  return (
    <img
      src={src}
      alt={alt}
      width={w}
      height={h}
      loading="lazy"
      className={cn("h-full w-full rounded-sm object-cover", className)}
    />
  );
}
