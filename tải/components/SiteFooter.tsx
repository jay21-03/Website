import { Link } from "@tanstack/react-router";
import { Facebook, Instagram, Music2, ShoppingBag } from "lucide-react";
import { NAV_LINKS } from "./SiteHeader";
import { PotIcon } from "./PotIcon";
import { useI18n } from "@/lib/i18n";

export function SiteFooter() {
  const { t } = useI18n();

  return (
    <footer className="mt-24 border-t border-primary/40 bg-card">
      <div className="mx-auto grid max-w-7xl gap-12 px-5 py-16 md:grid-cols-3 lg:px-8">
        <div>
          <div className="flex items-center gap-2.5">
            <PotIcon className="h-7 w-7 text-primary" />
            <span className="font-display text-2xl">Đàng Xem</span>
          </div>
          <p className="mt-3 font-serif text-lg italic text-primary">
            {t("Đất hóa hồn — Tay giữ lửa", "Clay becomes soul — Hands keep the fire")}
          </p>
          <p className="mt-3 max-w-xs text-sm leading-relaxed text-muted-foreground">
            {t(
              "Gốm thủ công Chăm Bàu Trúc — Di sản Văn hóa Phi vật thể UNESCO 2022.",
              "Handmade Cham pottery from Bàu Trúc — UNESCO Intangible Cultural Heritage 2022.",
            )}
          </p>
        </div>

        <div>
          <h3 className="font-display text-lg">{t("Liên Kết", "Links")}</h3>
          <ul className="mt-4 space-y-2.5">
            {NAV_LINKS.map((l) => (
              <li key={l.to}>
                <Link
                  to={l.to}
                  className="text-sm text-muted-foreground transition-colors hover:text-primary"
                >
                  {t(l.vi, l.en)}
                </Link>
              </li>
            ))}
          </ul>
        </div>

        <div>
          <h3 className="font-display text-lg">{t("Kết Nối", "Connect")}</h3>
          <address className="mt-4 space-y-2 text-sm not-italic leading-relaxed text-muted-foreground">
            <p>{t("Làng Bàu Trúc, xã Ninh Phước, Khánh Hòa", "Bàu Trúc Village, Ninh Phước, Khánh Hòa")}</p>
            <p>{t("7:00 – 17:00 hàng ngày", "7:00 – 17:00 daily")}</p>
            <p>lienhe.dangxem@gmail.com</p>
          </address>
          <div className="mt-4 flex gap-3">
            {[
              { Icon: Facebook, label: "Facebook" },
              { Icon: Music2, label: "TikTok" },
              { Icon: Instagram, label: "Instagram" },
              { Icon: ShoppingBag, label: "Shopee" },
            ].map(({ Icon, label }) => (
              <a
                key={label}
                href="#"
                aria-label={label}
                className="rounded-sm border border-primary/40 p-2 text-primary transition-colors hover:bg-primary hover:text-primary-foreground"
              >
                <Icon className="h-4 w-4" />
              </a>
            ))}
          </div>
        </div>
      </div>

      <div className="border-t border-border">
        <p className="mx-auto max-w-7xl px-5 py-6 text-xs text-muted-foreground lg:px-8">
          © {new Date().getFullYear()} Gốm Bàu Trúc — Nghệ Nhân Đàng Xem. Khánh Hòa, Việt Nam.
        </p>
      </div>
    </footer>
  );
}
