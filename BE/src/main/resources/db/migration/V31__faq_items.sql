CREATE TABLE faq_items (
    id bigint PRIMARY KEY DEFAULT nextval('app_global_id_seq'),
    question_vi varchar(500) NOT NULL,
    answer_vi varchar(5000) NOT NULL,
    question_en varchar(500) NOT NULL,
    answer_en varchar(5000) NOT NULL,
    position integer NOT NULL,
    visible boolean NOT NULL DEFAULT true,
    updated_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO faq_items(question_vi,answer_vi,question_en,answer_en,position)
SELECT
    COALESCE(content_json #>> ARRAY['pages','faq','question' || n,'vi'], defaults.question_vi),
    COALESCE(content_json #>> ARRAY['pages','faq','answer' || n,'vi'], defaults.answer_vi),
    COALESCE(content_json #>> ARRAY['pages','faq','question' || n,'en'], defaults.question_en),
    COALESCE(content_json #>> ARRAY['pages','faq','answer' || n,'en'], defaults.answer_en),
    n
FROM (VALUES
    (1, 'Gốm Bàu Trúc có dùng bàn xoay không?', 'Không. Sản phẩm được tạo hình bằng tay theo kỹ thuật truyền thống của người Chăm.', 'Is Bau Truc pottery made with a wheel?', 'No. Products are shaped by hand using traditional Cham techniques.'),
    (2, 'Tôi có thể đặt lịch workshop không?', 'Có. Mở trang Workshop, chọn gói phù hợp và gửi yêu cầu đặt lịch để cửa hàng xác nhận.', 'Can I book a workshop?', 'Yes. Open Workshop, choose an offering, and submit a booking request for store confirmation.'),
    (3, 'Thanh toán online hoạt động thế nào?', 'Hệ thống tạo Payment trạng thái PENDING và hiển thị thông tin payOS nếu cổng thanh toán đã được cấu hình.', 'How does online payment work?', 'The system creates a PENDING payment and shows payOS details when the gateway is configured.')
) AS defaults(n,question_vi,answer_vi,question_en,answer_en)
LEFT JOIN site_presentation ON site_presentation.id = 1;

INSERT INTO faq_items(question_vi,answer_vi,question_en,answer_en,position)
VALUES
('Gốm Bàu Trúc có tráng men không?', 'Không tráng men. Gốm được nung lộ thiên bằng củi, rơm, trấu nên có màu đất đỏ, nâu và các mảng khói đen tự nhiên; mỗi món một vẻ.', 'Is Bàu Trúc pottery glazed?', 'No. It is fired outdoors with wood, straw and rice husks, creating natural red, brown and black smoke patterns.' ,4),
('Làng gốm Bàu Trúc ở đâu?', 'Làng nằm ven Quốc lộ 1A, cách trung tâm Phan Rang khoảng 10 km về phía Nam, nay thuộc xã Ninh Phước, tỉnh Khánh Hòa (trước thuộc Ninh Thuận). Xưởng Đàng Xem ở số 35 đường Bàu Trúc.', 'Where is Bàu Trúc pottery village?', 'The village is near National Route 1A, about 10 km south of Phan Rang, in Ninh Phước commune, Khánh Hòa province. Đàng Xem workshop is at 35 Bàu Trúc Street.',5),
('Xưởng mở cửa giờ nào?', '7:00–17:00 mỗi ngày. Muốn làm gốm hoặc đi nhóm, nên gọi/Zalo trước để xưởng chuẩn bị.', 'What are the workshop opening hours?', 'The workshop is open daily from 7:00 to 17:00. Please contact us before coming for pottery activities or with a group.',6),
('Trẻ em có tham gia được không?', 'Được. Trẻ nhỏ nên làm cùng phụ huynh; nhóm học sinh vui lòng báo trước để xưởng sắp xếp nghệ nhân.', 'Can children participate?', 'Yes. Young children should join with a parent. Please notify the workshop in advance about student groups.',7),
('Vì sao mỗi sản phẩm có màu hơi khác ảnh?', 'Vì gốm nung lộ thiên, lửa không đều nên màu và vân khói của từng chiếc khác nhau. Đó là dấu hiệu của gốm Bàu Trúc làm tay thật.', 'Why does each item look slightly different from its photo?', 'Outdoor firing produces different colours and smoke patterns on every piece. This is characteristic of handmade Bàu Trúc pottery.',8),
('Bảo quản gốm Bàu Trúc thế nào?', 'Lau bằng khăn ẩm, tránh va đập mạnh. Gốm không men nên hút nước nhẹ; nếu dùng cắm hoa tươi, nên đặt ống nhựa hoặc lọ thủy tinh bên trong.', 'How should I care for Bàu Trúc pottery?', 'Wipe with a damp cloth and avoid impacts. Unglazed pottery absorbs some water, so use a plastic or glass insert for fresh flowers.',9);
