import { createFileRoute } from "@tanstack/react-router";
import { Clock, Download, Facebook, Mail, MapPin, Phone, ShoppingBag, Music2 } from "lucide-react";
import { toast } from "sonner";
import { Reveal } from "@/components/Reveal";
import { PageHero, SectionTitle } from "@/components/PageHero";
import { useI18n } from "@/lib/i18n";

export const Route = createFileRoute("/lien-he")({
  head: () => ({
    meta: [
      { title: "Liên Hệ — Gốm Bàu Trúc Đàng Xem | Khánh Hòa" },
      {
        name: "description",
        content:
          "Liên hệ xưởng gốm Chăm Đàng Xem tại làng Bàu Trúc, xã Ninh Phước, Khánh Hòa. Đặt hàng, đặt lịch workshop hoặc hợp tác B2B với resort và doanh nghiệp.",
      },
      { property: "og:title", content: "Liên Hệ — Gốm Bàu Trúc Đàng Xem" },
      {
        property: "og:description",
        content: "Kết nối với xưởng gốm Chăm Đàng Xem — đặt hàng, workshop và hợp tác B2B.",
      },
    ],
  }),
  component: Contact,
});

function Contact() {
  const { t } = useI18n();

  return (
    <main>
      <PageHero
        title={t("Kết Nối Với Chúng Tôi", "Get in Touch")}
        subtitle={t(
          "Xưởng mở cửa mỗi ngày — ghé thăm, đặt hàng hoặc hẹn lịch trải nghiệm.",
          "Our studio is open daily — visit us, place an order or book a workshop.",
        )}
        seed="contact-hero"
      />

      <section className="mx-auto max-w-7xl px-5 py-24 lg:px-8">
        <div className="grid gap-16 lg:grid-cols-2">
          <Reveal>
            <SectionTitle eyebrow={t("Thông tin", "Information")}>
              {t("Xưởng Gốm Đàng Xem", "Đàng Xem Pottery Studio")}
            </SectionTitle>
            <ul className="space-y-6">
              {[
                {
                  Icon: MapPin,
                  label: t("Địa chỉ", "Address"),
                  value: t(
                    "Làng Bàu Trúc, xã Ninh Phước, Khánh Hòa",
                    "Bàu Trúc Village, Ninh Phước, Khánh Hòa",
                  ),
                },
                { Icon: Phone, label: t("Điện thoại", "Phone"), value: "+84 (0) 000 000 000" },
                { Icon: Mail, label: "Email", value: "lienhe.dangxem@gmail.com" },
                {
                  Icon: Clock,
                  label: t("Giờ mở cửa", "Opening hours"),
                  value: t("7:00 – 17:00 hàng ngày", "7:00 – 17:00 daily"),
                },
              ].map(({ Icon, label, value }) => (
                <li key={label} className="flex gap-4">
                  <Icon className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
                  <div className="min-w-0">
                    <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">{label}</p>
                    <p className="mt-1 text-base">{value}</p>
                  </div>
                </li>
              ))}
            </ul>
            <div className="mt-10 flex gap-3">
              {[
                { Icon: Facebook, label: "Facebook" },
                { Icon: Music2, label: "TikTok" },
                { Icon: ShoppingBag, label: "Shopee" },
              ].map(({ Icon, label }) => (
                <a
                  key={label}
                  href="#"
                  aria-label={label}
                  className="rounded-sm border border-primary/40 p-2.5 text-primary transition-colors hover:bg-primary hover:text-primary-foreground"
                >
                  <Icon className="h-4 w-4" />
                </a>
              ))}
            </div>
          </Reveal>

          <Reveal delay={120}>
            <SectionTitle eyebrow={t("Gửi tin", "Message")}>
              {t("Biểu Mẫu Liên Hệ", "Contact Form")}
            </SectionTitle>
            <form
              className="space-y-5"
              onSubmit={(e) => {
                e.preventDefault();
                toast.success(
                  t("Đã gửi tin nhắn — cảm ơn bạn đã liên hệ!", "Message sent — thank you for reaching out!"),
                );
              }}
            >
              <div className="grid gap-5 sm:grid-cols-2">
                <Field label={t("Họ tên", "Full name")} name="name" required />
                <Field label="Email" name="email" type="email" required />
                <Field label={t("Số điện thoại", "Phone number")} name="phone" type="tel" />
                <label className="block">
                  <span className="text-sm text-foreground/80">{t("Chủ đề", "Topic")}</span>
                  <select
                    name="topic"
                    className="mt-2 w-full rounded-sm border border-input bg-background px-4 py-3 text-sm outline-none focus:border-primary"
                  >
                    <option>{t("Đặt hàng", "Place an order")}</option>
                    <option>{t("Đặt lịch workshop", "Book a workshop")}</option>
                    <option>{t("Hợp tác B2B", "B2B partnership")}</option>
                    <option>{t("Khác", "Other")}</option>
                  </select>
                </label>
              </div>
              <label className="block">
                <span className="text-sm text-foreground/80">{t("Tin nhắn", "Message")}</span>
                <textarea
                  name="message"
                  rows={5}
                  required
                  className="mt-2 w-full rounded-sm border border-input bg-background px-4 py-3 text-sm outline-none focus:border-primary"
                />
              </label>
              <button
                type="submit"
                className="rounded-sm bg-primary px-7 py-3.5 text-sm text-primary-foreground transition-colors hover:bg-wood"
              >
                {t("Gửi Tin Nhắn", "Send Message")}
              </button>
            </form>
          </Reveal>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-5 pb-24 lg:px-8">
        <Reveal>
          <div className="overflow-hidden rounded-sm border border-primary/30">
            <iframe
              title={t("Bản đồ làng Bàu Trúc, Ninh Phước", "Map of Bàu Trúc village, Ninh Phước")}
              src="https://www.openstreetmap.org/export/embed.html?bbox=108.90%2C11.50%2C109.10%2C11.65&layer=mapnik"
              className="h-[380px] w-full"
              loading="lazy"
            />
          </div>
        </Reveal>
      </section>

      <section className="bg-card">
        <div className="mx-auto max-w-3xl px-5 py-24 text-center lg:px-8">
          <Reveal>
            <h2 className="font-display text-3xl sm:text-4xl">
              {t("Dành Cho Đối Tác & Resort", "For Partners & Resorts")}
            </h2>
            <p className="mt-6 text-base leading-loose text-muted-foreground">
              {t(
                "Chúng tôi cung cấp sản phẩm gốm độc bản cho khách sạn, resort và doanh nghiệp. Liên hệ để nhận báo giá sỉ và catalog sản phẩm.",
                "We supply one-of-a-kind pottery to hotels, resorts and businesses. Contact us for wholesale pricing and our catalogue.",
              )}
            </p>
            <button
              type="button"
              onClick={() =>
                toast(
                  t(
                    "Catalog B2B sẽ sớm được cập nhật — vui lòng liên hệ qua email.",
                    "The B2B catalogue is coming soon — please reach us by email.",
                  ),
                )
              }
              className="mt-10 inline-flex items-center gap-2 rounded-sm bg-primary px-7 py-3.5 text-sm text-primary-foreground transition-colors hover:bg-wood"
            >
              <Download className="h-4 w-4" /> {t("Tải Catalog B2B", "Download B2B Catalogue")}
            </button>
          </Reveal>
        </div>
      </section>
    </main>
  );
}

function Field({
  label,
  name,
  type = "text",
  required,
}: {
  label: string;
  name: string;
  type?: string;
  required?: boolean;
}) {
  return (
    <label className="block">
      <span className="text-sm text-foreground/80">{label}</span>
      <input
        name={name}
        type={type}
        required={required}
        className="mt-2 w-full rounded-sm border border-input bg-background px-4 py-3 text-sm outline-none focus:border-primary"
      />
    </label>
  );
}
