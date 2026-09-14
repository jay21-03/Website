import { Link, useLocation } from "react-router-dom";
import { CalendarDays, LayoutGrid, Phone, ShoppingBag } from "lucide-react";
import { cn } from "./utils";
import { useI18n } from "./i18n";
import { useCart } from "./cart";

/** Sticky bottom quick-action bar — mobile only. */
export function MobileActionBar({ phone }: { phone?: string }) {
  const { t } = useI18n();
  const { count, setOpen } = useCart();
  const { pathname } = useLocation();

  const base =
    "flex flex-1 flex-col items-center justify-center gap-1 py-2.5 text-[11px] leading-none transition-colors";

  return (
    <div className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-background/95 backdrop-blur-sm safe-b lg:hidden">
      <div className="mx-auto flex max-w-md items-stretch px-2 pt-1">
        <Link
          to="/san-pham"
          className={cn(base, pathname === "/san-pham" ? "text-primary" : "text-foreground/70")}
        >
          <LayoutGrid className="h-5 w-5" />
          {t("Sản phẩm", "Shop")}
        </Link>
        <Link
          to="/trai-nghiem"
          className={cn(base, pathname === "/trai-nghiem" ? "text-primary" : "text-foreground/70")}
        >
          <CalendarDays className="h-5 w-5" />
          {t("Đặt lịch", "Book")}
        </Link>
        <a href={`tel:${phone}`} className={cn(base, "text-foreground/70")}>
          <Phone className="h-5 w-5" />
          {t("Gọi", "Call")}
        </a>
        <button type="button" onClick={() => setOpen(true)} className={cn(base, "relative text-foreground/70")}>
          <ShoppingBag className="h-5 w-5" />
          {t("Giỏ hàng", "Cart")}
          {count > 0 && (
            <span className="absolute right-3 top-1 min-w-4 rounded-full bg-primary px-1 text-[10px] leading-4 text-primary-foreground">
              {count}
            </span>
          )}
        </button>
      </div>
    </div>
  );
}
