-- Update only untouched legacy copy; preserve merchant-edited page fields and styling.
UPDATE site_presentation SET content_json = jsonb_set(jsonb_set(content_json,
    '{pages,policy,paymentBody,vi}', to_jsonb('Thanh toán online qua payOS (chuyển khoản/QR) ngay khi đặt hàng. Đơn được xác nhận khi xưởng nhận được tiền.'::text)),
    '{pages,policy,paymentBody,en}', to_jsonb('Pay online via payOS (bank transfer/QR) when placing an order. The order is confirmed when the workshop receives payment.'::text)), updated_at=now()
WHERE id=1 AND content_json #>> '{pages,policy,paymentBody,vi}' = 'Đơn hàng sử dụng thanh toán payOS. Ứng dụng không tự đánh dấu thanh toán thành công; trạng thái được cập nhật từ hệ thống backend.';

UPDATE site_presentation SET content_json = jsonb_set(jsonb_set(content_json,
    '{pages,policy,shippingBody,vi}', to_jsonb('Gốm được bọc nhiều lớp chống sốc. Xưởng báo phí giao hàng trước khi gửi.'::text)),
    '{pages,policy,shippingBody,en}', to_jsonb('Pottery is wrapped in multiple protective layers. The workshop confirms the shipping fee before dispatch.'::text)), updated_at=now()
WHERE id=1 AND content_json #>> '{pages,policy,shippingBody,vi}' = 'Phí vận chuyển và theo dõi vận chuyển chưa thuộc phạm vi hệ thống hiện tại.';

UPDATE site_presentation SET content_json = jsonb_set(jsonb_set(jsonb_set(jsonb_set(content_json,
    '{pages,policy,refundTitle,vi}', to_jsonb('Đổi trả và hoàn tiền'::text)),
    '{pages,policy,refundTitle,en}', to_jsonb('Returns and refunds'::text)),
    '{pages,policy,refundBody,vi}', to_jsonb('Nếu gốm vỡ hoặc nứt khi nhận, vui lòng quay video lúc mở hàng và báo xưởng. Xưởng gửi sản phẩm tương tự nếu còn, hoặc hoàn tiền 100% giá sản phẩm. Khác biệt nhỏ về màu sắc, vân khói là đặc trưng của gốm nung lộ thiên và không thuộc trường hợp đổi trả.'::text)),
    '{pages,policy,refundBody,en}', to_jsonb('If pottery arrives broken or cracked, please record an unboxing video and contact the workshop. We will send a similar item if available, or refund the product price. Small color and smoke-pattern differences are natural features of open-fired pottery and are not grounds for return.'::text)), updated_at=now()
WHERE id=1 AND content_json #>> '{pages,policy,refundBody,vi}' = 'Hoàn tiền tự động ngoài phạm vi. Khi cần, admin chỉ ghi nhận khoản hoàn tiền đã thực hiện bên ngoài hệ thống.';

UPDATE site_presentation SET content_json = jsonb_set(jsonb_set(content_json,
    '{pages,policy,experienceTitle}', jsonb_build_object('vi','Đặt lịch trải nghiệm','en','Workshop booking')),
    '{pages,policy,experienceBody}', jsonb_build_object('vi','Đặt trước qua biểu mẫu hoặc Zalo. Nếu cần đổi hoặc hủy lịch, vui lòng liên hệ xưởng.','en','Book in advance through the form or Zalo. Contact the workshop to change or cancel a booking.')), updated_at=now()
WHERE id=1 AND content_json #> '{pages,policy,experienceTitle}' IS NULL;
