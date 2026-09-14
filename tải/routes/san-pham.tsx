import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { ArrowRight, ShoppingBag } from "lucide-react";
import { toast } from "sonner";
import { Reveal } from "@/components/Reveal";
import { PageHero } from "@/components/PageHero";
import { useI18n } from "@/lib/i18n";
import { useCart } from "@/lib/cart";
import pVase from "@/assets/p-vase.jpg";
import pTea from "@/assets/p-tea.jpg";
import pApsara from "@/assets/p-apsara.jpg";
import pJug from "@/assets/p-jug.jpg";
import pPlate from "@/assets/p-plate.jpg";
import pUnique from "@/assets/p-unique.jpg";
import pKit from "@/assets/p-kit.jpg";

export const Route = createFileRoute("/san-pham")({
  head: () => ({
    meta: [
      { title: "Sản Phẩm Gốm Chăm Bàu Trúc | Đàng Xem" },
      {
        name: "description",
        content:
          "Bình cắm hoa, chén trà, tượng vũ nữ Chăm, bình nước truyền thống và tác phẩm độc bản — gốm thủ công Bàu Trúc từ xưởng Đàng Xem.",
      },
      { property: "og:title", content: "Sản Phẩm Gốm Chăm Bàu Trúc | Đàng Xem" },
      {
        property: "og:description",
        content: "Từng sản phẩm — một câu chuyện. Gốm Chăm thủ công, nung lộ thiên truyền thống.",
      },
    ],
  }),
  component: Products,
});

const FILTERS = [
  { key: "all", vi: "Tất Cả", en: "All" },
  { key: "deco", vi: "Trang Trí", en: "Decorative" },
  { key: "home", vi: "Gia Dụng", en: "Homeware" },
  { key: "unique", vi: "Độc Bản", en: "One-of-a-kind" },
  { key: "workshop", vi: "Workshop", en: "Workshop" },
] as const;

const PRODUCTS = [
  {
    id: "vase",
    vi: "Bình Cắm Hoa Chăm",
    en: "Cham Flower Vase",
    cat: "deco",
    descVi: "Dáng bình cổ cao, hoa văn sóng nước khắc tay, vệt lửa nung loang tự nhiên.",
    descEn: "Tall-necked vase with hand-carved wave motifs and natural fire marks.",
    price: 850000,
    priceVi: "850.000₫ – 1.400.000₫",
    priceEn: "850,000₫ – 1,400,000₫",
    img: pVase,
  },
  {
    id: "tea",
    vi: "Chén Uống Trà",
    en: "Tea Bowl Set",
    cat: "home",
    descVi: "Bộ 4 chén mộc, thành mỏng, giữ nhiệt tốt, chạm tay ấm và nhám nhẹ.",
    descEn: "Set of 4 unglazed bowls — thin walls, warm to the touch, great heat retention.",
    price: 520000,
    priceVi: "520.000₫ / bộ",
    priceEn: "520,000₫ / set",
    img: pTea,
  },
  {
    id: "apsara",
    vi: "Tượng Vũ Nữ Chăm",
    en: "Cham Dancer Figurine",
    cat: "deco",
    descVi: "Tượng Apsara nặn tay, mô phỏng điêu khắc tháp Chăm cổ.",
    descEn: "Hand-sculpted Apsara inspired by ancient Cham tower reliefs.",
    price: 0,
    priceVi: "Liên hệ để biết giá",
    priceEn: "Price on request",
    img: pApsara,
  },
  {
    id: "jug",
    vi: "Bình Nước Truyền Thống",
    en: "Traditional Water Jug",
    cat: "home",
    descVi: "Bình đựng nước kiểu Chăm cổ, thành gốm xốp giúp nước luôn mát.",
    descEn: "Classic Cham water jug; porous clay keeps water naturally cool.",
    price: 680000,
    priceVi: "680.000₫",
    priceEn: "680,000₫",
    img: pJug,
  },
  {
    id: "plate",
    vi: "Đĩa Trang Trí",
    en: "Decorative Plate",
    cat: "deco",
    descVi: "Đĩa treo tường khắc hoa văn hình học Chăm, đường kính 28cm.",
    descEn: "Wall plate carved with Cham geometric patterns, 28cm diameter.",
    price: 450000,
    priceVi: "450.000₫",
    priceEn: "450,000₫",
    img: pPlate,
  },
  {
    id: "unique",
    vi: "Lọ Hoa Độc Bản",
    en: "One-of-a-kind Vase",
    cat: "unique",
    unique: true,
    descVi: "Tác phẩm duy nhất, kèm Certificate of Authenticity ghi tên nghệ nhân.",
    descEn: "A single unrepeatable piece with a Certificate of Authenticity.",
    price: 0,
    priceVi: "Liên hệ để biết giá",
    priceEn: "Price on request",
    img: pUnique,
  },
  {
    id: "kit",
    vi: "Bộ Kit Workshop",
    en: "Workshop Clay Kit",
    cat: "workshop",
    descVi: "Đất sét Nu Lanh, vòng tre và dụng cụ miết — dùng trong buổi trải nghiệm.",
    descEn: "Nu Lanh clay, bamboo ring and smoothing tools used in the workshop.",
    price: 0,
    priceVi: "Bao gồm trong workshop",
    priceEn: "Included in the workshop",
    img: pKit,
  },
];

function Products() {
  const { t, lang } = useI18n();
  const { add, setOpen } = useCart();
  const [active, setActive] = useState<string>("all");
  const list = active === "all" ? PRODUCTS : PRODUCTS.filter((p) => p.cat === active);

  return (
    <main>
      <PageHero
        title={t("Từng Sản Phẩm — Một Câu Chuyện", "Every Piece — A Story")}
        subtitle={t(
          "Gốm nặn tay, nung lộ thiên bằng rơm và củi. Không sản phẩm nào giống sản phẩm nào.",
          "Hand-shaped pottery, open-fired with straw and wood. No two pieces are alike.",
        )}
        seed="products-hero"
      />

      <section className="mx-auto max-w-7xl px-5 py-16 lg:px-8">
        <div className="flex flex-wrap gap-3 border-b border-primary/30 pb-6">
          {FILTERS.map((f) => (
            <button
              key={f.key}
              type="button"
              onClick={() => setActive(f.key)}
              className={`rounded-sm border px-5 py-2 text-sm transition-colors ${
                active === f.key
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border text-muted-foreground hover:border-primary hover:text-primary"
              }`}
            >
              {t(f.vi, f.en)}
            </button>
          ))}
        </div>

        <div className="mt-10 grid grid-cols-2 gap-4 sm:mt-12 sm:gap-8 lg:grid-cols-3">
          {list.map((p, i) => (
            <Reveal key={p.id} delay={i * 70} as="article">
              <div className="flex h-full flex-col rounded-sm border border-border bg-card transition-shadow duration-300 hover:shadow-[0_14px_36px_rgba(44,26,14,0.16)]">
                <div className="relative aspect-square overflow-hidden rounded-t-sm">
                  <img
                    src={p.img}
                    alt={`${p.vi} — ${p.en}`}
                    width={700}
                    height={700}
                    loading="lazy"
                    className="h-full w-full object-cover"
                  />
                  {p.unique && (
                    <span className="absolute left-2.5 top-2.5 rounded-sm bg-accent px-2 py-1 text-[9px] uppercase tracking-[0.14em] text-accent-foreground sm:left-4 sm:top-4 sm:px-3 sm:text-[11px]">
                      {t("Độc Bản", "Unique")}
                    </span>
                  )}
                </div>
                <div className="flex flex-1 flex-col p-4 sm:p-6">
                  <h3 className="font-display text-base leading-snug sm:text-xl">{t(p.vi, p.en)}</h3>
                  <p className="mt-0.5 font-serif text-xs italic text-wood sm:text-sm">{lang === "vi" ? p.en : p.vi}</p>
                  <p className="mt-2 line-clamp-3 flex-1 text-xs leading-relaxed text-muted-foreground sm:mt-3 sm:line-clamp-none sm:text-sm">
                    {t(p.descVi, p.descEn)}
                  </p>
                  <p className="mt-3 text-sm text-primary sm:mt-4">{t(p.priceVi, p.priceEn)}</p>
                  <div className="mt-4 flex flex-col gap-2 sm:mt-5 sm:flex-row sm:flex-wrap sm:gap-3">
                    <button
                      type="button"
                      onClick={() => {
                        add({ id: p.id, nameVi: p.vi, nameEn: p.en, price: p.price, image: p.img });
                        toast.success(
                          t(`Đã thêm "${p.vi}" vào giỏ hàng.`, `Added "${p.en}" to your cart.`),
                          {
                            action: {
                              label: t("Xem giỏ", "View cart"),
                              onClick: () => setOpen(true),
                            },
                          },
                        );
                      }}
                      className="inline-flex items-center justify-center gap-2 rounded-sm bg-primary px-4 py-2.5 text-xs text-primary-foreground transition-colors hover:bg-wood sm:px-5 sm:text-sm"
                    >
                      <ShoppingBag className="hidden h-4 w-4 sm:block" />
                      <span className="whitespace-nowrap">{t("Thêm Vào Giỏ", "Add to Cart")}</span>
                    </button>
                    <Link
                      to="/lien-he"
                      className="inline-flex items-center justify-center gap-2 rounded-sm border border-primary px-4 py-2.5 text-xs text-primary transition-colors hover:bg-primary hover:text-primary-foreground sm:px-5 sm:text-sm"
                    >
                      <span className="whitespace-nowrap">{t("Tìm Hiểu Thêm", "Learn More")}</span> <ArrowRight className="h-4 w-4 shrink-0" />
                    </Link>
                  </div>
                </div>
              </div>
            </Reveal>
          ))}
        </div>

        <Reveal className="mt-14 rounded-sm border border-primary/35 bg-card p-6 text-center sm:mt-20 sm:p-10">
          <p className="font-serif text-xl italic text-wood">
            {t(
              "Cần sản phẩm theo yêu cầu? Chúng tôi nhận đặt hàng độc bản.",
              "Need something custom? We accept commissions for one-of-a-kind pieces.",
            )}
          </p>
          <Link
            to="/lien-he"
            className="mt-6 inline-flex items-center gap-2 rounded-sm bg-primary px-7 py-3.5 text-sm text-primary-foreground transition-colors hover:bg-wood"
          >
            {t("Liên Hệ Đặt Hàng", "Contact to Order")} <ArrowRight className="h-4 w-4" />
          </Link>
        </Reveal>
      </section>
    </main>
  );
}
