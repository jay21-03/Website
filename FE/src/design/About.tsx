import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { api } from "../api";
import { Reveal } from "./Reveal";
import { PageHero, SectionTitle } from "./PageHero";
import { useI18n } from "./i18n";
import { aboutDefaults } from "./aboutDefaults";
import { fontPresets } from "./sitePresentation";
import { useTheme } from "./theme";
import portrait from "./assets/portrait.jpg";

type TextPair = { vi: string; en: string; font?: string; color?: string; darkFont?: string; darkColor?: string };
type TimelineEntry = { year: string; text: TextPair };
type ValueEntry = { title: TextPair; description: TextPair };

export default function About() {
  const { t } = useI18n();
  const { theme } = useTheme();
  const preview = new URLSearchParams(window.location.search).get("presentationPreview") === "1";
  const [draft, setDraft] = useState<{ content: typeof aboutDefaults; media: { heroUrl?: string | null; portraitUrl?: string | null; certificateUrl?: string | null } } | null>(null);

  useEffect(() => {
    if (!preview) return;
    const receive = (event: MessageEvent) => {
      if (event.origin === window.location.origin && event.source === window.parent && event.data?.type === "site-presentation-preview") setDraft(event.data.about || null);
    };
    window.addEventListener("message", receive);
    return () => window.removeEventListener("message", receive);
  }, [preview]);

  const query = useQuery({
    queryKey: ["about"],
    queryFn: api.about
  });

  const content = draft?.content || query.data?.content || aboutDefaults;
  const heroUrl = (draft ? draft.media.heroUrl : query.data?.heroUrl) || null;
  const portraitUrl = (draft ? draft.media.portraitUrl : query.data?.portraitUrl) || portrait;
  const certificateUrl = (draft ? draft.media.certificateUrl : query.data?.certificateUrl) || null;

  const [certificateError, setCertificateError] = useState(false);

  useEffect(() => {
    setCertificateError(false);
  }, [certificateUrl]);

  const text = (value: { vi: string; en: string }) =>
    t(value.vi, value.en);
  const styled = (value: TextPair, key: string) => {
    const font = theme === "dark" ? value.darkFont || value.font : value.font;
    const color = theme === "dark" ? value.darkColor || value.color : value.color;
    return <span data-preview-target={`about:${key}`} style={{ ...(font ? { fontFamily: `"${fontPresets[font as keyof typeof fontPresets]?.heading || fontPresets.heritage.heading}", "Be Vietnam Pro", sans-serif` } : {}), ...(color ? { color } : {}) }}>{text(value)}</span>;
  };

  return (
    <main>
      <PageHero
        title={styled(content.heroTitle, "heroTitle")}
        subtitle={styled(content.heroSubtitle, "heroSubtitle")}
        seed="about-hero"
        imageUrl={heroUrl}
      />

      <section className="mx-auto max-w-7xl px-5 py-24 lg:px-8">
        <div className="grid gap-14 lg:grid-cols-[1fr_1.1fr]">
          <Reveal>
            <div className="aspect-3/4 overflow-hidden rounded-sm border border-primary/25">
              <img
                src={portraitUrl}
                alt={t(
                  "Chân dung nghệ nhân Đàng Xem",
                  "Portrait of artisan Đàng Xem"
                )}
                width={800}
                height={1060}
                loading="lazy"
                className="h-full w-full object-cover"
                onError={event => {
                  if (event.currentTarget.src !== new URL(portrait, window.location.href).href) {
                    event.currentTarget.src = portrait;
                  }
                }}
              />
            </div>
          </Reveal>

          <Reveal delay={100}>
            <SectionTitle eyebrow={styled(content.storyEyebrow, "storyEyebrow")}>
              {styled(content.storyTitle, "storyTitle")}
            </SectionTitle>

            <ol className="border-l border-primary/35">
              {content.timeline.map((item: TimelineEntry, index: number) => (
                <li
                  key={`${item.year}-${index}`}
                  className="relative pb-10 pl-8 last:pb-0"
                >
                  <span className="absolute -left-[5px] top-2 h-2.5 w-2.5 rounded-full bg-primary" />
                  <p className="font-display text-2xl text-primary">
                    {item.year}
                  </p>
                  <p className="mt-1.5 whitespace-pre-line text-sm leading-relaxed text-muted-foreground">
                    {styled(item.text, `timeline.${index}.text`)}
                  </p>
                </li>
              ))}
            </ol>
          </Reveal>
        </div>
      </section>

      <section className="bg-card">
        <div className="mx-auto max-w-7xl px-5 py-24 lg:px-8">
          <Reveal>
            <SectionTitle eyebrow={styled(content.valuesEyebrow, "valuesEyebrow")}>
              {styled(content.valuesTitle, "valuesTitle")}
            </SectionTitle>
          </Reveal>

          <div className="grid gap-x-10 gap-y-10 sm:grid-cols-2 lg:grid-cols-3">
            {content.values.map((item: ValueEntry, index: number) => (
              <Reveal key={index} delay={index * 70}>
                <div className="border-t border-primary/40 pt-5">
                  <h3 className="font-display text-xl">
                    {styled(item.title, `values.${index}.title`)}
                  </h3>
                  <p className="mt-2.5 whitespace-pre-line text-sm leading-relaxed text-muted-foreground">
                    {styled(item.description, `values.${index}.description`)}
                  </p>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      <section className="grain relative overflow-hidden bg-primary text-primary-foreground">
        <div className="mx-auto max-w-3xl px-5 py-24 text-center lg:px-8">
          <Reveal>
            <h2 className="font-display text-3xl leading-snug sm:text-4xl">
              {styled(content.heritageTitle, "heritageTitle")}
            </h2>
            <p className="mt-6 whitespace-pre-line text-base leading-loose opacity-95">
              {styled(content.heritageBody, "heritageBody")}
            </p>
          </Reveal>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-5 py-24 lg:px-8">
        <div className="grid items-center gap-14 lg:grid-cols-2">
          <Reveal>
            <SectionTitle eyebrow={styled(content.certificateEyebrow, "certificateEyebrow")}>
              {styled(content.certificateTitle, "certificateTitle")}
            </SectionTitle>

            <p className="whitespace-pre-line text-base leading-loose text-muted-foreground">
              {styled(content.certificateBody, "certificateBody")}
            </p>
          </Reveal>

          <Reveal delay={120}>
            {certificateUrl && !certificateError ? (
              <div className="overflow-hidden rounded-sm border border-primary/40 bg-paper">
                <img
                  src={certificateUrl}
                  alt={text(content.certificateTitle)}
                  className="h-auto w-full object-contain"
                  loading="lazy"
                  onError={() => setCertificateError(true)}
                />
              </div>
            ) : (
              <div className="rounded-sm border border-primary/40 bg-paper p-10 text-center">
                <p className="text-[11px] uppercase tracking-[0.28em] text-primary">
                  {styled(content.certificateTitle, "certificateTitle")}
                </p>
                <p className="mt-5 font-display text-2xl">
                  {styled(content.certificateMockTitle, "certificateMockTitle")}
                </p>
                <div className="mx-auto my-6 h-px w-24 bg-primary/50" />
                <p className="text-sm text-muted-foreground">
                  {styled(content.certificateMockPiece, "certificateMockPiece")}
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {styled(content.certificateMockMaker, "certificateMockMaker")}
                </p>
                <p className="mt-8 font-serif text-lg italic text-wood">
                  {styled(content.certificateMockSlogan, "certificateMockSlogan")}
                </p>
                <small className="mt-5 block text-muted-foreground">
                  {t(
                    "Hình minh họa — không phải chứng nhận được phát hành.",
                    "Illustration only — not an issued certificate."
                  )}
                </small>
              </div>
            )}
          </Reveal>
        </div>
      </section>
    </main>
  );
}
