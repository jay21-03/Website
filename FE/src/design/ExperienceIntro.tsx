import { useI18n } from './i18n'
import { SectionTitle } from './PageHero'

const steps = [
  ['Tìm hiểu lịch sử gốm Chăm', 'Learn about Cham pottery heritage', 'Nghe nghệ nhân kể về làng Bàu Trúc, đất Nu Lanh và kỹ thuật nung lộ thiên.', 'Hear the artisan tell of Bàu Trúc village, Nu Lanh clay and open-air firing.'],
  ['Tự tay nặn và tạo hình', 'Shape your own piece', 'Không bàn xoay — bạn đi vòng quanh khối đất theo đúng cách người Chăm làm.', 'No wheel — you walk around the clay exactly as the Cham people do.'],
  ['Mang về kỷ niệm độc bản', 'Take home your creation', 'Sản phẩm được hong khô, hoàn thiện và gửi về tận tay bạn.', 'Your piece is dried, finished and delivered to you.'],
]

export function ExperienceIntro() {
  const { t } = useI18n()
  return <section className="mx-auto max-w-7xl px-5 py-24 lg:px-8">
    <SectionTitle eyebrow={t('Trải nghiệm', 'Experience')}>{t('Một Buổi Làm Nghệ Nhân', 'Be an Artisan for a Day')}</SectionTitle>
    <div className="grid gap-10 md:grid-cols-3">
      {steps.map(([vi, en, dVi, dEn], i) => <article key={vi} className="border-t border-primary/40 pt-6">
        <p className="mb-5 font-display text-4xl text-primary/60">0{i + 1}</p>
        <h3 className="font-display text-xl">{t(vi, en)}</h3>
        <p className="mt-3 text-sm leading-relaxed text-muted-foreground">{t(dVi, dEn)}</p>
      </article>)}
    </div>
  </section>
}
