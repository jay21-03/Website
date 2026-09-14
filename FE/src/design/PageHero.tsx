import type { ReactNode } from "react";
import { ClayImage } from "./ClayImage";
import { Reveal } from "./Reveal";

export function PageHero({
  title,
  subtitle,
  seed,
  children,
}: {
  title: string;
  subtitle?: string;
  seed: string;
  children?: ReactNode;
}) {
  return (
    <section className="grain relative isolate overflow-hidden border-b border-primary/30">
      <div className="absolute inset-0 -z-10">
        <ClayImage seed={seed} alt="" w={1600} h={900} className="rounded-none opacity-35" />
        <div className="absolute inset-0 bg-background/70" />
      </div>
      <div className="mx-auto max-w-7xl px-5 py-24 sm:py-32 lg:px-8">
        <Reveal>
          <h1 className="max-w-3xl font-display text-4xl leading-[1.1] sm:text-5xl lg:text-6xl">{title}</h1>
          {subtitle && (
            <p className="mt-5 max-w-2xl text-base leading-relaxed text-muted-foreground sm:text-lg">
              {subtitle}
            </p>
          )}
          {children}
        </Reveal>
      </div>
    </section>
  );
}

export function SectionTitle({ children, eyebrow }: { children: ReactNode; eyebrow?: string }) {
  return (
    <div className="mb-12">
      {eyebrow && (
        <p className="mb-3 text-xs uppercase tracking-[0.25em] text-primary">{eyebrow}</p>
      )}
      <h2 className="font-display text-3xl sm:text-4xl">{children}</h2>
    </div>
  );
}
