CREATE TABLE seo_metadata (
    path varchar(100) PRIMARY KEY,
    title varchar(180),
    description varchar(500),
    og_title varchar(180),
    og_description varchar(500),
    image_slot varchar(64),
    updated_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO seo_metadata(path,title,description) VALUES
('/', 'Gốm Bàu Trúc thủ công | Đàng Xem – Làng gốm Chăm Ninh Thuận', 'Gốm Chăm Bàu Trúc làm tay, nung lộ thiên – di sản UNESCO. Mua gốm thủ công hoặc đặt lịch trải nghiệm tại xưởng Đàng Xem, Khánh Hòa.'),
('/san-pham', 'Mua gốm Bàu Trúc thủ công, giao toàn quốc | Đàng Xem', 'Bình, tượng, đèn và tiểu cảnh gốm Chăm làm tay, nung lộ thiên, mỗi món một vẻ. Ảnh chụp sản phẩm thật, đóng gói kỹ, giao toàn quốc.'),
('/trai-nghiem', 'Trải nghiệm làm gốm Bàu Trúc tại Ninh Thuận | Đàng Xem', 'Buổi làm gốm cùng nghệ nhân được cố Nghệ nhân Ưu tú Đàng Xem truyền nghề: nặn tay không bàn xoay, nhóm nhỏ, mang sản phẩm về. Đặt lịch tại Bàu Trúc.'),
('/ve-chung-toi', 'Cố Nghệ nhân Ưu tú Đàng Xem – Xưởng gốm Bàu Trúc từ 2009', 'Xưởng gốm Bàu Trúc do cố Nghệ nhân Ưu tú Đàng Xem sáng lập năm 2009, nay do con gái ông và các nghệ nhân ông truyền nghề tiếp nối.'),
('/lien-he', 'Liên hệ & đường đi xưởng gốm Bàu Trúc Đàng Xem', 'Xưởng Đàng Xem, 35 Bàu Trúc, Ninh Phước, Khánh Hòa. Mở cửa 7:00–17:00 mỗi ngày. Gọi/Zalo 0343 478 155 để đặt hàng hoặc hẹn lịch.'),
('/faq', 'Gốm Bàu Trúc có dùng bàn xoay không? Hỏi đáp | Đàng Xem', 'Giải đáp về gốm Bàu Trúc: có dùng bàn xoay không, cách đặt lịch trải nghiệm, thanh toán, giao hàng, bảo quản và đổi trả tại Đàng Xem.'),
('/chinh-sach', 'Chính sách đổi trả, thanh toán, vận chuyển | Đàng Xem', 'Chính sách thanh toán, vận chuyển và đổi trả khi mua gốm Bàu Trúc thủ công tại Đàng Xem, viết rõ ràng, dễ hiểu.'),
('/blog', 'Câu chuyện gốm Chăm Bàu Trúc – Blog | Đàng Xem', 'Kiến thức, câu chuyện và kinh nghiệm về gốm Chăm Bàu Trúc: làng nghề, quy trình, di sản UNESCO, mùa lễ hội và cách chọn gốm thật.'),
('/san-pham/binh-gom-trang-tri', 'Bình gốm Bàu Trúc trang trí làm tay | Đàng Xem', 'Bình, lọ, ống cắm hoa gốm Chăm Bàu Trúc nặn tay, nung lộ thiên, mỗi chiếc một màu đất và vân khói riêng. Giao toàn quốc.'),
('/san-pham/tuong-gom-nghe-thuat', 'Tượng gốm Chăm, tượng vũ nữ Chăm làm tay | Đàng Xem', 'Tượng vũ nữ, tượng người và linh vật gốm Chăm do nghệ nhân xưởng Đàng Xem nặn tay, nung lộ thiên. Giao toàn quốc.'),
('/san-pham/phu-dieu-mat-na-gom', 'Phù điêu, mặt nạ gốm Chăm treo tường | Đàng Xem', 'Phù điêu và mặt nạ gốm Chăm chạm nổi cho tường phòng khách, sảnh khách sạn. Nặn tay, nung lộ thiên tại Bàu Trúc.'),
('/san-pham/mo-hinh-thap-cham', 'Mô hình tháp Chăm bằng gốm Bàu Trúc | Đàng Xem', 'Mô hình tháp Chăm bằng gốm đất nung, lấy cảm hứng từ tháp Po Klong Garai, làm tay tại làng gốm Bàu Trúc. Quà lưu niệm và trang trí.'),
('/san-pham/tieu-canh-gom-san-vuon', 'Đèn gốm sân vườn, đài phun nước gốm Bàu Trúc', 'Đèn gốm đất nung, đài phun nước và chậu gốm trồng sen đá làm tay tại làng Bàu Trúc cho sân vườn và tiểu cảnh.'),
('/san-pham/do-gom-gia-dung', 'Đồ gốm gia dụng Bàu Trúc làm tay | Đàng Xem', 'Chum, hũ và đồ gốm dùng hằng ngày nặn tay theo lối gốm Chăm Bàu Trúc, nung lộ thiên. Giao toàn quốc.');
