import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { toast } from "sonner";
import { Reveal } from "@/components/Reveal";
import { PageHero, SectionTitle } from "@/components/PageHero";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { useI18n } from "@/lib/i18n";
import wsCouple from "@/assets/ws-couple.jpg";
import wsFamily from "@/assets/ws-family.jpg";
import wsTour from "@/assets/ws-tour.jpg";

export const Route = createFileRoute("/trai-nghiem")({
  head: () => ({
    meta: [
      { title: "Workshop Trải Nghiệm Làm Gốm Chăm | Đàng Xem, Khánh Hòa" },
      {
        name: "description",
        content:
          "Tự tay nặn gốm cùng nghệ nhân Chăm tại xưởng Đàng Xem, làng Bàu Trúc. Workshop cho cá nhân, gia đình và đoàn tour — đặt lịch trong 24 giờ.",
      },
      { property: "og:title", content: "Workshop Trải Nghiệm Làm Gốm Chăm | Đàng Xem" },
      {
        property: "og:description",
        content: "Chạm tay vào lịch sử — workshop làm gốm thủ công Chăm tại Khánh Hòa.",
      },
    ],
  }),
  component: Experience,
});

const STEPS = [
  {
    n: "01",
    vi: "Tìm hiểu lịch sử gốm Chăm",
    en: "Learn about Cham pottery heritage",
    dVi: "Nghe nghệ nhân kể về làng Bàu Trúc, đất Nu Lanh và kỹ thuật nung lộ thiên.",
    dEn: "Hear the artisan tell of Bàu Trúc village, Nu Lanh clay and open-air firing.",
  },
  {
    n: "02",
    vi: "Tự tay nặn và tạo hình",
    en: "Shape your own piece",
    dVi: "Không bàn xoay — bạn đi vòng quanh khối đất theo đúng cách người Chăm làm.",
    dEn: "No wheel — you walk around the clay exactly as the Cham people do.",
  },
  {
    n: "03",
    vi: "Mang về kỷ niệm độc bản",
    en: "Take home your creation",
    dVi: "Sản phẩm được hong khô, hoàn thiện và gửi về tận tay bạn.",
    dEn: "Your piece is dried, finished and delivered to you.",
  },
];

const TYPES = [
  {
    vi: "Cá Nhân & Cặp Đôi",
    en: "Individual & Couple",
    mVi: "1–2 người · ~2 giờ",
    mEn: "1–2 people · ~2 hours",
    img: wsCouple,
  },
  {
    vi: "Gia Đình & Nhóm",
    en: "Family & Group",
    mVi: "3–10 người · ~2–3 giờ",
    mEn: "3–10 people · ~2–3 hours",
    img: wsFamily,
  },
  {
    vi: "Đoàn Tour",
    en: "Tour Groups",
    mVi: "10+ người · liên hệ để báo giá",
    mEn: "10+ people · contact us for a quote",
    img: wsTour,
  },
];

const FAQS = [
  {
    qVi: "Workshop kéo dài bao lâu?",
    qEn: "How long does the workshop last?",
    aVi: "Buổi trải nghiệm tiêu chuẩn kéo dài khoảng 2 giờ, gồm phần giới thiệu, thực hành nặn gốm và hoàn thiện sản phẩm. Đoàn đông có thể kéo dài đến 3 giờ.",
    aEn: "A standard session runs about 2 hours: introduction, hands-on shaping and finishing. Larger groups may take up to 3 hours.",
  },
  {
    qVi: "Có cần kinh nghiệm làm gốm không?",
    qEn: "Do I need any pottery experience?",
    aVi: "Hoàn toàn không. Nghệ nhân hướng dẫn từng bước từ nhồi đất đến tạo hình, phù hợp cả với người lần đầu chạm vào đất sét.",
    aEn: "Not at all. The artisan guides every step from kneading to shaping — perfect for first-timers.",
  },
  {
    qVi: "Phù hợp với trẻ em không?",
    qEn: "Is it suitable for children?",
    aVi: "Rất phù hợp với trẻ từ 6 tuổi trở lên. Trẻ nhỏ hơn vẫn tham gia được khi có người lớn đi kèm.",
    aEn: "Great for children aged 6 and up. Younger children are welcome with an adult.",
  },
  {
    qVi: "Sản phẩm có được mang về không?",
    qEn: "Can I keep what I make?",
    aVi: "Có. Sản phẩm của bạn được hong khô và nung; bạn có thể nhận trực tiếp sau khi hoàn thiện hoặc chúng tôi gửi chuyển phát về tận nơi.",
    aEn: "Yes. Your piece is dried and fired; collect it in person or we can ship it to you.",
  },
];

function Experience() {
  const { t } = useI18n();
  const [sent, setSent] = useState(false);

  return (
    <main>
      <PageHero
        title={t("Chạm Tay Vào Lịch Sử", "Touch History with Your Hands")}
        subtitle={t(
          "Workshop làm gốm thủ công Chăm tại xưởng Đàng Xem, Khánh Hòa",
          "Handmade Cham pottery workshops at the Đàng Xem studio, Khánh Hòa",
        )}
        seed="experience-hero"
      />

      <section className="mx-auto max-w-7xl px-5 py-24 lg:px-8">
        <Reveal>
          <SectionTitle eyebrow={t("Bạn sẽ làm gì", "What you'll do")}>
            {t("Ba Bước Trong Một Buổi Trải Nghiệm", "Three Steps in One Session")}
          </SectionTitle>
        </Reveal>
        <div className="grid gap-10 md:grid-cols-3">
          {STEPS.map((s, i) => (
            <Reveal key={s.n} delay={i * 100}>
              <p className="font-display text-5xl text-primary/45">{s.n}</p>
              <h3 className="mt-4 font-display text-xl">{t(s.vi, s.en)}</h3>
              <p className="mt-3 text-sm leading-relaxed text-muted-foreground">{t(s.dVi, s.dEn)}</p>
            </Reveal>
          ))}
        </div>
      </section>

      <section className="bg-card">
        <div className="mx-auto max-w-7xl px-5 py-24 lg:px-8">
          <Reveal>
            <SectionTitle eyebrow={t("Hình thức", "Formats")}>
              {t("Các Gói Workshop", "Workshop Packages")}
            </SectionTitle>
          </Reveal>
          <div className="grid gap-8 md:grid-cols-3">
            {TYPES.map((ty, i) => (
              <Reveal key={ty.vi} delay={i * 100} as="article">
                <div className="h-full rounded-sm border border-border bg-background transition-shadow duration-300 hover:shadow-[0_14px_36px_rgba(44,26,14,0.16)]">
                  <div className="aspect-4/3 overflow-hidden rounded-t-sm">
                    <img
                      src={ty.img}
                      alt={t(ty.vi, ty.en)}
                      width={700}
                      height={520}
                      loading="lazy"
                      className="h-full w-full object-cover"
                    />
                  </div>
                  <div className="p-6">
                    <h3 className="font-display text-xl">{t(ty.vi, ty.en)}</h3>
                    <p className="mt-4 text-sm text-primary">{t(ty.mVi, ty.mEn)}</p>
                  </div>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* Booking form */}
      <section className="mx-auto max-w-3xl px-5 py-24 lg:px-8">
        <Reveal>
          <SectionTitle eyebrow={t("Đặt lịch", "Booking")}>
            {t("Gửi Yêu Cầu Trải Nghiệm", "Request a Workshop")}
          </SectionTitle>
          <form
            className="space-y-5"
            onSubmit={(e) => {
              e.preventDefault();
              setSent(true);
              toast.success(
                t(
                  "Đã gửi yêu cầu — chúng tôi sẽ liên hệ trong 24 giờ.",
                  "Request sent — we will contact you within 24 hours.",
                ),
              );
            }}
          >
            <div className="grid gap-5 sm:grid-cols-2">
              <Field label={t("Họ tên", "Full name")} name="name" required />
              <Field label={t("Email", "Email")} name="email" type="email" required />
              <Field label={t("Số điện thoại", "Phone number")} name="phone" type="tel" required />
              <Field label={t("Ngày mong muốn", "Preferred date")} name="date" type="date" required />
              <Field label={t("Số người", "Number of people")} name="people" type="number" required />
            </div>
            <label className="block">
              <span className="text-sm text-foreground/80">{t("Ghi chú", "Notes")}</span>
              <textarea
                name="note"
                rows={4}
                className="mt-2 w-full rounded-sm border border-input bg-background px-4 py-3 text-sm outline-none focus:border-primary"
              />
            </label>
            <button
              type="submit"
              className="rounded-sm bg-primary px-7 py-3.5 text-sm text-primary-foreground transition-colors hover:bg-wood"
            >
              {t("Gửi Yêu Cầu Đặt Lịch", "Send Booking Request")}
            </button>
            <p className="text-sm text-muted-foreground">
              {sent
                ? t("Cảm ơn bạn! Yêu cầu đã được ghi nhận.", "Thank you! Your request has been received.")
                : t(
                    "Chúng tôi sẽ liên hệ xác nhận trong vòng 24 giờ.",
                    "We will confirm with you within 24 hours.",
                  )}
            </p>
          </form>
        </Reveal>
      </section>

      <section className="mx-auto max-w-3xl px-5 pb-24 lg:px-8">
        <Reveal>
          <SectionTitle eyebrow={t("Hỏi đáp", "FAQ")}>
            {t("Câu Hỏi Thường Gặp", "Frequently Asked Questions")}
          </SectionTitle>
          <Accordion type="single" collapsible className="w-full">
            {FAQS.map((f, i) => (
              <AccordionItem key={f.qVi} value={`i${i}`}>
                <AccordionTrigger className="text-left font-display text-lg">
                  {t(f.qVi, f.qEn)}
                </AccordionTrigger>
                <AccordionContent className="text-sm leading-relaxed text-muted-foreground">
                  {t(f.aVi, f.aEn)}
                </AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        </Reveal>
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
