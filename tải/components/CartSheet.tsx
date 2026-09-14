import { Minus, Plus, ShoppingBag, Trash2 } from "lucide-react";
import { toast } from "sonner";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { formatVnd, useCart } from "@/lib/cart";
import { useI18n } from "@/lib/i18n";

export function CartSheet() {
  const { items, open, setOpen, setQty, remove, clear, total, hasQuoteItem } = useCart();
  const { t, lang } = useI18n();

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetContent className="flex w-full flex-col bg-background sm:max-w-md">
        <SheetHeader>
          <SheetTitle className="font-display text-2xl">{t("Giỏ Hàng", "Your Cart")}</SheetTitle>
          <SheetDescription>
            {t(
              "Chúng tôi sẽ liên hệ xác nhận đơn và phí vận chuyển.",
              "We will contact you to confirm your order and shipping.",
            )}
          </SheetDescription>
        </SheetHeader>

        {items.length === 0 ? (
          <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6 text-center">
            <ShoppingBag className="h-10 w-10 text-primary/50" />
            <p className="text-sm text-muted-foreground">
              {t("Giỏ hàng đang trống.", "Your cart is empty.")}
            </p>
          </div>
        ) : (
          <div className="flex-1 space-y-4 overflow-y-auto px-4">
            {items.map((it) => (
              <div key={it.id} className="flex gap-4 rounded-sm border border-border bg-card p-3">
                <img
                  src={it.image}
                  alt={lang === "vi" ? it.nameVi : it.nameEn}
                  className="h-20 w-20 shrink-0 rounded-sm object-cover"
                  loading="lazy"
                />
                <div className="min-w-0 flex-1">
                  <p className="truncate font-display text-base">{lang === "vi" ? it.nameVi : it.nameEn}</p>
                  <p className="mt-0.5 text-sm text-primary">
                    {it.price > 0 ? formatVnd(it.price) : t("Liên hệ", "On request")}
                  </p>
                  <div className="mt-2 flex items-center gap-2">
                    <button
                      type="button"
                      aria-label={t("Giảm số lượng", "Decrease quantity")}
                      onClick={() => setQty(it.id, it.qty - 1)}
                      className="rounded-sm border border-border p-1 text-foreground/70 hover:border-primary hover:text-primary"
                    >
                      <Minus className="h-3.5 w-3.5" />
                    </button>
                    <span className="w-6 text-center text-sm">{it.qty}</span>
                    <button
                      type="button"
                      aria-label={t("Tăng số lượng", "Increase quantity")}
                      onClick={() => setQty(it.id, it.qty + 1)}
                      className="rounded-sm border border-border p-1 text-foreground/70 hover:border-primary hover:text-primary"
                    >
                      <Plus className="h-3.5 w-3.5" />
                    </button>
                    <button
                      type="button"
                      aria-label={t("Xoá khỏi giỏ", "Remove from cart")}
                      onClick={() => remove(it.id)}
                      className="ml-auto rounded-sm p-1 text-muted-foreground hover:text-destructive"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {items.length > 0 && (
          <div className="space-y-4 border-t border-border p-4">
            <div className="flex items-center justify-between">
              <span className="text-sm text-muted-foreground">{t("Tạm tính", "Subtotal")}</span>
              <span className="font-display text-xl text-primary">{formatVnd(total)}</span>
            </div>
            {hasQuoteItem && (
              <p className="text-xs text-muted-foreground">
                {t(
                  "Một số sản phẩm cần báo giá riêng — chưa tính vào tạm tính.",
                  "Some items are quoted on request and are not included in the subtotal.",
                )}
              </p>
            )}
            <button
              type="button"
              onClick={() => {
                toast.success(
                  t(
                    "Đã ghi nhận đơn — chúng tôi sẽ liên hệ trong 24 giờ.",
                    "Order received — we will contact you within 24 hours.",
                  ),
                );
                clear();
                setOpen(false);
              }}
              className="w-full rounded-sm bg-primary px-6 py-3.5 text-sm text-primary-foreground transition-colors hover:bg-wood"
            >
              {t("Gửi Yêu Cầu Đặt Hàng", "Send Order Request")}
            </button>
            <button
              type="button"
              onClick={clear}
              className="w-full rounded-sm border border-border px-6 py-2.5 text-sm text-muted-foreground transition-colors hover:border-primary hover:text-primary"
            >
              {t("Xoá toàn bộ giỏ", "Clear cart")}
            </button>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
