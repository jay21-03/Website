import type { SyntheticEvent } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, Leaf, Landmark } from "lucide-react";
import { Reveal } from "./Reveal";
import { SectionTitle } from "./PageHero";
import { PotIcon } from "./PotIcon";
import { useI18n } from "./i18n";
import heroPottery from "./assets/hero-pottery.jpg";
import artisan from "./assets/artisan.jpg";
import pVase from "./assets/p-vase.jpg";
import pKit from "./assets/p-kit.jpg";
import pApsara from "./assets/p-apsara.jpg";
import wsFamily from "./assets/ws-family.jpg";
import wsTour from "./assets/ws-tour.jpg";


const TRUST = [
  {
    icon: PotIcon,
    vi: "100% Thủ Công",
    en: "100% Handmade",
    dVi: "Không khuôn, không bàn xoay máy — tạo hình hoàn toàn bằng tay.",
    dEn: "No molds, no wheel — every piece is shaped entirely by hand.",
  },
  {
    icon: Leaf,
    vi: "Nguyên Liệu Tự Nhiên",
    en: "Natural Materials",
    dVi: "Đất sét lấy từ cánh đồng Nu Lanh, pha cát mịn sông Quao.",
    dEn: "Clay from the Nu Lanh fields blended with fine Quao river sand.",
  },
  {
    icon: Landmark,
    vi: "Di Sản UNESCO",
    en: "UNESCO Heritage",
    dVi: "Nghệ thuật gốm Chăm Bàu Trúc được ghi danh năm 2022.",
    dEn: "Cham pottery of Bàu Trúc was inscribed by UNESCO in 2022.",
  },
];

type HomepageMedia = {
  slot: string;
  url?: string | null;
};

type HomepageProduct = {
  slot?: number;
  id: number;
  nameVi: string;
  nameEn?: string | null;
  descriptionVi?: string | null;
  descriptionEn?: string | null;
  thumbnailUrl?: string | null;
};

type HomepageData = {
  heroImageUrl?: string | null;
  storyImageUrl?: string | null;
  socialImages?: HomepageMedia[];
  featuredProducts?: HomepageProduct[];
};

type HomeProps = {
  home?: HomepageData | null;
  fallbackProducts?: HomepageProduct[];
};

const SOCIAL_FALLBACKS = [artisan, pVase, pKit, wsFamily, pApsara, wsTour];

function imageFallback(event: SyntheticEvent<HTMLImageElement>, fallback: string) {
  if (event.currentTarget.dataset.fallbackApplied === "true") return;
  event.currentTarget.dataset.fallbackApplied = "true";
  event.currentTarget.src = fallback;
}


export default function Home({ home, fallbackProducts = [] }: HomeProps) {
  const { t } = useI18n();
  const managedFeatured = [...(home?.featuredProducts ?? [])].sort((a, b) => (a.slot ?? 0) - (b.slot ?? 0));
  const featuredProducts = managedFeatured.length === 3 ? managedFeatured : fallbackProducts.slice(0, 3);
  const socialBySlot = new Map((home?.socialImages ?? []).map(item => [item.slot, item.url]));

  return (
    <main>
      {/* Hero */}
      <section className="grain relative isolate flex min-h-[88svh] items-center sm:min-h-screen overflow-hidden">
        <div className="absolute inset-0 -z-10">
          <img
            src={home?.heroImageUrl || heroPottery}
            alt={t("Nghệ nhân Chăm tạo hình gốm bằng tay", "A Cham artisan shaping pottery by hand")}
            width={1600}
            height={1008}
            className="h-full w-full object-cover"
            onError={event => imageFallback(event, heroPottery)}
          />
          <div className="absolute inset-0 bg-background/72" />
        </div>

        <div className="absolute right-4 top-4 z-10 sm:top-24 lg:right-8">
          <span className="inline-flex items-center gap-2 rounded-sm border border-accent bg-accent/25 px-3.5 py-1.5 text-[11px] uppercase tracking-[0.18em] text-foreground">
            <Landmark className="h-3.5 w-3.5" /> UNESCO Heritage 2022
          </span>
        </div>

        <div className="mx-auto w-full max-w-7xl px-5 py-24 sm:py-32 lg:px-8">
          <Reveal>
            <p className="mb-6 text-xs uppercase tracking-[0.3em] text-primary">
              {t("Làng gốm Bàu Trúc · Khánh Hòa", "Bàu Trúc pottery village · Khánh Hòa")}
            </p>
            <h1 className="max-w-4xl font-display text-[2.4rem] leading-[1.08] sm:text-6xl lg:text-7xl">
              {t("Tinh hoa gốm Chăm - Gìn giữ hồn di sản", "The essence of Cham pottery - preserving heritage soul")}
            </h1>
            <p className="mt-6 font-serif text-xl italic text-wood sm:text-2xl">
              {t(
                "Gốm thủ công Chăm Bàu Trúc | Di sản UNESCO 2022",
                "Handmade Cham pottery from Bàu Trúc | UNESCO Heritage 2022",
              )}
            </p>
            <p className="mt-6 max-w-2xl text-base leading-relaxed text-muted-foreground sm:text-lg">
              {t(
                "Mỗi sản phẩm là một phiên bản duy nhất — được tạo ra hoàn toàn bằng đôi bàn tay của nghệ nhân Chăm, từ đất sét tự nhiên làng Bàu Trúc, Khánh Hòa.",
                "Every piece is one of a kind — shaped entirely by the hands of Cham artisans from natural clay of Bàu Trúc village, Khánh Hòa.",
              )}
            </p>
            <div className="mt-9 flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:gap-4">
              <Link
                to="/san-pham"
                className="inline-flex items-center justify-center gap-2 rounded-sm bg-primary px-7 py-3.5 text-sm tracking-wide text-primary-foreground transition-colors hover:bg-wood"
              >
                {t("Khám Phá Sản Phẩm", "Explore Products")} <ArrowRight className="h-4 w-4" />
              </Link>
              <Link
                to="/trai-nghiem"
                className="inline-flex items-center justify-center rounded-sm border border-primary px-7 py-3.5 text-sm tracking-wide text-primary transition-colors hover:bg-primary hover:text-primary-foreground"
              >
                {t("Đặt Lịch Trải Nghiệm", "Book an Experience")}
              </Link>
            </div>
          </Reveal>
        </div>
      </section>

      {/* Trust bar */}
      <section className="rule-clay border-b border-primary/45">
        <div className="mx-auto grid max-w-7xl gap-10 px-5 py-14 sm:grid-cols-3 lg:px-8">
          {TRUST.map(({ icon: Icon, vi, en, dVi, dEn }, i) => (
            <Reveal key={vi} delay={i * 90} className="flex gap-4">
              <Icon className="mt-1 h-8 w-8 shrink-0 text-primary" />
              <div className="min-w-0">
                <h3 className="font-display text-xl">{t(vi, en)}</h3>
                <p className="mt-1.5 text-sm leading-relaxed text-muted-foreground">{t(dVi, dEn)}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </section>

      {/* Story teaser */}
      <section className="mx-auto max-w-7xl px-5 py-16 sm:py-24 lg:px-8">
        <div className="grid items-center gap-10 sm:gap-14 lg:grid-cols-2">
          <Reveal>
            <div className="aspect-4/3 overflow-hidden rounded-sm border border-primary/25">
              <img
                src={home?.storyImageUrl || artisan}
                alt={t("Nghệ nhân Đàng Xem bên lò nung gốm", "Artisan Đàng Xem beside the open kiln")}
                width={1000}
                height={750}
                loading="lazy"
                className="h-full w-full object-cover"
                onError={event => imageFallback(event, artisan)}
              />
            </div>
          </Reveal>
          <Reveal delay={120}>
            <p className="mb-3 text-xs uppercase tracking-[0.25em] text-primary">{t("Câu chuyện", "Our story")}</p>
            <h2 className="font-display text-3xl sm:text-4xl">
              {t("Hơn 15 năm giữ lửa nghề Chăm", "Over 15 years keeping the Cham craft alive")}
            </h2>
            <p className="mt-6 text-base leading-loose text-muted-foreground">
              {t(
                "Từ năm 2009, Nghệ nhân Đàng Xem đã gìn giữ và phát triển nghề gốm truyền thống của người Chăm tại làng Bàu Trúc. Mỗi sản phẩm mang trong mình hàng trăm năm lịch sử và dấu ấn riêng của đôi bàn tay tạo ra nó.",
                "Since 2009, artisan Đàng Xem has preserved and developed the traditional Cham pottery craft in Bàu Trúc village. Each piece carries centuries of history and the mark of the hands that made it.",
              )}
            </p>
            <Link
              to="/ve-chung-toi"
              className="mt-8 inline-flex items-center gap-2 border-b border-primary pb-1 text-sm text-primary transition-colors hover:text-wood"
            >
              {t("Đọc thêm câu chuyện của chúng tôi →", "Read more of our story →")}
            </Link>
          </Reveal>
        </div>
      </section>

      {/* Featured products */}
      <section className="mx-auto max-w-7xl px-5 py-16 lg:px-8">
        <Reveal>
          <SectionTitle eyebrow={t("Bộ sưu tập", "Collection")}>
            {t("Sản Phẩm Nổi Bật", "Featured Products")}
          </SectionTitle>
        </Reveal>
        {featuredProducts.length > 0 ? (
          <div className="grid gap-8 md:grid-cols-3">
            {featuredProducts.map((product, i) => {
              const name = t(product.nameVi, product.nameEn || product.nameVi);
              const description = t(
                product.descriptionVi || "Gốm Bàu Trúc được tạo hình thủ công theo kỹ thuật truyền thống của người Chăm.",
                product.descriptionEn || "Bàu Trúc pottery shaped by hand using traditional Cham techniques.",
              );
              return (
                <Reveal key={product.id} delay={i * 100} as="article">
                  <div className="group flex h-full flex-col rounded-sm border border-border bg-card transition-shadow duration-300 hover:shadow-[0_14px_36px_rgba(44,26,14,0.16)]">
                    <div className="aspect-square overflow-hidden rounded-t-sm">
                      <img
                        src={product.thumbnailUrl || pVase}
                        alt={name}
                        width={700}
                        height={700}
                        loading="lazy"
                        className="h-full w-full object-cover"
                        onError={event => imageFallback(event, pVase)}
                      />
                    </div>
                    <div className="flex flex-1 flex-col p-6">
                      <h3 className="font-display text-xl">{name}</h3>
                      <p className="mt-2 flex-1 text-sm leading-relaxed text-muted-foreground">{description}</p>
                      <Link
                        to={`/products/${product.id}`}
                        className="mt-6 inline-flex w-fit items-center gap-2 rounded-sm border border-primary px-5 py-2.5 text-sm text-primary transition-colors hover:bg-primary hover:text-primary-foreground"
                      >
                        {t("Xem Chi Tiết", "View Details")} <ArrowRight className="h-4 w-4" />
                      </Link>
                    </div>
                  </div>
                </Reveal>
              );
            })}
          </div>
        ) : (
          <div className="rounded-sm border border-border bg-card px-6 py-10 text-center text-sm text-muted-foreground">
            {t("Sản phẩm nổi bật đang được cập nhật.", "Featured products are being updated.")}
          </div>
        )}
      </section>

      {/* Experience teaser */}
      <section className="grain relative mt-16 overflow-hidden bg-card">
        <div className="mx-auto max-w-3xl px-5 py-24 text-center lg:px-8">
          <Reveal>
            <h2 className="font-display text-3xl sm:text-4xl">
              {t("Trải Nghiệm Làm Gốm Cùng Nghệ Nhân", "Make Pottery with the Artisan")}
            </h2>
            <p className="mt-6 text-base leading-loose text-muted-foreground">
              {t(
                "Đặt tay vào đất sét — cảm nhận hàng trăm năm văn hóa Chăm qua đôi bàn tay của chính bạn. Workshop phù hợp cho cá nhân, gia đình và đoàn tour.",
                "Put your hands in the clay and feel centuries of Cham culture. Workshops for individuals, families and tour groups.",
              )}
            </p>
            <Link
              to="/trai-nghiem"
              className="mt-10 inline-flex items-center gap-2 rounded-sm bg-primary px-8 py-3.5 text-sm tracking-wide text-primary-foreground transition-colors hover:bg-wood"
            >
              {t("Đặt Lịch Ngay", "Book Now")} <ArrowRight className="h-4 w-4" />
            </Link>
          </Reveal>
        </div>
      </section>

      {/* Partners */}
      <section className="mx-auto max-w-5xl px-5 py-24 text-center lg:px-8">
        <Reveal>
          <h2 className="font-display text-3xl">{t("Đối Tác Tin Cậy", "Trusted Partners")}</h2>
          <div className="mt-10 flex flex-wrap items-center justify-center gap-x-14 gap-y-6 font-serif text-xl text-wood">
            <span>Amanoi Resort</span>
            <span className="hidden h-5 w-px bg-primary/40 sm:block" />
            <span>{t("Tour lữ hành địa phương", "Local tour operators")}</span>
            <span className="hidden h-5 w-px bg-primary/40 sm:block" />
            <span>UNESCO</span>
          </div>
        </Reveal>
      </section>

      {/* Social feed */}
      <section className="mx-auto max-w-7xl px-5 pb-8 lg:px-8">
        <Reveal className="text-center">
          <h2 className="font-display text-3xl">{t("Theo Dõi Hành Trình Gốm", "Follow the Pottery Journey")}</h2>
          <p className="mt-3 text-sm tracking-wide text-primary">@dangxem.baoutruc</p>
        </Reveal>
        <div className="mt-10 grid grid-cols-2 gap-4 sm:grid-cols-3">
          {[1, 2, 3, 4, 5, 6].map((n, i) => {
            const fallback = SOCIAL_FALLBACKS[i];
            const src = socialBySlot.get(`HOME_SOCIAL_${n}`) || fallback;
            return (
              <Reveal key={n} delay={i * 60}>
                <div className="aspect-square overflow-hidden rounded-sm border border-border">
                  <img
                    src={src}
                    alt={t(`Ảnh xưởng gốm ${n}`, `Pottery workshop photo ${n}`)}
                    width={600}
                    height={600}
                    loading="lazy"
                    className="h-full w-full rounded-sm object-cover"
                    onError={event => imageFallback(event, fallback)}
                  />
                </div>
              </Reveal>
            );
          })}
        </div>

      </section>
    </main>
  );
}
