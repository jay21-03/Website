import { Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { ArrowRight, Menu, Moon, Phone, ShoppingBag, Sun, X } from "lucide-react";
import { PotIcon } from "./PotIcon";
import { cn } from "@/lib/utils";
import { useI18n } from "@/lib/i18n";
import { useTheme } from "@/lib/theme";
import { useCart } from "@/lib/cart";

export const NAV_LINKS = [
  { to: "/", vi: "Trang Chủ", en: "Home" },
  { to: "/san-pham", vi: "Sản Phẩm", en: "Products" },
  { to: "/trai-nghiem", vi: "Trải Nghiệm", en: "Experience" },
  { to: "/ve-chung-toi", vi: "Về Chúng Tôi", en: "About Us" },
  { to: "/lien-he", vi: "Liên Hệ", en: "Contact" },
] as const;

export function SiteHeader() {
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);
  const { lang, setLang, t } = useI18n();
  const { theme, toggle: toggleTheme } = useTheme();
  const { count, setOpen: setCartOpen } = useCart();

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(() => {
    document.body.style.overflow = open ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [open]);

  const langSwitch = (
    <div className="flex items-center rounded-sm border border-border">
      {(["vi", "en"] as const).map((l) => (
        <button
          key={l}
          type="button"
          onClick={() => setLang(l)}
          aria-pressed={lang === l}
          className={cn(
            "px-2.5 py-1.5 text-xs uppercase tracking-[0.12em] transition-colors",
            lang === l ? "bg-primary text-primary-foreground" : "text-foreground/70 hover:text-primary",
          )}
        >
          {l === "vi" ? "VIE" : "ENG"}
        </button>
      ))}
    </div>
  );

  const themeBtn = (
    <button
      type="button"
      onClick={toggleTheme}
      aria-label={t("Đổi nền sáng/tối", "Toggle light/dark theme")}
      className="rounded-sm border border-border p-2.5 text-foreground/80 transition-colors hover:border-primary hover:text-primary"
    >
      {theme === "dark" ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
    </button>
  );

  const cartBtn = (
    <button
      type="button"
      onClick={() => setCartOpen(true)}
      aria-label={t("Mở giỏ hàng", "Open cart")}
      className="relative rounded-sm border border-border p-2.5 text-foreground/80 transition-colors hover:border-primary hover:text-primary"
    >
      <ShoppingBag className="h-4 w-4" />
      {count > 0 && (
        <span className="absolute -right-2 -top-2 min-w-5 rounded-full bg-primary px-1.5 py-0.5 text-center text-[10px] leading-none text-primary-foreground">
          {count}
        </span>
      )}
    </button>
  );

  return (
    <header
      className={cn(
        "sticky top-0 z-50 transition-all duration-300",
        scrolled || open
          ? "bg-background shadow-[0_2px_16px_rgba(44,26,14,0.12)]"
          : "bg-background/0",
      )}
    >
      <div className="mx-auto grid max-w-7xl grid-cols-[minmax(0,1fr)_auto] items-center gap-3 px-4 py-3 sm:px-5 sm:py-4 lg:px-8">
        <Link to="/" className="flex min-w-0 items-center gap-2.5" onClick={() => setOpen(false)}>
          <PotIcon className="h-7 w-7 shrink-0 text-primary" />
          <span className="truncate font-display text-xl tracking-tight text-foreground sm:text-2xl">
            Đàng Xem
          </span>
        </Link>

        <div className="hidden items-center gap-8 lg:flex">
          <nav className="flex items-center gap-8">
            {NAV_LINKS.map((l) => (
              <Link
                key={l.to}
                to={l.to}
                activeOptions={{ exact: l.to === "/" }}
                activeProps={{ className: "text-primary" }}
                inactiveProps={{ className: "text-foreground/75" }}
                className="text-sm tracking-wide transition-colors hover:text-primary"
              >
                {t(l.vi, l.en)}
              </Link>
            ))}
          </nav>
          <div className="flex items-center gap-2">
            {langSwitch}
            {themeBtn}
            {cartBtn}
          </div>
        </div>

        <div className="flex items-center gap-2 justify-self-end lg:hidden">
          {cartBtn}
          <button
            type="button"
            aria-label={t("Mở menu", "Open menu")}
            aria-expanded={open}
            onClick={() => setOpen((v) => !v)}
            className="rounded-sm border border-border p-2.5 text-foreground"
          >
            {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {/* Mobile full-screen drawer */}
      <div
        className={cn(
          "fixed inset-x-0 bottom-0 top-[57px] sm:top-[68px] z-40 overflow-y-auto border-t border-border bg-background transition-all duration-300 lg:hidden",
          open ? "translate-y-0 opacity-100" : "pointer-events-none -translate-y-2 opacity-0",
        )}
      >
        <nav className="px-5 pt-2">
          {NAV_LINKS.map((l, i) => (
            <Link
              key={l.to}
              to={l.to}
              onClick={() => setOpen(false)}
              activeOptions={{ exact: l.to === "/" }}
              activeProps={{ className: "text-primary" }}
              inactiveProps={{ className: "text-foreground/85" }}
              className="flex items-center justify-between border-b border-border/60 py-4 font-display text-2xl"
            >
              <span>{t(l.vi, l.en)}</span>
              <span className="text-xs tracking-[0.2em] text-primary/60">0{i + 1}</span>
            </Link>
          ))}
        </nav>

        <div className="mt-6 grid gap-3 px-5">
          <Link
            to="/trai-nghiem"
            onClick={() => setOpen(false)}
            className="inline-flex items-center justify-center gap-2 rounded-sm bg-primary px-6 py-3.5 text-sm tracking-wide text-primary-foreground"
          >
            {t("Đặt Lịch Trải Nghiệm", "Book an Experience")} <ArrowRight className="h-4 w-4" />
          </Link>
          <a
            href="tel:+84906123456"
            className="inline-flex items-center justify-center gap-2 rounded-sm border border-primary px-6 py-3.5 text-sm tracking-wide text-primary"
          >
            <Phone className="h-4 w-4" /> {t("Gọi nghệ nhân", "Call the artisan")}
          </a>
        </div>

        <div className="mt-6 flex items-center justify-between px-5 safe-b">
          {langSwitch}
          {themeBtn}
        </div>
      </div>
    </header>
  );
}

