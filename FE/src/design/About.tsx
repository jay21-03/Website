
import { Reveal } from "./Reveal";
import { PageHero, SectionTitle } from "./PageHero";
import { useI18n } from "./i18n";
import portrait from "./assets/portrait.jpg";


const TIMELINE = [
  {
    year: "2009",
    vi: "Cơ sở được thành lập tại làng Bàu Trúc, Khánh Hòa.",
    en: "The studio is founded in Bàu Trúc village, Khánh Hòa.",
  },
  {
    year: "2015",
    vi: "Mở rộng dịch vụ trải nghiệm làm gốm cho khách du lịch.",
    en: "Hands-on pottery experiences open to visitors.",
  },
  {
    year: "2022",
    vi: "Gốm Chăm Bàu Trúc được UNESCO công nhận Di sản Văn hóa Phi vật thể.",
    en: "Cham pottery of Bàu Trúc is inscribed by UNESCO as Intangible Cultural Heritage.",
  },
  {
    year: "2023",
    vi: "Hợp tác với Amanoi Resort — đưa gốm Bàu Trúc vào không gian cao cấp.",
    en: "Partnership with Amanoi Resort brings Bàu Trúc pottery into luxury spaces.",
  },
  {
    year: "2026",
    vi: "Bước đầu số hóa — mở rộng kênh phân phối online.",
    en: "First digital steps — expanding online distribution.",
  },
];

const VALUES = [
  { vi: "Độc Bản", en: "Uniqueness", dVi: "Mỗi sản phẩm chỉ tồn tại một lần.", dEn: "Each piece exists only once." },
  {
    vi: "Nguyên Bản",
    en: "Authenticity",
    dVi: "Đúng kỹ thuật Chăm truyền đời, không rút ngắn.",
    dEn: "True Cham technique, passed down and never shortcut.",
  },
  {
    vi: "Bền Vững",
    en: "Sustainability",
    dVi: "Đất, rơm, củi — nguyên liệu địa phương, ít can thiệp.",
    dEn: "Clay, straw and wood — local materials, minimal intervention.",
  },
  {
    vi: "Sáng Tạo",
    en: "Innovation",
    dVi: "Dáng gốm mới cho không gian sống đương đại.",
    dEn: "New forms for contemporary living spaces.",
  },
  {
    vi: "Trải Nghiệm Chiều Sâu",
    en: "Meaningful Experience",
    dVi: "Khách không xem — khách làm.",
    dEn: "Guests don't just watch — they make.",
  },
  {
    vi: "Minh Bạch",
    en: "Integrity",
    dVi: "Rõ nguồn gốc, rõ nghệ nhân, rõ giá.",
    dEn: "Clear origin, clear maker, clear price.",
  },
];

export default function About() {
  const { t } = useI18n();

  return (
    <main>
      <PageHero
        title={t("Người Gìn Giữ Linh Hồn Gốm Chăm", "Keepers of the Cham Pottery Soul")}
        subtitle={t(
          "Nghệ nhân Đàng Xem — làng Bàu Trúc, xã Ninh Phước, Khánh Hòa",
          "Artisan Đàng Xem — Bàu Trúc village, Ninh Phước, Khánh Hòa",
        )}
        seed="about-hero"
      />

      <section className="mx-auto max-w-7xl px-5 py-24 lg:px-8">
        <div className="grid gap-14 lg:grid-cols-[1fr_1.1fr]">
          <Reveal>
            <div className="aspect-3/4 overflow-hidden rounded-sm border border-primary/25">
              <img
                src={portrait}
                alt={t("Chân dung nghệ nhân Đàng Xem", "Portrait of artisan Đàng Xem")}
                width={800}
                height={1060}
                loading="lazy"
                className="h-full w-full object-cover"
              />
            </div>
          </Reveal>
          <Reveal delay={100}>
            <SectionTitle eyebrow={t("Hành trình", "Journey")}>
              {t("Câu Chuyện Của Chúng Tôi", "Our Story")}
            </SectionTitle>
            <ol className="border-l border-primary/35">
              {TIMELINE.map((ti) => (
                <li key={ti.year} className="relative pb-10 pl-8 last:pb-0">
                  <span className="absolute -left-[5px] top-2 h-2.5 w-2.5 rounded-full bg-primary" />
                  <p className="font-display text-2xl text-primary">{ti.year}</p>
                  <p className="mt-1.5 text-sm leading-relaxed text-muted-foreground">{t(ti.vi, ti.en)}</p>
                </li>
              ))}
            </ol>
          </Reveal>
        </div>
      </section>

      <section className="bg-card">
        <div className="mx-auto max-w-7xl px-5 py-24 lg:px-8">
          <Reveal>
            <SectionTitle eyebrow={t("Giá trị cốt lõi", "Core values")}>
              {t("Sáu Điều Chúng Tôi Giữ", "Six Things We Hold On To")}
            </SectionTitle>
          </Reveal>
          <div className="grid gap-x-10 gap-y-10 sm:grid-cols-2 lg:grid-cols-3">
            {VALUES.map((v, i) => (
              <Reveal key={v.vi} delay={i * 70}>
                <div className="border-t border-primary/40 pt-5">
                  <h3 className="font-display text-xl">{t(v.vi, v.en)}</h3>
                  <p className="mt-2.5 text-sm leading-relaxed text-muted-foreground">{t(v.dVi, v.dEn)}</p>
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
              {t(
                "Di Sản Văn Hóa Phi Vật Thể UNESCO 2022",
                "UNESCO Intangible Cultural Heritage 2022",
              )}
            </h2>
            <p className="mt-6 text-base leading-loose opacity-95">
              {t(
                "Nghệ thuật làm gốm Chăm Bàu Trúc là một trong những làng gốm cổ nhất Đông Nam Á còn tồn tại. Được UNESCO ghi danh năm 2022, chúng tôi gánh trên vai trách nhiệm gìn giữ di sản này cho các thế hệ sau.",
                "Bàu Trúc is one of the oldest surviving pottery villages in Southeast Asia. Inscribed by UNESCO in 2022, we carry the responsibility of preserving this heritage for generations to come.",
              )}
            </p>
          </Reveal>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-5 py-24 lg:px-8">
        <div className="grid items-center gap-14 lg:grid-cols-2">
          <Reveal>
            <SectionTitle eyebrow={t("Chứng nhận", "Certification")}>
              Certificate of Authenticity
            </SectionTitle>
            <p className="text-base leading-loose text-muted-foreground">
              {t(
                "Mỗi sản phẩm kèm Certificate of Authenticity — xác nhận nguồn gốc và tên nghệ nhân tạo ra.",
                "Every piece comes with a Certificate of Authenticity confirming its origin and its maker.",
              )}
            </p>
          </Reveal>
          <Reveal delay={120}>
            <div className="rounded-sm border border-primary/40 bg-paper p-10 text-center">
              <p className="text-[11px] uppercase tracking-[0.28em] text-primary">Certificate of Authenticity</p>
              <p className="mt-5 font-display text-2xl">Gốm Chăm Bàu Trúc</p>
              <div className="mx-auto my-6 h-px w-24 bg-primary/50" />
              <p className="text-sm text-muted-foreground">
                {t("Tác phẩm số ____ / độc bản", "Piece no. ____ / one-of-a-kind")}
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                {t("Nghệ nhân: Đàng Xem", "Artisan: Đàng Xem")}
              </p>
              <p className="mt-8 font-serif text-lg italic text-wood">
                {t("Đất hóa hồn — Tay giữ lửa", "Clay becomes soul — Hands keep the fire")}
              </p>
            </div>
          </Reveal>
        </div>
      </section>
    </main>
  );
}
