import { useI18n } from './i18n'
import { SectionTitle } from './PageHero'
import { presentationNode } from './sitePresentation'

export function ExperienceIntro({ presentation }: { presentation?: object | null }) {
  const { lang } = useI18n()
  const copy = (key: string) => presentationNode(presentation, 'workshop', key, lang)
  return <section className="mx-auto max-w-7xl px-5 py-24 lg:px-8">
    <SectionTitle eyebrow={copy('introEyebrow')}>{copy('introTitle')}</SectionTitle>
    <div className="grid gap-10 md:grid-cols-3">
      {[1, 2, 3].map((n, i) => <article key={n} className="border-t border-primary/40 pt-6">
        <p className="mb-5 font-display text-4xl text-primary/60">0{i + 1}</p>
        <h3 className="font-display text-xl">{copy(`step${i + 1}Title`)}</h3>
        <p className="mt-3 text-sm leading-relaxed text-muted-foreground">{copy(`step${i + 1}Body`)}</p>
      </article>)}
    </div>
  </section>
}
