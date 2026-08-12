# SOFTWARE REQUIREMENTS SPECIFICATION

## HỆ THỐNG WEBSITE THƯƠNG MẠI ĐIỆN TỬ GỐM BÀU TRÚC

**Document ID:** `02\\\_SRS\\\_SPEC`
**Version:** `1.3`
**Status:** `BASELINE / FROZEN`
**System Type:** E-Commerce Web Application
**Backend:** Spring Boot
**Frontend:** Next.js
**Database:** PostgreSQL
**Deployment:** AWS
**Payment Gateway:** payOS
**Image Storage:** Amazon S3

\---

# 1\. GIỚI THIỆU

## 1.1. Mục đích tài liệu

Tài liệu Software Requirements Specification (SRS) mô tả đầy đủ các yêu cầu chức năng, yêu cầu phi chức năng, quy tắc nghiệp vụ, yêu cầu dữ liệu, phân quyền, validation, transaction consistency, security, testing và acceptance criteria cho hệ thống website thương mại điện tử Gốm Bàu Trúc.

Tài liệu là cơ sở cho:

* System Architecture Design;
* Database Design;
* REST API Design;
* Backend Implementation bằng Spring Boot;
* Frontend Implementation bằng Next.js;
* payOS Integration;
* AWS Deployment;
* Testing \& QA;
* Project Planning.

SRS tập trung mô tả:

* hệ thống phải làm gì;
* business rule nào phải được bảo đảm;
* dữ liệu nào cần tồn tại;
* actor nào được thực hiện chức năng;
* các constraint và quality requirement phải đáp ứng.

Các quyết định implementation chi tiết như:

* Java package structure;
* Controller/Service/Repository cụ thể;
* locking strategy cụ thể;
* transaction boundary chi tiết với external API;
* idempotency storage cụ thể;
* scheduler implementation;
* timestamp persistence strategy;
* detailed AWS Infrastructure as Code;
* task assignment;

không thuộc phạm vi SRS và sẽ được mô tả trong:

```text

03\\\_SYSTEM\\\_ARCHITECTURE

04\\\_IMPLEMENTATION\\\_SPEC

05\\\_PROJECT\\\_PLAN

```

\---

# 2\. PHẠM VI HỆ THỐNG

Hệ thống là website thương mại điện tử của **một cửa hàng duy nhất**, phục vụ việc giới thiệu và bán các sản phẩm gốm Bàu Trúc.

Hệ thống không phải nền tảng nhiều nhà bán hàng hoặc nhiều cửa hàng.

Hệ thống gồm hai khu vực giao diện chính:

1. Website khách hàng.
2. Website quản trị.

\---

# 3\. ACTORS

Hệ thống có các actor:

```text

GUEST

USER

ADMIN

```

Application role chỉ gồm:

```text

USER

ADMIN

```

\---

## 3.1. GUEST

GUEST là người chưa đăng nhập.

GUEST được phép:

* xem trang chủ;
* xem danh sách sản phẩm;
* xem chi tiết sản phẩm;
* tìm kiếm sản phẩm;
* lọc sản phẩm;
* sắp xếp sản phẩm;
* xem Collection public;
* xem FAQ;
* xem Policy;
* xem Contact;
* chuyển đổi giao diện tiếng Việt/tiếng Anh.

GUEST không được:

* quản lý Cart của tài khoản;
* checkout;
* tạo Order;
* xem Order cá nhân;
* truy cập chức năng quản trị.

\---

## 3.2. USER

USER là người đã đăng nhập hợp lệ bằng Google.

USER có thể:

* sử dụng các chức năng public;
* xem thông tin tài khoản hiện tại;
* quản lý Cart của chính mình;
* checkout;
* tạo Order;
* thanh toán qua payOS;
* xem danh sách Order của chính mình;
* xem chi tiết Order của chính mình;
* xem trạng thái hiện tại của Order.

USER không được:

* tự thay đổi role;
* tự promote thành ADMIN;
* quản lý User khác;
* truy cập chức năng ADMIN;
* thay đổi Order Status;
* tự hủy Order;
* tự cập nhật Payment Status;
* tự ghi nhận refund;
* chỉnh sửa Google identity thông qua application.

\---

## 3.3. ADMIN

ADMIN là User có:

```text

role = ADMIN

AND

status = ACTIVE

```

theo dữ liệu authoritative trong database.

ADMIN có thể:

* quản lý User;
* promote USER thành ADMIN;
* demote ADMIN thành USER;
* block/unblock User;
* quản lý Product;
* quản lý Collection;
* quản lý Simple Product Discount;
* quản lý Product Image;
* quản lý Inventory;
* xem Inventory Transaction History;
* xem toàn bộ Order;
* tìm kiếm và lọc Order;
* cập nhật Order Status;
* hủy Order theo business rule;
* xem Notification;
* quản lý read/unread state Notification của chính mình;
* xem Dashboard;
* xem Revenue;
* xem Best Selling Products;
* ghi nhận Manual Refund sau khi refund thực tế đã được thực hiện bên ngoài application.

\---

# 4\. MỤC TIÊU HỆ THỐNG

Hệ thống phải hỗ trợ tối thiểu:

* Google Login;
* JWT Authentication;
* Role-based Authorization;
* Admin Bootstrap;
* Initial ADMIN Bootstrap Validation;
* Admin User Management;
* Product Management;
* Product Search;
* Product Filter;
* Product Pagination;
* Product Sorting;
* Collection Management;
* Simple Product Discount;
* Product Image Management;
* Amazon S3 Image Storage;
* Shopping Cart;
* Cart Post-checkout Lifecycle;
* Inventory Management;
* Inventory Reservation;
* Inventory Transaction History;
* Inventory Audit;
* Concurrency-safe Checkout;
* Order Management;
* Order Payment Preconditions;
* Checkout Idempotency;
* payOS Payment;
* Payment Expiration;
* Payment Creation Failure Handling;
* Payment Webhook Verification;
* Payment Idempotency;
* Manual Refund Recording;
* Multi-ADMIN Notification;
* Notification Trigger Semantics;
* Revenue Reporting;
* Best Selling Product Reporting;
* Low Stock Monitoring;
* Business Timezone Consistency;
* Vietnamese/English UI;
* Static FAQ;
* Static Policy;
* Contact Information;
* Responsive UI;
* AWS Production Deployment;
* Application Logging.

\---

# 5\. SYSTEM CONSTRAINTS

## 5.1. Technology Constraints

Backend:

```text

Spring Boot

```

Frontend:

```text

Next.js

```

Relational Database:

```text

PostgreSQL

```

Production Deployment:

```text

AWS

```

Product Image Storage:

```text

Amazon S3

```

Payment Gateway:

```text

payOS

```

\---

## 5.2. Architecture Constraint

Hệ thống được phát triển dưới dạng E-Commerce Web Application.

Application architecture ở mức hiện tại:

```text

Modular Monolith

```

Chi tiết component/module boundary được xác định trong:

```text

03\\\_SYSTEM\\\_ARCHITECTURE

```

\---

# 6\. BUSINESS TIME REQUIREMENTS

## BR-TIME-001 – Business Timezone

Business timezone của hệ thống là:

```text

Asia/Ho\\\_Chi\\\_Minh

UTC+07:00

```

Tất cả business-facing date/time phải được diễn giải nhất quán theo timezone:

```text

Asia/Ho\\\_Chi\\\_Minh

```

Rule này áp dụng tối thiểu cho:

* Discount `startAt`;
* Discount `endAt`;
* đánh giá Discount effective;
* Order `createdAt` khi hiển thị;
* Payment expiration khi hiển thị;
* Dashboard date filter;
* Revenue report date range;
* Recent Orders display;
* administrative timestamps hiển thị cho USER/ADMIN.

Business timezone không bắt buộc database phải lưu local timestamp.

Implementation có thể sử dụng:

```text

store UTC

→ convert to Asia/Ho\\\_Chi\\\_Minh when needed

```

hoặc representation phù hợp khác.

Exact persistence và time conversion strategy:

```text

To be defined in Implementation Specification.

```

Cùng một timestamp phải có interpretation nhất quán giữa:

```text

Frontend

Backend

Database

Reporting

Discount Calculation

Payment Expiration

```

Frontend local clock không phải authoritative source cho business time calculation.

Backend phải là authoritative source đối với:

* Discount activation/effectiveness;
* Payment expiration;
* business date-range interpretation cần thiết cho reporting.

\---

# 7\. AUTHENTICATION \& AUTHORIZATION

## FR-AUTH-001 – Google Login

Hệ thống phải cho phép USER đăng nhập bằng Google Account.

Hệ thống không cung cấp username/password login.

Flow logic:

```text

USER

↓

Google Authentication

↓

Frontend nhận Google credential

↓

Backend verify Google credential

↓

Find User

↓

Create User nếu chưa tồn tại

↓

Determine Initial Role

↓

Issue JWT

```

Backend phải verify Google credential trước khi tin tưởng:

* Google user identifier;
* email;
* identity của người đăng nhập.

Frontend không được xem là security boundary.

\---

## FR-AUTH-002 – Automatic User Creation

Nếu Google Account hợp lệ chưa tồn tại trong database, Backend phải tạo User mới.

Thông tin tối thiểu:

```text

id

googleId

email

fullName

avatarUrl

role

status

createdAt

updatedAt

```

Role ban đầu được xác định theo Admin Bootstrap Rule.

User thông thường:

```text

role = USER

```

Trạng thái ban đầu:

```text

status = ACTIVE

```

\---

## FR-AUTH-003 – JWT Issuance

Sau khi authentication thành công, Backend phải phát hành JWT.

JWT phải chứa tối thiểu:

```text

userId

email

role

issuedAt

expiration

```

JWT chỉ được cấp nếu:

* Google credential hợp lệ;
* User tồn tại hoặc tạo mới thành công;
* User không ở trạng thái `BLOCKED`.

\---

## FR-AUTH-004 – Token Validation

Đối với protected API, Backend phải xử lý các trường hợp sau.

### Valid Token

Nếu JWT:

* tồn tại;
* hợp lệ;
* chưa hết hạn;
* không bị sửa đổi;
* xác định được User hợp lệ;

request có thể tiếp tục qua authorization process.

### Expired Token

JWT hết hạn phải bị từ chối.

```text

HTTP 401 Unauthorized

```

### Invalid Token

JWT sai signature, malformed hoặc không hợp lệ phải bị từ chối.

```text

HTTP 401 Unauthorized

```

### Missing Token

Protected API được gọi mà không có token hợp lệ phải trả:

```text

HTTP 401 Unauthorized

```

### Insufficient Permission

User đã authenticated nhưng không đủ quyền phải trả:

```text

HTTP 403 Forbidden

```

\---

## FR-AUTH-005 – Runtime Authorization

Backend phải enforce authorization.

Các API ADMIN chỉ cho phép User hiện có:

```text

role = ADMIN

AND

status = ACTIVE

```

Sau bootstrap:

```text

User.role

User.status

```

trong database là runtime source of truth.

JWT có thể chứa role claim, nhưng frontend hoặc role claim phía client không được là nguồn duy nhất quyết định authorization.

Nếu role hoặc status trong database thay đổi sau khi token được phát hành, protected operation phải phản ánh authoritative state hiện tại theo security design.

Cơ chế kỹ thuật cụ thể:

```text

To be defined in Implementation Specification.

```

\---

## FR-AUTH-006 – Current User

Hệ thống phải cung cấp khả năng lấy thông tin của User đang authenticated.

Thông tin tối thiểu:

```text

id

email

fullName

avatarUrl

role

status

```

Dữ liệu profile chủ yếu có nguồn từ Google identity.

Version 1.3 không yêu cầu chức năng chỉnh sửa profile.

\---

## FR-AUTH-007 – Logout

Hệ thống sử dụng stateless JWT access-token-only theo scope hiện tại.

Logout phải:

* xóa token/session phía client;
* ngừng sử dụng token đã lưu trên client;
* đưa User về trạng thái chưa authenticated trên frontend.

Nếu không triển khai server-side token blacklist, access token đã phát hành chỉ tự hết hiệu lực khi expiration xảy ra.

Refresh Token không thuộc requirement Version 1.3.

\---

# 8\. ADMIN BOOTSTRAP \& USER MANAGEMENT

## FR-ADMIN-001 – Bootstrap Admin

Production environment phải hỗ trợ:

```text

ADMIN\\\_EMAILS

```

Ví dụ:

```text

ADMIN\\\_EMAILS=admin1@gmail.com,admin2@gmail.com

```

`ADMIN\\\_EMAILS` chỉ được sử dụng để bootstrap initial role.

Nếu một email:

* thuộc `ADMIN\\\_EMAILS`;
* chưa tồn tại trong database;
* login Google thành công lần đầu;

User được tạo với:

```text

role = ADMIN

status = ACTIVE

```

Email khác mặc định:

```text

role = USER

status = ACTIVE

```

Sau khi User đã được lưu trong database:

```text

User.role

User.status

```

là source of truth.

`ADMIN\\\_EMAILS` không được tự động re-promote User đã bị demote trong database.

Ví dụ:

```text

ADMIN\\\_EMAILS contains admin@gmail.com



admin@gmail.com first login

→ role = ADMIN



Later:

ADMIN B demotes admin@gmail.com

→ role = USER



admin@gmail.com logs in again

→ remains USER

```

\---

## FR-ADMIN-002 – User List, Search, Pagination and Detail

ADMIN phải có thể:

* xem User List;
* search User;
* pagination;
* xem User Detail.

Search tối thiểu:

```text

email

fullName

```

User List tối thiểu hiển thị:

```text

id

email

fullName

role

status

createdAt

```

\---

## FR-ADMIN-003 – Promote User

ADMIN phải có thể:

```text

USER → ADMIN

```

Business Rules:

* chỉ ADMIN được promote;
* USER không được tự promote;
* role được validate ở Backend;
* target User phải tồn tại;
* target User phải có role USER trước operation.

\---

## FR-ADMIN-004 – Demote Admin

ADMIN phải có thể:

```text

ADMIN → USER

```

Business Rules:

* hệ thống luôn phải có ít nhất một ADMIN hợp lệ trong runtime state;
* không được demote ADMIN cuối cùng.

ADMIN hợp lệ:

```text

role = ADMIN

AND

status = ACTIVE

```

Nếu operation khiến hệ thống không còn ADMIN hợp lệ, Backend phải reject.

\---

## FR-ADMIN-005 – Block User

ADMIN phải có thể:

```text

ACTIVE → BLOCKED

```

Blocked User:

* không được login để nhận JWT mới;
* không được tiếp tục protected business operation;
* role không bắt buộc thay đổi khi block.

Không được block ADMIN cuối cùng nếu operation làm hệ thống không còn ADMIN hợp lệ.

\---

## FR-ADMIN-006 – Unblock User

ADMIN phải có thể:

```text

BLOCKED → ACTIVE

```

Sau khi unblock, User có thể authentication và sử dụng quyền dựa trên role hiện tại trong database.

\---

## FR-ADMIN-007 – Initial Production Bootstrap Validation

Trước khi database đã có một ADMIN hợp lệ, initial production configuration phải có tối thiểu:

```text

1 valid Google email

```

trong:

```text

ADMIN\\\_EMAILS

```

Initial production deployment không được được xem là valid nếu đồng thời:

```text

Database chưa có ADMIN hợp lệ

AND

ADMIN\\\_EMAILS empty / không có email bootstrap hợp lệ

```

Mục tiêu của requirement này là đảm bảo hệ thống luôn có con đường thiết lập ADMIN đầu tiên để thực hiện:

```text

User Management

USER → ADMIN Promotion

ADMIN Management

```

Hệ thống không được silently operate indefinitely trong production mà không có cách thiết lập initial ADMIN.

Exact fail-fast, startup validation hoặc deployment validation mechanism:

```text

To be defined in Implementation Specification.

```

\---

## 8.1. Admin Integrity Rules

### Initial State

Trước khi database có ADMIN hợp lệ:

```text

ADMIN\\\_EMAILS phải chứa ít nhất 1 valid Google email.

```

### Runtime State

Sau khi hệ thống đã có ADMIN hợp lệ:

```text

Demote Admin

Block Admin

```

phải concurrency-safe ở mức business integrity.

Hai hoặc nhiều ADMIN operation đồng thời không được khiến hệ thống mất toàn bộ ADMIN hợp lệ.

Initial Bootstrap Validation và Last-ADMIN Protection là hai business rule độc lập và cùng phải tồn tại.

Implementation strategy:

```text

To be defined in Implementation Specification.

```

\---

# 9\. USER DATA REQUIREMENTS

User tối thiểu có:

```text

id

googleId

email

fullName

avatarUrl

role

status

createdAt

updatedAt

```

Role:

```text

USER

ADMIN

```

UserStatus:

```text

ACTIVE

BLOCKED

```

Các identifier phù hợp như `googleId` và `email` phải có uniqueness constraint phù hợp.

Exact database constraint:

```text

To be defined in Implementation Specification.

```

\---

# 10\. PRODUCT MODULE

Product không có:

```text

Variant

Size

Color

ProductVariant

Variant SKU

```

Mỗi Product là một sản phẩm độc lập.

\---

## 10.1. Product Data

Product tối thiểu có:

```text

id

nameVi

nameEn

descriptionVi

descriptionEn

basePrice

status

collectionId

createdAt

updatedAt

deletedAt

```

ProductStatus:

```text

ACTIVE

INACTIVE

```

Soft Delete sử dụng:

```text

deletedAt

```

Không sử dụng thêm `DELETED` như một ProductStatus.

\---

## FR-PRODUCT-001 – Public Product List

GUEST và USER phải có thể xem danh sách Product.

Public API chỉ trả Product thỏa:

```text

status = ACTIVE

AND

deletedAt IS NULL

```

List response phải cung cấp đủ dữ liệu để frontend hiển thị tối thiểu:

* tên phù hợp locale;
* base price;
* effective selling price;
* thumbnail nếu có;
* Collection;
* availability status.

\---

## FR-PRODUCT-002 – Product Detail

GUEST và USER phải có thể xem chi tiết Product public hợp lệ.

Product Detail phải cung cấp tối thiểu:

```text

id

nameVi

nameEn

descriptionVi

descriptionEn

basePrice

sellingPrice

effective discount information nếu có

images

collection

availableQuantity / availability status

```

Product đã soft-delete hoặc không còn public không được hiển thị như Product public thông thường.

\---

## FR-PRODUCT-003 – Product Search and Filter

Public Product List phải hỗ trợ search tối thiểu:

```text

keyword

```

Filter tối thiểu:

```text

collectionId

minPrice

maxPrice

```

Filter theo giá phải phản ánh effective selling price mà khách hàng nhìn thấy tại thời điểm request.

Backend là source of truth cho effective selling price.

\---

## FR-PRODUCT-004 – Product Pagination and Sorting

Product List phải hỗ trợ:

```text

page

size

```

Sorting tối thiểu:

```text

price

createdAt

```

Direction:

```text

ascending

descending

```

Frontend không được load toàn bộ Product database để tự thực hiện filter/pagination như cơ chế production chính.

\---

## FR-PRODUCT-005 – Admin Product List

ADMIN phải có thể:

* xem Product `ACTIVE`;
* xem Product `INACTIVE`;
* search;
* pagination;
* xem Product Detail phục vụ quản trị.

Product đã soft-delete có thể được loại khỏi list mặc định hoặc hỗ trợ filter riêng.

Exact behavior:

```text

To be defined in Implementation Specification.

```

\---

## FR-PRODUCT-006 – Create Product

ADMIN phải có thể tạo Product.

Product mới phải đáp ứng:

* required fields;
* `basePrice > 0`;
* `basePrice` phải là số nguyên VND theo `BR-MONEY-001`;
* Collection validation;
* Product status;
* Product Image rule nếu upload;
* Discount validation nếu được cấu hình.

Backend phải reject `Product.basePrice` có fractional VND và không được silently round giá trị input này.

Frontend validation có thể hỗ trợ UX nhưng Backend vẫn là authoritative validation boundary cho `Product.basePrice`.

\---

## FR-PRODUCT-007 – Update Product

ADMIN phải có thể cập nhật Product chưa bị soft-delete.

Cho phép cập nhật các dữ liệu phù hợp như:

```text

nameVi

nameEn

descriptionVi

descriptionEn

basePrice

collectionId

status

```

Thay đổi Product không được làm thay đổi OrderItem snapshot đã tồn tại.

Rule này bao gồm tối thiểu các thay đổi xảy ra sau Order creation đối với:

```text

nameVi
nameEn
basePrice
Discount
status
deletedAt

```

Các thay đổi trên không được mutate historical Product Name hoặc historical pricing đã snapshot trong OrderItem.

Nếu ADMIN cập nhật `basePrice`, giá trị mới phải thỏa:

```text

basePrice > 0

AND

basePrice là số nguyên VND theo BR-MONEY-001

```

Backend phải reject fractional `basePrice` và không được silently round Product base price. Frontend validation không thay thế Backend validation.

\---

## FR-PRODUCT-008 – Enable/Disable Product

ADMIN phải có thể:

```text

ACTIVE ↔ INACTIVE

```

Product `INACTIVE`:

* không xuất hiện trong public Product List;
* không được thêm mới vào Cart;
* không được checkout.

\---

## FR-PRODUCT-009 – Soft Delete Product

ADMIN phải có thể soft-delete Product bằng:

```text

deletedAt

```

Soft-deleted Product:

* không xuất hiện trong public Product API;
* không được thêm vào Cart;
* không được checkout;
* không bị hard-delete trong normal delete operation;
* không làm mất OrderItem history.

\---

# 11\. COLLECTION MODULE

## 11.1. Collection Data

Collection tối thiểu có:

```text

id

nameVi

nameEn

descriptionVi

descriptionEn

status

createdAt

updatedAt

deletedAt

```

CollectionStatus:

```text

ACTIVE

INACTIVE

```

Soft Delete:

```text

deletedAt

```

\---

## FR-COLLECTION-001 – Public Collection List

GUEST và USER phải có thể xem Collection List.

Public chỉ trả:

```text

status = ACTIVE

AND

deletedAt IS NULL

```

Priority:

```text

MUST

```

\---

## FR-COLLECTION-002 – Public Collection Detail

Hệ thống SHOULD hỗ trợ Public Collection Detail nếu frontend triển khai trang riêng cho Collection.

Collection Detail có thể cung cấp:

* Collection metadata;
* thông tin cần thiết để truy vấn Product thuộc Collection.

Priority:

```text

SHOULD

```

Đây không phải requirement bắt buộc để nghiệm thu core Version 1.3 nếu frontend không có Collection Detail page riêng.

\---

## FR-COLLECTION-003 – Admin Collection List

ADMIN phải có thể:

* xem Collection List;
* search;
* pagination;
* xem trạng thái Collection.

\---

## FR-COLLECTION-004 – Create Collection

ADMIN phải có thể tạo Collection.

\---

## FR-COLLECTION-005 – Update Collection

ADMIN phải có thể cập nhật:

```text

nameVi

nameEn

descriptionVi

descriptionEn

status

```

\---

## FR-COLLECTION-006 – Enable/Disable Collection

ADMIN phải có thể:

```text

ACTIVE ↔ INACTIVE

```

Collection `INACTIVE` không xuất hiện trong public Collection List.

Disable Collection không được tự động delete Product.

\---

## FR-COLLECTION-007 – Soft Delete Collection

ADMIN có thể soft-delete Collection nếu data integrity cho phép.

Business Rules:

* không hard-delete Collection đang được Product sử dụng;
* không cascade delete Product;
* không làm mất historical data.

Nếu Collection còn được Product tham chiếu, Backend phải reject soft-delete hoặc yêu cầu Product được chuyển sang Collection hợp lệ trước.

Exact handling:

```text

To be defined in Implementation Specification.

```

\---

# 12\. SIMPLE PRODUCT DISCOUNT

Hệ thống chỉ hỗ trợ giảm giá đơn giản trực tiếp theo Product.

DiscountType:

```text

PERCENTAGE

FIXED\\\_PRICE

```

Logical data tối thiểu:

```text

discountType

discountValue

startAt

endAt

isActive

```

SRS không bắt buộc Discount là entity riêng hay fields liên kết với Product.

Data model cụ thể:

```text

To be defined in Implementation Specification.

```

\---

## FR-DISCOUNT-001 – Configure Discount

ADMIN phải có thể tạo/cấu hình Discount cho Product.

\---

## FR-DISCOUNT-002 – Update Discount

ADMIN phải có thể cập nhật Discount nếu configuration mới hợp lệ.

\---

## FR-DISCOUNT-003 – Enable/Disable Discount

ADMIN phải có thể:

```text

isActive = true

isActive = false

```

\---

## FR-DISCOUNT-004 – Remove Discount

ADMIN phải có thể remove Discount configuration.

Remove Discount không được:

* delete Product;
* thay đổi historical OrderItem.

\---

## FR-DISCOUNT-005 – Effective Discount and Selling Price

Discount chỉ effective khi:

```text

isActive = true

AND

businessCurrentTime >= startAt

AND

businessCurrentTime <= endAt

```

Trong đó:

```text

businessCurrentTime

startAt

endAt

```

phải được diễn giải theo business-time semantics nhất quán với:

```text

Asia/Ho\\\_Chi\\\_Minh

UTC+07:00

```

Frontend không được tự quyết định Discount active/inactive.

Backend là source of truth cho Discount effectiveness.

Không có effective Discount:

```text

sellingPrice = basePrice

```

`PERCENTAGE`:

```text

rawSellingPrice =

basePrice - (basePrice × discountValue / 100)

sellingPrice =

HALF\\\_UP(rawSellingPrice, nearest 1 VND)

```

Nếu `rawSellingPrice` có phần thập phân, Backend phải áp dụng `HALF\\\_UP` về đơn vị 1 VND trước khi `sellingPrice` được sử dụng để tính `OrderItem.totalPrice`.

`FIXED\\\_PRICE`:

```text

sellingPrice = discountValue

```

Authoritative `sellingPrice` phải tuân theo `BR-MONEY-001`.

Backend phải tính sellingPrice.

Frontend không được quyết định authoritative sellingPrice.

\---

## 12.1. Discount Validation Rules

Luôn yêu cầu:

```text

basePrice > 0

startAt < endAt

sellingPrice > 0

```

`PERCENTAGE`:

```text

0 < discountValue < 100

```

`PERCENTAGE discountValue` không bắt buộc là integer. Các giá trị như `10.5` hoặc `12.25` có thể hợp lệ nếu vẫn thỏa toàn bộ validation; monetary result sau percentage calculation phải áp dụng `HALF\\\_UP` theo `BR-MONEY-001`.

`FIXED\\\_PRICE`:

```text

0 < discountValue < basePrice

AND

discountValue phải là số nguyên VND

```

`FIXED\\\_PRICE discountValue` có fractional VND phải bị reject. Hệ thống không được silently round invalid `FIXED\\\_PRICE` input thành một giá trị VND khác.

Một Product không được có nhiều effective Discount đồng thời.

Không được có overlap gây ambiguity.

\---

## BR-MONEY-001 – VND Monetary and Rounding Rule

Currency authoritative duy nhất của hệ thống là:

```text

VND

```

Các authoritative monetary value sau phải là số nguyên VND:

```text

basePrice

sellingPrice

OrderItem.totalPrice

Order.totalAmount

Payment.amount

```

Không được tồn tại authoritative business monetary value có fractional VND đối với các giá trị trên.

### PERCENTAGE Discount Rounding

Đối với:

```text

DiscountType = PERCENTAGE

```

Backend phải tính raw selling price:

```text

rawSellingPrice =
basePrice - (basePrice × discountValue / 100)

```

Nếu `rawSellingPrice` có phần thập phân, Backend phải làm tròn theo:

```text

sellingPrice =
HALF\\\_UP(rawSellingPrice, nearest 1 VND)

```

Việc làm tròn phải hoàn tất trước khi `sellingPrice` được sử dụng để tính `OrderItem.totalPrice`.

### Ví dụ bắt buộc

Given:

```text

basePrice = 100001 VND
discountValue = 15%

rawSellingPrice =
100001 - (100001 × 15 / 100)
= 85000.85

HALF\\\_UP:

sellingPrice = 85001 VND

```

Nếu:

```text

quantity = 2

```

thì:

```text

OrderItem.totalPrice =
85001 × 2
= 170002 VND

```

Nếu Order chỉ có OrderItem này:

```text

Order.totalAmount = 170002 VND
Payment.amount = 170002 VND

```

### Rounding Order of Calculation

Authoritative calculation order phải là:

```text

Product.basePrice
↓
Evaluate effective Discount
↓
Calculate rawSellingPrice
↓
Apply VND HALF\\\_UP rounding
↓
sellingPrice
↓
sellingPrice × quantity
↓
OrderItem.totalPrice
↓
SUM(OrderItem.totalPrice)
↓
Order.totalAmount
↓
Payment.amount

```

Không được round `Order.totalAmount` theo một rule khác.

Frontend không được tự áp dụng một rounding rule khác Backend.

### Payment Amount Consistency

Sau khi toàn bộ monetary calculation và rounding hoàn tất, invariant bắt buộc là:

```text

Payment.amount = Order.totalAmount

```

Không được tồn tại mismatch giữa `Payment.amount` và `Order.totalAmount`.

Ví dụ không hợp lệ:

```text

Order.totalAmount = 170002
Payment.amount = 170001

```

Backend là authoritative source cho monetary calculation và rounding.

### Monetary Consistency Across Modules

Cùng `BR-MONEY-001` phải được áp dụng nhất quán cho:

```text

Product Display
Cart
Checkout
Order
Payment

```

Frontend chỉ render authoritative monetary data do Backend trả về và không được tự tạo hoặc override authoritative monetary calculation.

\---

# 13\. PRICE SOURCE OF TRUTH

Backend là authoritative source cho:

```text

basePrice

sellingPrice

OrderItem.totalPrice

Order.totalAmount

Payment.amount

```

Frontend không được gửi authoritative price để Backend tin tưởng.

Tại checkout, Backend phải:

1. lấy Product hiện tại;
2. validate Product;
3. lấy `basePrice`;
4. kiểm tra effective Discount bằng business-time semantics;
5. tính `rawSellingPrice` khi áp dụng `PERCENTAGE` Discount;
6. áp dụng `HALF\\\_UP` về nearest 1 VND theo `BR-MONEY-001` trước khi xác lập `sellingPrice`;
7. tính `OrderItem.totalPrice = sellingPrice × quantity`;
8. tính `Order.totalAmount = SUM(OrderItem.totalPrice)`;
9. tạo `Payment.amount = Order.totalAmount`.

Không được round Order total theo một rule khác với `BR-MONEY-001`.

Nếu Product price hoặc Discount thay đổi giữa thời điểm Cart được hiển thị và checkout, giá authoritative tại thời điểm checkout được sử dụng.

Frontend phải sử dụng giá/total do Backend trả về sau validation và không được override authoritative rounded value.

## 13.1. Price Terminology

Price terminology được sử dụng nhất quán trong SRS gồm:

```text

basePrice
rawSellingPrice
sellingPrice
OrderItem.totalPrice
Order.totalAmount
Payment.amount

```

Semantics:

```text

basePrice
= giá gốc Product

rawSellingPrice
= temporary calculation value cho PERCENTAGE Discount trước rounding

sellingPrice
= giá bán authoritative cuối cùng sau Discount/rounding

OrderItem.totalPrice
= sellingPrice × quantity

Order.totalAmount
= SUM(OrderItem.totalPrice)

Payment.amount
= Order.totalAmount

```

`rawSellingPrice` chỉ là logical calculation intermediate value cho `PERCENTAGE` Discount và không phải persistent business field bắt buộc trong SRS.

Exact persistence representation của `rawSellingPrice`, nếu có:

```text

To be defined in Implementation Specification.

```

SRS không yêu cầu thêm database field mới cho `rawSellingPrice`.

\---

# 14\. PRODUCT IMAGE MODULE

Mỗi Product:

```text

maximum 10 images

maximum 1 thumbnail

```

Supported formats:

```text

JPEG

PNG

WebP

```

Storage:

```text

Amazon S3

```

PostgreSQL chỉ lưu metadata/reference/URL.

Không lưu binary image trong PostgreSQL.

\---

## FR-IMAGE-001 – Product Image Retrieval

Product Detail phải trả Product Images theo thứ tự hiển thị.

Logical data tối thiểu:

```text

id

url

isThumbnail

sortOrder

```

\---

## FR-IMAGE-002 – Upload Product Image

ADMIN phải có thể upload Product Image.

Backend phải validate:

* file type;
* MIME type;
* configured file size limit;
* Product tồn tại;
* Product chưa soft-delete;
* current image count;
* maximum 10 images.

Maximum file size:

```text

To be defined in Implementation Specification.

```

\---

## FR-IMAGE-003 – Delete Product Image

ADMIN phải có thể delete Product Image.

Delete metadata và S3 object phải được xử lý nhất quán theo implementation design.

\---

## FR-IMAGE-004 – Reorder Product Images

ADMIN phải có thể thay đổi thứ tự Product Images.

\---

## FR-IMAGE-005 – Set Thumbnail

ADMIN phải có thể chọn Product Image làm thumbnail.

Invariant:

```text

maximum 1 thumbnail per Product

```

Nếu thumbnail hiện tại bị xóa, cách chọn thumbnail thay thế:

```text

To be defined in Implementation Specification.

```

\---

## FR-IMAGE-006 – Image Validation and Storage

Hệ thống phải reject:

* unsupported file type;
* invalid MIME type;
* file vượt configured maximum size;
* upload làm Product vượt quá 10 images.

Product Image production storage phải sử dụng Amazon S3.

\---

## FR-IMAGE-007 – Product Image Frontend Interaction

Frontend Product Detail phải hỗ trợ:

```text

Zoom In

Zoom Out

Thumbnail Selection

Mobile Swipe

```

Không yêu cầu Backend API riêng ngoài dữ liệu Product Images.

\---

## 14.1. Image Capacity Assumption

Website dự kiến khoảng:

```text

<= 100 Product Images

```

Đây là capacity assumption phục vụ planning.

Không phải hard business limit.

Hệ thống không bắt buộc reject image thứ 101 chỉ dựa trên assumption này.

\---

# 15\. CART MODULE

Mỗi USER có Cart riêng.

Cart chỉ lưu:

```text

Product Reference

Quantity

```

Cart không phải source of truth cho giá.

Thêm Product vào Cart không reserve Inventory.

\---

## FR-CART-001 – View Cart

USER phải có thể xem Cart của chính mình.

Backend phải trả tối thiểu:

* Cart Items;
* Product information cần thiết;
* quantity;
* current base price;
* current selling price;
* current line total;
* current availability.

Giá phải được Backend tính lại tại thời điểm đọc Cart.

\---

## FR-CART-002 – Add Cart Item

USER phải có thể thêm Product vào Cart.

Validation:

```text

quantity >= 1

Product exists

Product status = ACTIVE

Product deletedAt IS NULL

requestedQuantity <= availableQuantity

```

Stock validation tại thời điểm add Cart không đồng nghĩa Inventory đã được reserve.

\---

## FR-CART-003 – Update Cart Quantity

USER phải có thể thay đổi Cart Item quantity.

Validation:

```text

quantity >= 1

requestedQuantity <= current availableQuantity

Product remains purchasable

```

\---

## FR-CART-004 – Remove Cart Item

USER phải có thể remove Cart Item khỏi Cart.

\---

## FR-CART-005 – Backend Cart Pricing

Client không được authoritative cho:

```text

basePrice

sellingPrice

lineTotal

cartTotal

```

Backend phải tự lấy Product pricing và tính lại.

\---

## FR-CART-006 – Checkout Cart

USER phải có thể sử dụng Cart Item hợp lệ để bắt đầu checkout.

Checkout phải validate lại toàn bộ:

* Product;
* Product status;
* soft-delete state;
* effective selling price;
* Inventory;
* quantity.

\---

## FR-CART-007 – Successful Checkout Cart Lifecycle

Sau khi checkout đã đạt đầy đủ state:

```text

Order created

OrderItems created

Inventory reserved

Payment record created

payOS Payment Information created successfully

```

các CartItem tham gia checkout phải được remove khỏi Cart.

Nếu toàn bộ CartItem được checkout:

```text

Cart trở thành empty

```

Cart lifecycle operation phải tương ứng đúng với các CartItem thuộc successful checkout operation.

Checkout idempotency vẫn phải được enforce độc lập ở server.

\---

## FR-CART-008 – Failed Checkout Cart Preservation

Nếu checkout thất bại trước khi payOS Payment Information được tạo thành công:

```text

Cart Items phải được giữ nguyên

```

Ví dụ:

* Product validation failure;
* insufficient Inventory;
* Order local transaction rollback;
* Payment record creation failure;
* payOS creation failure.

Payment failure xảy ra sau một successful checkout initiation không bắt buộc tự động restore CartItem đã được clear.

Nếu USER muốn mua lại, USER thực hiện một checkout mới với Product được chọn lại phù hợp.

\---

# 16\. INVENTORY MODEL

Mỗi Product có đúng một Inventory.

Logical fields tối thiểu:

```text

productId

quantity

reservedQuantity

lowStockThreshold

updatedAt

```

Available stock:

```text

availableQuantity =

quantity - reservedQuantity

```

Invariant:

```text

quantity >= 0

reservedQuantity >= 0

availableQuantity >= 0

```

Inventory không có Variant dimension.

\---

# 17\. ADMIN INVENTORY MANAGEMENT

## FR-INV-001 – Inventory List, Search and Pagination

ADMIN phải có thể:

* xem Inventory List;
* search Inventory;
* pagination.

List tối thiểu hiển thị:

```text

Product Name

Quantity

Reserved Quantity

Available Quantity

Low Stock Threshold

Status

```

Display status có thể derive:

```text

IN\\\_STOCK

LOW\\\_STOCK

OUT\\\_OF\\\_STOCK

```

Exact persistence/derivation:

```text

To be defined in Implementation Specification.

```

\---

## FR-INV-002 – Adjust Inventory

ADMIN phải có thể điều chỉnh physical quantity.

Hỗ trợ:

```text

Increase Quantity

Decrease Quantity

Reason

```

Ví dụ:

```text

quantityChange = +20

reason = "Nhập hàng mới"

```

hoặc:

```text

quantityChange = -2

reason = "Điều chỉnh kiểm kê"

```

Resulting Inventory phải tiếp tục thỏa invariants.

Không được làm:

```text

quantity < reservedQuantity

```

hoặc:

```text

availableQuantity < 0

```

\---

## FR-INV-003 – Inventory Transaction History

ADMIN phải có thể xem Inventory Transaction History.

Hỗ trợ tối thiểu:

* theo Product;
* pagination;
* chronological ordering.

\---

## FR-INV-004 – Reserve Inventory

Khi checkout hợp lệ, hệ thống phải reserve `orderedQuantity`.

Rule:

```text

inventory.quantity = unchanged



inventory.reservedQuantity =

inventory.reservedQuantity + orderedQuantity

```

Chỉ reserve nếu:

```text

orderedQuantity <= availableQuantity

```

\---

## FR-INV-005 – Commit Reserved Inventory

Khi authoritative Payment state được xác nhận:

```text

Payment.status = PAID

```

hệ thống phải commit reserved stock đúng một lần.

Rule:

```text

inventory.quantity =

inventory.quantity - orderedQuantity



inventory.reservedQuantity =

inventory.reservedQuantity - orderedQuantity

```

Sau commit vẫn phải thỏa Inventory invariants.

\---

## FR-INV-006 – Release Reserved Inventory

Khi reservation phải release:

```text

inventory.quantity = unchanged



inventory.reservedQuantity =

inventory.reservedQuantity - releasedQuantity

```

Release phải xảy ra đúng một lần cho reserved quantity tương ứng.

\---

## FR-INV-007 – Low Stock and Out of Stock

Low Stock:

```text

availableQuantity <= lowStockThreshold

AND

availableQuantity > 0

```

Out of Stock:

```text

availableQuantity = 0

```

Hệ thống phải có khả năng cung cấp Low Stock Products cho ADMIN Dashboard và Notification.

\---

## FR-INV-008 – Concurrency-safe Inventory

Inventory reservation phải concurrency-safe.

Nếu:

```text

availableQuantity = 1

```

và:

```text

User A checkout quantity 1

User B checkout quantity 1

```

đồng thời, chỉ một checkout được phép reserve thành công.

Hệ thống không được oversell.

Exact locking/concurrency implementation:

```text

To be defined in Implementation Specification.

```

\---

# 18\. INVENTORY TRANSACTION AUDIT

Mọi thay đổi Inventory quan trọng phải có InventoryTransaction phù hợp.

Logical InventoryTransaction data phải đủ audit cả physical quantity và reserved quantity.

Tối thiểu:

```text

id

productId

type

quantityChange / businessQuantity

beforeQuantity

afterQuantity

beforeReservedQuantity

afterReservedQuantity

referenceId

reason

createdAt

```

Representation tương đương được phép nếu vẫn audit đầy đủ:

* physical quantity trước/sau;
* reserved quantity trước/sau;
* business event gây thay đổi.

Exact schema:

```text

To be defined in Implementation Specification.

```

InventoryTransactionType tối thiểu:

```text

IMPORT

ADJUSTMENT

RESERVE

RELEASE

SALE

CANCEL\\\_ORDER

```

\---

## 18.1. IMPORT Semantics

```text

physical quantity increases

reservedQuantity unchanged

```

\---

## 18.2. ADJUSTMENT Semantics

```text

physical quantity increases hoặc decreases

reservedQuantity normally unchanged

```

Sau adjustment vẫn phải thỏa Inventory invariants.

\---

## 18.3. RESERVE Semantics

```text

physical quantity unchanged

reservedQuantity increases

```

Ví dụ:

```text

Before:

quantity = 10

reservedQuantity = 0



Reserve:

orderedQuantity = 2



After:

quantity = 10

reservedQuantity = 2

```

Audit record phải thể hiện được thay đổi reserved quantity.

\---

## 18.4. RELEASE Semantics

```text

physical quantity unchanged

reservedQuantity decreases

```

Dùng khi reservation không còn cần thiết.

\---

## 18.5. SALE Semantics

```text

physical quantity decreases

reservedQuantity decreases

```

Dùng khi Payment thành công và reserved Inventory được commit.

\---

## 18.6. CANCEL\_ORDER Semantics

Đối với paid Order đã commit Inventory nhưng sau đó được ADMIN cancel:

```text

physical quantity increases

reservedQuantity unchanged

```

Inventory chỉ được restore đúng một lần.

\---

# 19\. CANONICAL CHECKOUT FLOW

Hệ thống chỉ sử dụng một canonical checkout flow:

```text

USER Checkout

↓

Validate Cart

↓

Validate Product

↓

Calculate Selling Price

↓

Calculate Order Total

↓

Validate Inventory

↓

Create Order

↓

Create OrderItems

↓

Reserve Inventory

↓

Create Payment Record

↓

Create payOS Payment Information

↓

Remove checked-out Cart Items

↓

Create NEW\\\_ORDER Notification

↓

Return payOS Payment Information

```

Successful checkout initiation được xác định khi:

```text

Order created

OrderItems created

Inventory reserved

Payment record created

payOS Payment Information created successfully

Cart Items processed according to checkout lifecycle

```

State thông thường tại thời điểm successful checkout initiation:

```text

Order = NEW

Payment = PENDING

Inventory = RESERVED

```

Các local database operation liên quan đến:

```text

Order

OrderItems

Inventory Reservation

InventoryTransaction

Payment Record

Cart lifecycle operation khi appropriate

Notification persistence khi appropriate

```

phải sử dụng transaction/consistency boundary phù hợp.

Nếu một local database step quan trọng thất bại:

```text

rollback

```

Không được để Order incomplete.

Ví dụ không hợp lệ:

```text

Order tồn tại

nhưng chỉ tạo được một phần OrderItems.

```

Việc gọi payOS là external operation và có thể không nằm trong cùng database transaction.

Transaction boundary, external retry và compensation strategy:

```text

To be defined in Implementation Specification.

```

\---

## 19.1. payOS Creation Failure Consistency

Nếu các state sau đã được tạo:

```text

Order created

OrderItems created

Inventory reserved

Payment record created

```

nhưng:

```text

Create payOS Payment Information

```

thất bại và operation không thể tiếp tục, eventual business state phải trở thành:

```text

Payment = FAILED

Order = CANCELLED

Inventory Reservation = RELEASED

InventoryTransaction = RELEASE

```

Operation phải:

```text

idempotent

consistent

logged

```

Không được để:

```text

Order = NEW

Payment = PENDING hoặc FAILED

Inventory vẫn RESERVED vô thời hạn

```

Nếu external failure mang tính transient và hệ thống áp dụng retry strategy, implementation cụ thể:

```text

To be defined in Implementation Specification.

```

Dù áp dụng retry, eventual state vẫn phải đảm bảo consistency.

Trong trường hợp payOS creation failure:

```text

Cart Items phải được giữ nguyên

```

vì successful checkout lifecycle chưa hoàn tất.

`NEW\\\_ORDER` Notification không được tạo nếu successful checkout initiation chưa hoàn tất.

\---

# 20\. ORDER MODULE

## 20.1. Order Data

Order logical data tối thiểu:

```text

id

orderCode

userId



receiverName

phone

email

address

note



subtotal

totalAmount



orderStatus



createdAt

updatedAt

```

`Payment.status` là authoritative payment state.

Order API response có thể expose:

```text

paymentStatus

```

nhưng giá trị phải derive từ Payment tương ứng.

SRS không yêu cầu `Order.paymentStatus` là một authoritative persistent field.

Order total:

```text

subtotal =

SUM(OrderItem.totalPrice)



totalAmount =

subtotal

```

\---

## 20.2. OrderItem Data

OrderItem phải snapshot tối thiểu:

```text

id

orderId

productId

productNameVi

productNameEn

basePrice

sellingPrice

quantity

totalPrice

```

Rule:

```text

totalPrice =

sellingPrice × quantity

```

`sellingPrice` sử dụng trong phép tính trên phải là authoritative rounded value theo `BR-MONEY-001`.

OrderItem phải snapshot Product name bằng cả Vietnamese và English tại thời điểm Order được tạo.

Sau Order creation, các thay đổi sau của Product không được làm thay đổi historical OrderItem:

```text

Product.nameVi thay đổi
Product.nameEn thay đổi
Product bị disable
Product bị soft-delete

```

Cụ thể:

```text

OrderItem.productNameVi = historical Product.nameVi tại Order creation
OrderItem.productNameEn = historical Product.nameEn tại Order creation

```

Order Detail phải sử dụng historical Product Name theo current UI locale:

```text

locale = vi
→ OrderItem.productNameVi

locale = en
→ OrderItem.productNameEn

```

Không được query current Product name để thay thế historical snapshot. Nếu current Product đã rename, Order history vẫn phải sử dụng snapshot.

Không snapshot `productDescriptionVi` hoặc `productDescriptionEn` vào OrderItem trong scope Version 1.3.

Nếu implementation dùng tên `unitPrice` thay cho `sellingPrice`, semantics phải tương đương.

\---

## 20.3. Order Status

Chỉ có:

```text

NEW

CONFIRMED

COMPLETED

CANCELLED

```

Order processing chỉ được tiếp tục sau khi Payment đã được xác nhận:

```text

PAID

```

\---

## FR-ORDER-001 – Create Order

USER phải có thể tạo Order thông qua checkout.

Order chỉ được tạo khi:

* User authenticated;
* Cart hợp lệ;
* Product hợp lệ;
* sellingPrice do Backend tính;
* Inventory đủ;
* receiver information hợp lệ.

Order ban đầu:

```text

OrderStatus = NEW

```

\---

## FR-ORDER-002 – View My Orders

USER phải có thể xem danh sách Order của chính mình.

Danh sách phải hỗ trợ pagination.

Mỗi item tối thiểu hiển thị:

```text

orderCode

createdAt

totalAmount

orderStatus

paymentStatus derived from Payment.status

```

Business-facing `createdAt` phải được hiển thị theo:

```text

Asia/Ho\\\_Chi\\\_Minh

```

\---

## FR-ORDER-003 – View User Order Detail

USER phải có thể xem Order Detail của chính mình.

USER không được xem Order của User khác.

Order Detail tối thiểu:

* orderCode;
* receiver information;
* Order Items;
* total amount;
* Order Status;
* Payment Status derived từ Payment;
* createdAt.

USER chỉ xem trạng thái Order hiện tại.

Product Name trong Order Items phải được hiển thị từ historical OrderItem snapshot theo current UI locale; không được thay thế bằng current Product name.

Business-facing timestamps phải tuân theo `BR-TIME-001`.

\---

## FR-ORDER-004 – Admin Order List, Search, Filter and Pagination

ADMIN phải có thể:

* xem tất cả Order;
* search;
* pagination;
* filter.

Filter tối thiểu:

```text

orderStatus

paymentStatus

fromDate

toDate

keyword

```

`paymentStatus` filter phải sử dụng authoritative Payment state.

Date range phải được diễn giải theo business timezone:

```text

Asia/Ho\\\_Chi\\\_Minh

```

Keyword có thể hỗ trợ:

```text

orderCode

receiverName

email

phone

```

Exact search behavior:

```text

To be defined in Implementation Specification.

```

\---

## FR-ORDER-005 – Admin Order Detail

ADMIN phải có thể xem Order Detail của mọi User.

Thông tin tối thiểu:

```text

Order Code

Customer

Receiver Information

Order Items

Price Snapshot

Total Amount

Order Status

Payment Status derived from Payment

Created At

Updated At

```

Product Name trong Order Items phải được hiển thị từ historical OrderItem snapshot theo current UI locale; không được thay thế bằng current Product name.

Business-facing timestamps phải tuân theo `BR-TIME-001`.

\---

## FR-ORDER-006 – Update Order Status

ADMIN phải có thể cập nhật Order Status theo state transition và Payment precondition hợp lệ.

### NEW → CONFIRMED

Chỉ hợp lệ khi:

```text

OrderStatus = NEW

AND

Payment.status = PAID

```

Allowed:

```text

NEW + PAID

→ CONFIRMED

```

Các trường hợp sau phải reject:

```text

NEW + PENDING → CONFIRMED



NEW + FAILED → CONFIRMED



NEW + CANCELLED → CONFIRMED



NEW + EXPIRED → CONFIRMED



NEW + REFUNDED → CONFIRMED

```

### CONFIRMED → COMPLETED

Chỉ hợp lệ khi:

```text

OrderStatus = CONFIRMED

AND

Payment.status = PAID

```

Allowed:

```text

CONFIRMED + PAID

→ COMPLETED

```

Nếu:

```text

Payment.status != PAID

```

thì:

```text

CONFIRMED → COMPLETED

```

phải reject.

Không được hoàn thành Order chưa thanh toán.

### Cancellation

ADMIN được phép:

```text

NEW → CANCELLED



CONFIRMED → CANCELLED

```

Cancellation phải xử lý Payment và Inventory theo cancellation business rules tương ứng.

### Invalid State Examples

```text

COMPLETED → NEW = REJECT



COMPLETED → CANCELLED = REJECT



CANCELLED → CONFIRMED = REJECT



CANCELLED → COMPLETED = REJECT

```

Backend phải validate:

* current Order state;
* requested transition;
* authoritative Payment state.

Frontend không được là nơi duy nhất enforce rule.

payOS Payment success:

```text

Payment = PAID

Inventory = COMMITTED

Order vẫn = NEW

```

Payment success không tự động chuyển Order sang `CONFIRMED`.

ADMIN sau đó mới được:

```text

NEW → CONFIRMED → COMPLETED

```

\---

## FR-ORDER-007 – Admin Cancel Order

Chỉ ADMIN được hủy Order.

USER không có:

* Cancel Order API;
* Cancel Order action;
* quyền chuyển Order sang `CANCELLED`.

Nếu USER muốn hủy:

```text

USER liên hệ cửa hàng

↓

ADMIN xử lý cancellation

```

\---

## FR-ORDER-008 – Receiver Information and Order Code

Order phải lưu:

```text

receiverName

phone

email

address

note

```

Mỗi Order phải có:

```text

orderCode UNIQUE

```

Ví dụ:

```text

ORD-20260811-000001

```

Generation algorithm:

```text

To be defined in Implementation Specification.

```

Receiver information được lưu theo Order, độc lập với Google profile.

\---

## FR-ORDER-009 – Historical OrderItem Snapshot

Khi Order được tạo, Backend phải snapshot:

```text

productId

productNameVi

productNameEn

basePrice

sellingPrice

quantity

totalPrice

```

`productNameVi` và `productNameEn` phải phản ánh Product name tại đúng thời điểm Order được tạo.

Sau Order creation, các thay đổi sau không được thay đổi OrderItem historical snapshot đã tồn tại:

* Product `nameVi` thay đổi;
* Product `nameEn` thay đổi;
* Product `basePrice` thay đổi;
* Product Discount thay đổi;
* Discount hết hạn;
* Product `status` thay đổi hoặc Product bị disable;
* Product `deletedAt` thay đổi hoặc Product bị soft-delete.

Historical `basePrice`, `sellingPrice`, `quantity` và `totalPrice` không được bị mutate bởi Product price/Discount change sau Order creation.

Order Detail phải hiển thị historical Product Name theo current UI locale:

```text

locale = vi
→ OrderItem.productNameVi

locale = en
→ OrderItem.productNameEn

```

Không được query current Product name để thay thế historical snapshot.

\---

## FR-ORDER-010 – Checkout Idempotency

Checkout phải hỗ trợ idempotency hoặc cơ chế tương đương.

Nếu cùng logical checkout operation được gửi lặp do:

```text

double click

network retry

browser retry

```

hệ thống không được tạo duplicate Order.

Có thể sử dụng:

```text

Idempotency-Key

```

hoặc cơ chế tương đương.

Implementation:

```text

To be defined in Implementation Specification.

```

Idempotency phải hoạt động độc lập với việc CartItem được clear sau successful checkout.

\---

## FR-ORDER-011 – One Order One Payment

Trong Version 1.3:

```text

1 Order

→ exactly 1 Payment

```

Một Order không có nhiều Payment Attempt.

Logical constraint:

```text

Payment.orderId UNIQUE

```

hoặc cơ chế tương đương phải bảo đảm một Order không có nhiều Payment record.

Exact database constraint:

```text

To be defined in Implementation Specification.

```

Nếu Payment kết thúc ở:

```text

FAILED

CANCELLED

EXPIRED

```

thì Order phải:

```text

CANCELLED

```

Order đã `CANCELLED` không được reuse để tạo Payment mới.

Nếu USER muốn mua lại:

```text

USER thực hiện checkout mới

↓

New Order

↓

New Payment

```

\---

# 21\. PAYMENT MODULE

Payment Gateway active duy nhất:

```text

payOS

```

PaymentProvider:

```text

PAYOS

```

\---

## 21.1. Payment Status Source of Truth

## FR-PAY-001 – Authoritative Payment Status

```text

Payment.status

```

là authoritative payment state của hệ thống.

Nếu Order API response hiển thị:

```text

paymentStatus

```

giá trị phải được derive từ Payment tương ứng.

Không được tồn tại contradictory business state như:

```text

Order response paymentStatus = PENDING

Payment.status = PAID

```

Nếu implementation quyết định denormalize Payment Status vào Order table vì performance hoặc query reason, implementation phải bảo đảm transactional consistency giữa hai representation.

Exact design:

```text

To be defined in Implementation Specification.

```

\---

## 21.2. Payment Data

Payment logical data tối thiểu phải đủ để:

* liên kết với Order;
* nhận diện external payment;
* kiểm tra amount;
* theo dõi status;
* enforce expiration;
* enforce idempotency;
* audit payment events.

Logical data tối thiểu:

```text

id

orderId

provider

amount

status

externalTransactionIdentifier

createdAt

expiresAt

paidAt

updatedAt

```

Logical cardinality:

```text

orderId is unique per Payment relationship

```

`externalTransactionIdentifier` có thể chưa tồn tại ngay tại thời điểm local Payment record vừa được tạo.

Ví dụ:

```text

Create local Payment record

↓

Payment.status = PENDING

↓

Call payOS

```

Nếu payOS Payment Information chưa được tạo thành công hoặc payOS chưa cung cấp identifier tương ứng:

```text

externalTransactionIdentifier

```

có thể:

```text

null / unavailable

```

theo protocol thực tế.

Khi payOS cung cấp identifier hợp lệ, hệ thống phải lưu identifier tương ứng.

Nếu external identifier tồn tại, hệ thống phải áp dụng uniqueness/idempotency protection phù hợp.

Exact payOS-specific field mapping:

```text

To be defined in Implementation Specification.

```

\---

## 21.3. Payment Status

Sử dụng:

```text

PENDING

PAID

FAILED

CANCELLED

EXPIRED

REFUNDED

```

### PENDING

Payment đã được tạo và đang chờ thanh toán.

Inventory Reservation được giữ trong thời gian Payment còn hợp lệ.

### PAID

Backend đã nhận và verify confirmation hợp lệ từ payOS.

Inventory Reservation phải được commit đúng một lần.

### FAILED

Payment không thể hoàn thành thành công.

Order phải được cancel và reserved Inventory phải release nếu chưa release.

### CANCELLED

Payment bị cancel trước khi thanh toán thành công.

Order phải được cancel và reserved Inventory phải release nếu chưa release.

### EXPIRED

Payment vẫn chưa thanh toán thành công sau timeout 15 phút.

Order phải được cancel và reserved Inventory phải release.

### REFUNDED

Order đã được cancel theo business rule, refund thực tế đã thành công bên ngoài application và ADMIN đã ghi nhận trạng thái refund.

\---

## FR-PAY-002 – Create Payment Record

Sau khi:

* Order;
* OrderItems;
* Inventory Reservation;

đã được tạo thành công, Backend phải tạo đúng một Payment record cho Order.

Initial data:

```text

provider = PAYOS

status = PENDING

amount = Backend-calculated Order.totalAmount

```

`Payment.amount` phải bằng chính xác `Order.totalAmount` sau khi toàn bộ monetary calculation và VND rounding theo `BR-MONEY-001` đã hoàn tất.

Payment amount không được lấy authoritative từ client.

`externalTransactionIdentifier` không bắt buộc tồn tại tại thời điểm local Payment record vừa được tạo.

\---

## FR-PAY-003 – Create payOS Payment Information

Backend phải tích hợp payOS để tạo payment information.

Hệ thống phải có khả năng trả thông tin cần thiết cho frontend, ví dụ:

* payment link;
* QR/payment information;
* expiration information;

theo response thực tế của payOS.

Khi payOS trả external payment identifier phù hợp, hệ thống phải lưu identifier đó để hỗ trợ correlation/idempotency theo protocol.

Exact request/response payload và payOS field mapping:

```text

To be defined in Implementation Specification.

```

\---

## FR-PAY-004 – payOS Creation Failure

Nếu Payment record đã được tạo nhưng việc tạo payOS Payment Information thất bại và checkout không thể tiếp tục, eventual business state phải là:

```text

Payment = FAILED

Order = CANCELLED

Inventory Reservation = RELEASED

InventoryTransaction = RELEASE

```

Operation phải:

```text

idempotent

consistent

logged

```

Không được để reservation bị treo vô thời hạn.

CartItem chưa được clear trong trường hợp này.

`NEW\\\_ORDER` Notification không được tạo nếu successful checkout initiation chưa hoàn tất.

Nếu áp dụng external retry cho transient failure:

```text

To be defined in Implementation Specification.

```

Eventual state vẫn phải bảo đảm consistency.

\---

## FR-PAY-005 – Verify payOS Webhook

payOS Webhook phải được verify phía Backend.

Backend không được cập nhật `PAID` chỉ vì frontend redirect đến success page.

Backend phải:

1. nhận Webhook;
2. validate request;
3. verify authenticity/signature;
4. xác định Payment/Order;
5. kiểm tra amount và identifier phù hợp;
6. kiểm tra current Payment/Order state;
7. thực hiện idempotency check;
8. mới thực hiện state transition.

Invalid Webhook phải bị reject hoặc ignore an toàn theo protocol và phải được log phù hợp.

\---

## FR-PAY-006 – Payment Success

Khi payOS xác nhận Payment thành công bằng verified event hợp lệ:

```text

Payment → PAID

```

Sau đó hệ thống phải commit reserved Inventory:

```text

inventory.quantity -= orderedQuantity



inventory.reservedQuantity -= orderedQuantity

```

Commit phải xảy ra đúng một lần.

Sau Payment Success:

```text

Order = NEW

Payment = PAID

Inventory = COMMITTED

```

Order không tự động chuyển sang:

```text

CONFIRMED

```

ADMIN phải thực hiện Order processing sau đó.

Sau khi state transition và Inventory commit hoàn tất thành công, hệ thống phải tạo:

```text

PAYMENT\\\_SUCCESS

```

Notification theo Notification Trigger Rules.

\---

## FR-PAY-007 – Failed Payment

Nếu:

```text

Payment → FAILED

```

thì:

```text

Order → CANCELLED

```

Reserved Inventory phải release đúng một lần.

Tạo InventoryTransaction `RELEASE` phù hợp.

Sau khi unsuccessful terminal state và cleanup hoàn tất, hệ thống phải xử lý Notification theo `PAYMENT\\\_FAILED` semantics.

\---

## FR-PAY-008 – Cancelled Payment

Nếu:

```text

Payment → CANCELLED

```

trước Payment Success:

```text

Order → CANCELLED

```

Reserved Inventory phải release đúng một lần.

Sau cleanup hoàn tất, hệ thống phải xử lý Notification theo `PAYMENT\\\_FAILED` semantics.

\---

## FR-PAY-009 – Payment Expiration

Payment `PENDING` chỉ được giữ reservation trong:

```text

15 minutes

```

Expiration logic:

```text

expiresAt =

authoritative Backend payment creation time + 15 minutes

```

Frontend local clock không phải source of truth cho expiration.

Backend authoritative timestamp phải quyết định Payment có hết hạn hay chưa.

Nếu Payment chưa thành công khi hết hạn:

```text

Payment → EXPIRED

Order → CANCELLED

Release Inventory

Create InventoryTransaction(RELEASE)

```

Operation phải:

```text

atomic

idempotent

logged

```

Payment expiration khi hiển thị cho USER/ADMIN phải được diễn giải theo:

```text

Asia/Ho\\\_Chi\\\_Minh

UTC+07:00

```

Scheduler/background expiration mechanism:

```text

To be defined in Implementation Specification.

```

Sau cleanup hoàn tất, hệ thống phải xử lý Notification theo `PAYMENT\\\_FAILED` semantics.

\---

## FR-PAY-010 – Payment Webhook Idempotency

payOS Webhook có thể được gửi lặp.

Backend phải đảm bảo:

* Payment chỉ thực hiện valid transition một lần;
* Inventory không commit nhiều lần;
* Inventory không release nhiều lần;
* Inventory không restore nhiều lần;
* InventoryTransaction không duplicate cho cùng business event;
* Order state không bị corrupt;
* duplicate Webhook không tạo duplicate business operation;
* duplicate Webhook không tạo duplicate `PAYMENT\\\_SUCCESS` Notification không kiểm soát.

External transaction identifier hoặc identifier tương đương phải có uniqueness protection nếu phù hợp.

Implementation:

```text

To be defined in Implementation Specification.

```

\---

## FR-PAY-011 – Late Success Event Handling

Nếu Payment đã ở:

```text

FAILED

CANCELLED

EXPIRED

```

và:

```text

Order = CANCELLED

Inventory = RELEASED

```

nhưng Backend nhận late verified payment-success event, hệ thống không được tự:

```text

Payment → PAID

Order reopen

Inventory commit

```

nếu operation làm business state inconsistent.

Hệ thống phải:

```text

audit/log late event

flag manual resolution

```

nếu thực tế tiền đã được nhận.

Exact manual resolution policy:

```text

To be defined in Implementation Specification.

```

\---

## FR-PAY-012 – External Payment Identifier Lifecycle

`externalTransactionIdentifier` hoặc identifier tương đương của payOS có thể chưa có tại thời điểm local Payment record được tạo.

Rule:

```text

Local Payment creation

→ identifier may be unavailable



payOS returns valid external identifier

→ system persists identifier

```

Nếu identifier tồn tại:

* phải được dùng phù hợp cho correlation;
* phải có consistency/idempotency protection;
* duplicate external identifier không được dẫn đến duplicate Payment business processing.

SRS không bắt buộc tên field cụ thể của payOS.

Exact field mapping và database constraint:

```text

To be defined in Implementation Specification.

```

\---

# 22\. ORDER CANCELLATION \& INVENTORY CONSISTENCY

## 22.1. Cancel Order Before Payment Success

Nếu ADMIN hủy Order khi Payment chưa thanh toán thành công:

```text

Order → CANCELLED

```

Nếu Payment đang:

```text

PENDING

```

Payment phải chuyển sang:

```text

CANCELLED

```

Reserved Inventory phải release đúng một lần.

Result:

```text

Order = CANCELLED

Payment = CANCELLED

Inventory Reservation = RELEASED

```

\---

## 22.2. Cancel Paid Order

Nếu ADMIN hủy Order đang:

```text

OrderStatus = NEW hoặc CONFIRMED

Payment.status = PAID

Inventory = already COMMITTED

```

thì:

1. Order chuyển `CANCELLED`.
2. Không thực hiện automatic refund.
3. Physical Inventory đã commit phải restore đúng một lần.
4. Reserved quantity không thay đổi vì reservation đã được commit trước đó.
5. Tạo InventoryTransaction `CANCEL\\\_ORDER` hoặc semantics tương đương.
6. Payment vẫn là `PAID` trong thời gian chờ ADMIN xử lý refund thực tế bên ngoài application.

State hợp lệ tạm thời:

```text

Order = CANCELLED

Payment = PAID

Inventory = RESTORED exactly once

```

Sau refund thực tế và Manual Refund Recording:

```text

Order = CANCELLED

Payment = REFUNDED

Inventory = already RESTORED

```

Manual Refund Recording không được làm Inventory thay đổi lần hai.

\---

## 22.3. Completed Order

Order:

```text

COMPLETED

```

là final business state trong Version 1.3.

Không cho phép:

```text

COMPLETED → CANCELLED

```

Post-sale return/refund workflow sau `COMPLETED` không thuộc Version 1.3.

\---

# 23\. MANUAL REFUND

## FR-REFUND-001 – Record Manual Refund

Hệ thống không tự động refund.

Manual Refund Recording chỉ hợp lệ khi:

```text

OrderStatus = CANCELLED

AND

Payment.status = PAID

```

Flow chuẩn:

```text

Order = NEW hoặc CONFIRMED

Payment = PAID

Inventory = COMMITTED

↓

ADMIN Cancel Order

↓

Order = CANCELLED

Payment = PAID

Inventory = RESTORED exactly once

↓

ADMIN refund thực tế bên ngoài application

↓

Refund thực tế thành công

↓

ADMIN Record Manual Refund

↓

Order = CANCELLED

Payment = REFUNDED

```

Không được cho phép trực tiếp:

```text

Order = NEW

Payment = PAID

→ REFUNDED

```

```text

Order = CONFIRMED

Payment = PAID

→ REFUNDED

```

```text

Order = COMPLETED

Payment = PAID

→ REFUNDED

```

trong Version 1.3.

\---

## FR-REFUND-002 – Refund Inventory Isolation

Transition:

```text

Payment = PAID

→ Payment = REFUNDED

```

trong Manual Refund Recording chỉ là payment/audit state update.

Không được:

```text

increase Inventory lần nữa

release Inventory lần nữa

restore Inventory lần nữa

create duplicate Inventory restoration

```

Inventory đã được restore khi ADMIN cancel paid Order.

Manual Refund action phải được audit/log.

USER không được tự thực hiện operation này.

\---

# 24\. NOTIFICATION MODULE

Notification Database là core requirement để hỗ trợ ADMIN Notification Bell.

Notification types tối thiểu:

```text

NEW\\\_ORDER

PAYMENT\\\_SUCCESS

PAYMENT\\\_FAILED

LOW\\\_STOCK

OUT\\\_OF\\\_STOCK

```

Hệ thống hỗ trợ nhiều ADMIN.

Do đó read/unread state phải độc lập theo recipient.

\---

## FR-NOTIFY-001 – Persist Notification and Recipient State

Hệ thống phải có khả năng lưu Notification cho ADMIN.

Logical Notification data tối thiểu:

```text

notificationId

type

title

message

referenceId

createdAt

```

Logical recipient state phải đủ thể hiện:

```text

recipientAdminId

isRead

readAt

```

cho từng ADMIN recipient.

Không được sử dụng một global `isRead` duy nhất nếu Notification được chia sẻ cho nhiều ADMIN.

Có thể triển khai:

```text

Notification per Admin

```

hoặc:

```text

Notification

\\\\+

NotificationRecipient

```

Implementation:

```text

To be defined in Implementation Specification.

```

Business-facing Notification timestamps phải tuân theo `BR-TIME-001`.

\---

## FR-NOTIFY-002 – Create Business Notifications

Hệ thống phải tạo Notification phù hợp cho:

```text

NEW\\\_ORDER

PAYMENT\\\_SUCCESS

PAYMENT\\\_FAILED

LOW\\\_STOCK

OUT\\\_OF\\\_STOCK

```

Duplicate technical event không được tạo duplicate business Notification không kiểm soát.

Notification trigger phải tuân theo `FR-NOTIFY-005`.

\---

## FR-NOTIFY-003 – View, Read and Unread Notification per ADMIN

Mỗi ADMIN phải có thể:

* xem Notification List của mình;
* nhận biết read/unread;
* mark Notification read.

Read-state phải độc lập theo ADMIN.

Ví dụ:

```text

ADMIN A đọc Notification #100

→ Notification #100 = READ đối với ADMIN A



ADMIN B chưa đọc

→ Notification #100 = UNREAD đối với ADMIN B

```

Không được xảy ra:

```text

ADMIN A mark read

→ ADMIN B tự động bị mark read

```

\---

## FR-NOTIFY-004 – Real-time Notification

Hệ thống SHOULD hỗ trợ đẩy Notification gần realtime tới Admin UI.

Có thể sử dụng:

```text

Server-Sent Events

```

WebSocket không bắt buộc.

Implementation:

```text

To be defined in Implementation Specification.

```

\---

## FR-NOTIFY-005 – Notification Trigger Semantics

### NEW\_ORDER

`NEW\\\_ORDER` được tạo sau khi successful checkout initiation hoàn tất.

Successful checkout initiation nghĩa là:

```text

Order created

OrderItems created

Inventory reserved

Payment record created

payOS Payment Information created successfully

Cart Items processed according to checkout lifecycle

```

State thông thường:

```text

Order = NEW

Payment = PENDING

Inventory = RESERVED

```

Sau đó hệ thống tạo:

```text

NotificationType = NEW\\\_ORDER

```

cho các ADMIN recipient phù hợp.

`NEW\\\_ORDER` không đồng nghĩa Payment đã thành công.

\---

### PAYMENT\_SUCCESS

`PAYMENT\\\_SUCCESS` chỉ được tạo sau:

```text

verified payOS success event

↓

Payment = PAID

↓

Inventory = COMMITTED

↓

Order remains NEW

```

Sau đó:

```text

NotificationType = PAYMENT\\\_SUCCESS

```

được tạo.

Duplicate Webhook không được tạo duplicate `PAYMENT\\\_SUCCESS` business Notification không kiểm soát.

`NEW\\\_ORDER` và `PAYMENT\\\_SUCCESS` là hai business events khác nhau.

Một checkout có thể tạo:

```text

NEW\\\_ORDER

```

và sau khi thanh toán thành công, tiếp tục tạo:

```text

PAYMENT\\\_SUCCESS

```

\---

### PAYMENT\_FAILED

Trong Version 1.3, `PAYMENT\\\_FAILED` Notification được dùng như notification chung cho unsuccessful terminal payment event:

```text

FAILED

CANCELLED

EXPIRED

```

Notification chỉ được tạo sau khi business cleanup tương ứng đã đạt state nhất quán.

Ví dụ:

```text

Payment = FAILED/CANCELLED/EXPIRED

Order = CANCELLED

Inventory = RELEASED

```

Notification message/detail phải thể hiện Payment Status thực tế để ADMIN phân biệt:

```text

FAILED

CANCELLED

EXPIRED

```

Không tạo thêm Notification type riêng cho các status này trong Version 1.3.

\---

### LOW\_STOCK

Trigger condition:

```text

availableQuantity <= lowStockThreshold

AND

availableQuantity > 0

```

Notification phải được tạo khi Product chuyển từ trạng thái
không LOW\_STOCK sang LOW\_STOCK,
hoặc từ trạng thái còn hàng sang OUT\_OF\_STOCK.

\---

### OUT\_OF\_STOCK

Trigger condition:

```text

availableQuantity = 0

```

Notification phải được tạo khi Product chuyển từ trạng thái
còn hàng sang OUT\_OF\_STOCK:

availableQuantity > 0
→
availableQuantity = 0

Không được tạo lặp không kiểm soát khi Product vẫn duy trì
trạng thái OUT\_OF\_STOCK.

Exact notification deduplication strategy:
To be defined in Implementation Specification.

\---

## 24.1. NEW\_ORDER Notification vs Dashboard New Orders

Hai khái niệm này không giống nhau.

### NEW\_ORDER Notification

Được tạo sau successful checkout initiation, state thông thường:

```text

Order = NEW

Payment = PENDING

Inventory = RESERVED

```

Ý nghĩa:

```text

Có checkout/order mới được tạo thành công.

```

### Dashboard New Orders

Chỉ tính:

```text

Order = NEW

AND

Payment = PAID

```

Ý nghĩa:

```text

Order mới đã thanh toán

và sẵn sàng để ADMIN xử lý.

```

Việc tồn tại `NEW\\\_ORDER` Notification không làm Order tự động được tính vào Dashboard `New Orders` khi Payment vẫn `PENDING`.

\---

# 25\. REPORTING \& ADMIN DASHBOARD

## FR-REPORT-001 – Admin Dashboard

ADMIN Dashboard phải hiển thị tối thiểu:

```text

Total Orders

Total Revenue

New Orders

Low Stock Products

Revenue Chart

Recent Orders

Best Selling Products

```

\---

## FR-REPORT-002 – Revenue

Revenue chỉ được tính từ:

```text

OrderStatus = COMPLETED

AND

Payment.status = PAID

```

Order có Payment:

```text

REFUNDED

```

không được tính vào effective Revenue.

Revenue grouping có thể hỗ trợ:

```text

day

week

month

custom range

```

Date range và grouping phải được diễn giải theo business timezone:

```text

Asia/Ho\\\_Chi\\\_Minh

```

Exact API/query format:

```text

To be defined in Implementation Specification.

```

\---

## FR-REPORT-003 – Best Selling Products

Best Selling Products phải dựa trên:

```text

OrderStatus = COMPLETED

AND

Payment.status = PAID

```

Quantity sold phải dựa trên OrderItem snapshot.

\---

## FR-REPORT-004 – Recent Orders and Low Stock

Dashboard phải cung cấp:

* Recent Orders theo thời gian tạo;
* Low Stock Products dựa trên availableQuantity và lowStockThreshold.

Recent Orders timestamp hiển thị phải tuân theo:

```text

Asia/Ho\\\_Chi\\\_Minh

```

\---

## FR-REPORT-005 – New Orders Metric

Dashboard metric:

```text

New Orders

```

chỉ tính các Order:

```text

OrderStatus = NEW

AND

Payment.status = PAID

```

Đây là các Order đã thanh toán và đang chờ ADMIN xử lý.

Không tính:

```text

NEW + PENDING

NEW + FAILED

NEW + CANCELLED

NEW + EXPIRED

NEW + REFUNDED

```

vào actionable New Orders metric.

\---

# 26\. STATIC CONTENT \& CONTACT

FAQ, Policy và Contact là static content.

Không yêu cầu Admin CRUD Backend.

Static content có thể được lưu trong:

* Next.js;
* configuration;
* static content source phù hợp.

\---

## FR-CONTENT-001 – FAQ and Policy

Website phải hiển thị:

```text

FAQ

Policy

```

Nội dung được cung cấp bởi cửa hàng.

Không yêu cầu Backend CMS.

\---

## FR-CONTENT-002 – Contact Information

Website phải hiển thị:

```text

Email:

Cosogombautrucdangxem@gmail.com



Phone:

0343478155

0966477160



Facebook:

https://www.facebook.com/share/18jwSfSPD7/?mibextid=wwXIfr



Address:

35 Bàu Trúc,

thôn Vĩnh Thuận,

xã Ninh Phước,

tỉnh Khánh Hòa

```

Có thể hiển thị tại:

```text

Footer

Contact Page

```

Không xây live chat backend.

\---

# 27\. LOCALIZATION

## FR-I18N-001 – Vietnamese and English

Website phải hỗ trợ:

```text

Vietnamese

English

```

Product:

```text

nameVi

nameEn

descriptionVi

descriptionEn

```

Collection:

```text

nameVi

nameEn

descriptionVi

descriptionEn

```

Frontend UI phải hỗ trợ locale tối thiểu cho:

* navigation;
* button label;
* form label;
* common message;
* basic validation display;
* Product display;
* Collection display.

Nếu FAQ hoặc Policy chỉ được cửa hàng cung cấp bằng một ngôn ngữ, hệ thống không bắt buộc tự sinh translation nghiệp vụ.

Localization không được làm thay đổi interpretation của business timestamp; business-facing timestamp vẫn phải theo `BR-TIME-001`.

\---

# 28\. API REQUIREMENTS

## 28.1. API Base Boundary

REST API sử dụng base path:

```text

/api

```

Public boundary bao gồm chức năng tương đương:

```text

/api/products

/api/collections

```

Authenticated USER boundary:

```text

/api/auth/\\\\\\\*\\\\\\\*

/api/cart/\\\\\\\*\\\\\\\*

/api/orders/\\\\\\\*\\\\\\\*

```

ADMIN boundary:

```text

/api/admin/\\\\\\\*\\\\\\\*

```

URI cụ thể có thể được chuẩn hóa trong API/Implementation Specification nhưng access boundary phải được giữ.

\---

## 28.2. API Response Consistency

API phải có response structure nhất quán.

Success response phải có khả năng thể hiện:

```text

success

data

message

```

Error response phải có khả năng thể hiện:

```text

success

code

message

timestamp

validation details nếu có

```

Exact JSON envelope:

```text

To be defined in Implementation Specification.

```

\---

## 28.3. Payment State in Order API

Nếu Order response expose:

```text

paymentStatus

```

Backend phải lấy hoặc derive giá trị từ associated:

```text

Payment.status

```

Order API không được trả payment state contradictory với authoritative Payment record.

\---

## 28.4. Business-facing Timestamp Representation

API response chứa business-facing date/time phải có representation đủ rõ để frontend diễn giải nhất quán theo:

```text

Asia/Ho\\\_Chi\\\_Minh

```

SRS không bắt buộc database hoặc API phải lưu/truyền local timezone representation cụ thể.

Exact serialization strategy:

```text

To be defined in Implementation Specification.

```

\---

# 29\. API ERROR HANDLING

Hệ thống phải có error handling thống nhất.

Category tối thiểu:

```text

Validation Error

Unauthenticated

Forbidden

Resource Not Found

Conflict

Insufficient Stock

Invalid Order State

Payment Error

Duplicate/Idempotency Conflict

Unexpected Server Error

```

HTTP mapping tối thiểu:

```text

400 → Validation / Invalid Request



401 → Unauthenticated



403 → Authenticated but Forbidden



404 → Resource Not Found



409 → Business Conflict / State Conflict /

\\\&#x20;     Insufficient Stock / Idempotency Conflict



500 → Unexpected Server Error

```

Business error code nên hỗ trợ các trường hợp như:

```text

PRODUCT\\\_NOT\\\_FOUND

INSUFFICIENT\\\_STOCK

INVALID\\\_ORDER\\\_STATE

PAYMENT\\\_NOT\\\_PAID

PAYMENT\\\_ALREADY\\\_PROCESSED

INVALID\\\_REFUND\\\_STATE

LAST\\\_ADMIN\\\_PROTECTION

DUPLICATE\\\_CHECKOUT

```

Production error response không được leak:

* stack trace;
* database credentials;
* JWT secret;
* payOS secret;
* AWS credentials;
* sensitive authentication credential.

\---

# 30\. VALIDATION REQUIREMENTS

## 30.1. Product Validation

```text

nameVi required

nameEn required

basePrice > 0

basePrice phải là số nguyên VND theo BR-MONEY-001

maximum 10 images

```

Các giá trị `basePrice` như `100000`, `150000`, `99999` hợp lệ về monetary format nếu lớn hơn `0`.

Các giá trị có fractional VND như `100000.5` hoặc `99999.99` phải bị reject. Hệ thống không được silently round `Product.basePrice`.

Frontend validation có thể hỗ trợ UX nhưng Backend vẫn là authoritative validation boundary.

Collection reference phải hợp lệ nếu Product yêu cầu Collection.

\---

## 30.2. Discount Validation

```text

discountType valid

discountValue valid

startAt < endAt

sellingPrice > 0

```

Percentage:

```text

0 < discountValue < 100

```

`PERCENTAGE discountValue` không bắt buộc là integer nếu vẫn thỏa validation. Monetary result sau percentage calculation phải áp dụng `HALF\\\_UP` theo `BR-MONEY-001`.

Fixed Price:

```text

0 < discountValue < basePrice

discountValue phải là integer VND theo BR-MONEY-001

```

`FIXED\\\_PRICE discountValue` có fractional VND phải bị reject và không được silently round.

Discount time validation và effectiveness phải tuân theo `BR-TIME-001`.

\---

## 30.3. Cart Validation

```text

quantity >= 1

Product exists

Product ACTIVE

Product not soft-deleted

quantity <= availableQuantity

```

\---

## 30.4. Inventory Validation

```text

quantity >= 0

reservedQuantity >= 0

availableQuantity >= 0

```

Inventory Adjustment không được vi phạm reservation hiện có.

\---

## 30.5. Order Validation

Required receiver fields:

```text

receiverName

phone

address

```

Email phải hợp lệ nếu được cung cấp hoặc lấy từ checkout.

OrderItem quantity:

```text

>= 1

```

Order processing transition phải validate Payment precondition.

\---

## 30.6. Payment Validation

```text

Payment.amount =

Backend-calculated Order.totalAmount

```

Invariant này phải áp dụng sau monetary rounding theo `BR-MONEY-001`; không được tồn tại mismatch giữa `Payment.amount` và `Order.totalAmount`.

Client không được override amount.

payOS response/Webhook phải được verify trước Payment state transition.

Expiration phải được quyết định dựa trên authoritative Backend time.

\---

## 30.7. Manual Refund Validation

Manual Refund Recording chỉ được chấp nhận khi:

```text

OrderStatus = CANCELLED

AND

Payment.status = PAID

```

Các state khác phải reject.

\---

# 31\. DATA CONSISTENCY \& TRANSACTION REQUIREMENTS

Các nghiệp vụ sau phải bảo đảm consistency:

```text

Order Creation

OrderItem Creation

Inventory Reservation

Inventory Commit

Inventory Release

Inventory Restoration on Paid Order Cancellation

Payment Creation

payOS Creation Failure Compensation

Payment Update

Order Cancellation

Payment Expiration

Manual Refund Status Update

Cart Post-checkout Removal

Admin Last-Admin Protection

Notification Recipient Read State

Notification Business Event Creation

```

Không được có partial/inconsistent state.

Không hợp lệ:

```text

Order tồn tại nhưng OrderItems tạo thiếu.

```

```text

Payment PAID nhưng Inventory commit hai lần.

```

```text

Order CANCELLED do unpaid Payment nhưng Inventory vẫn RESERVED.

```

```text

Payment EXPIRED nhưng Inventory chưa release.

```

```text

Payment FAILED nhưng Order vẫn NEW và Inventory vẫn RESERVED.

```

```text

Payment creation thất bại nhưng reservation tồn tại vô hạn.

```

```text

Manual Refund làm Inventory restore lần hai.

```

```text

Successful checkout hoàn thành nhưng checked-out CartItem vẫn còn

do Cart lifecycle operation bị bỏ sót.

```

```text

Order response paymentStatus khác Payment.status.

```

```text

Hai Admin operation đồng thời làm hệ thống không còn ACTIVE ADMIN.

```

```text

ADMIN A mark Notification read

làm ADMIN B bị mark read.

```

```text

Duplicate Webhook tạo duplicate PAYMENT\\\_SUCCESS business notification.

```

Local database operations phải sử dụng transaction phù hợp.

Spring Boot transaction boundary cụ thể:

```text

To be defined in Implementation Specification.

```

\---

# 32\. NON-FUNCTIONAL REQUIREMENTS

## 32.1. Security Requirements

### NFR-SEC-001 – HTTPS

Production traffic giữa client và application endpoint phải sử dụng HTTPS.

\---

### NFR-SEC-002 – JWT Validation

Protected API phải validate JWT.

Expired, invalid hoặc missing token phải được xử lý theo Authentication Requirements.

\---

### NFR-SEC-003 – Backend RBAC

Authorization phải enforce phía Backend.

Frontend menu visibility không phải security control đầy đủ.

\---

### NFR-SEC-004 – Google Identity Verification

Google credential phải được verify phía Backend trước khi login/create User.

\---

### NFR-SEC-005 – payOS Webhook Verification

Mọi payOS Webhook có khả năng thay đổi Payment, Order hoặc Inventory phải được verify trước business operation.

\---

### NFR-SEC-006 – Secret Management

Sensitive secrets phải lấy từ:

```text

Environment Variable

hoặc

AWS Secrets Manager

```

Không hard-code trong source code.

\---

### NFR-SEC-007 – Sensitive Logging Protection

Không log:

```text

JWT Secret

Full JWT

payOS Secret

AWS Credentials

Sensitive Google Credential

Database Password

```

\---

### NFR-SEC-008 – Input Validation

Backend phải validate mọi state-changing request.

Client-side validation không thay thế Backend validation.

\---

### NFR-SEC-009 – User Role/Status Integrity

Runtime authorization phải phản ánh authoritative role/status trong database.

Blocked User không được thực hiện protected business operation.

\---

## 32.2. Performance Requirements

### NFR-PER-001 – Pagination

Các list có khả năng tăng dữ liệu phải sử dụng pagination phù hợp, tối thiểu:

```text

Product List

User List

Inventory List

Inventory Transaction History

Order List

Notification List

```

\---

### NFR-PER-002 – Backend Filtering

Frontend không được load toàn bộ Product database để thực hiện search/filter/pagination như production mechanism chính.

\---

### NFR-PER-003 – Database Indexing

Database phải có index phù hợp cho query thường dùng, ví dụ:

```text

User.email

Product.status

Product.collectionId

Product.basePrice / searchable pricing data

Order.orderCode

Order.orderStatus

Payment.status

createdAt

```

Exact index design:

```text

To be defined in Implementation Specification.

```

\---

### NFR-PER-004 – API Response Target

Trong môi trường test với baseline dataset và concurrency được xác định trong Test Plan, read/list API thông thường không phụ thuộc external service SHOULD có response time mục tiêu:

```text

<= 2 seconds cho phần lớn request thông thường

```

Không áp dụng trực tiếp cho:

* large image upload;
* third-party payment latency;
* network condition ngoài kiểm soát application.

\---

## 32.3. Reliability Requirements

### NFR-REL-001 – Payment Idempotency

Duplicate payOS Webhook không được gây duplicate business operation.

\---

### NFR-REL-002 – Checkout Idempotency

Duplicate logical checkout không được tạo duplicate Order.

\---

### NFR-REL-003 – Inventory Concurrency Safety

Concurrent checkout không được làm Inventory âm hoặc oversell.

\---

### NFR-REL-004 – Reservation Expiration Reliability

Expired reservation phải được release đáng tin cậy.

Retry expiration processing phải idempotent.

\---

### NFR-REL-005 – Transaction Consistency

Critical business operation không được để lại partial state.

\---

### NFR-REL-006 – Last ADMIN Protection

Concurrent role/status operation không được khiến hệ thống mất toàn bộ ACTIVE ADMIN.

\---

### NFR-REL-007 – Payment Creation Failure Recovery

payOS creation failure phải eventual converge về state:

```text

Order = CANCELLED

Payment = FAILED

Inventory = RELEASED

```

nếu operation không thể tiếp tục.

\---

### NFR-REL-008 – Order Payment State Consistency

Order API payment state phải luôn consistent với authoritative `Payment.status`.

\---

### NFR-REL-009 – Inventory Audit Completeness

Inventory Transaction History phải audit đủ physical quantity và reserved quantity thay đổi.

\---

### NFR-REL-010 – Notification Recipient Isolation

Một ADMIN thay đổi read-state Notification không được ảnh hưởng read-state của ADMIN khác.

\---

### NFR-REL-011 – Business Time Consistency

Business date/time interpretation phải nhất quán giữa Backend, Frontend, Reporting và Discount/Payment business rules.

Frontend clock không được thay thế authoritative Backend time đối với business-critical expiration/effectiveness calculation.

\---

### NFR-REL-012 – Notification Event Idempotency

Duplicate technical event không được tạo duplicate business Notification không kiểm soát.

Đặc biệt:

```text

duplicate payment success Webhook

```

không được tạo nhiều `PAYMENT\\\_SUCCESS` Notification cho cùng một payment-success business event.

\---

## 32.4. Compatibility Requirements

### NFR-COMPAT-001 – Responsive Web

Website phải responsive cho:

```text

Desktop

Tablet

Mobile

```

\---

### NFR-COMPAT-002 – Browser Compatibility

Website phải hỗ trợ tối thiểu:

```text

Google Chrome

Microsoft Edge

Modern Mobile Browser

```

\---

### NFR-COMPAT-003 – Image Interaction

Product Image UI trên mobile phải hỗ trợ touch interaction phù hợp, bao gồm Mobile Swipe.

\---

## 32.5. Observability Requirements

### NFR-OBS-001 – Application Logging

Backend phải ghi application log đủ để điều tra các business event quan trọng.

\---

### NFR-OBS-002 – AWS CloudWatch

Production application logs phải có khả năng được thu thập/quan sát thông qua AWS CloudWatch hoặc logging architecture tương đương trên AWS.

\---

### NFR-OBS-003 – Authentication Logging

Authentication error phải được log mà không ghi sensitive credential.

\---

### NFR-OBS-004 – Order Logging

Order Creation, Order Status Change và Order Cancellation quan trọng phải được log/audit phù hợp.

\---

### NFR-OBS-005 – Payment Logging

Phải log tối thiểu:

```text

Payment Record Creation

payOS Payment Creation

payOS Creation Failure

payOS Webhook Received

Webhook Verification Failure

Payment Status Transition

Payment Expiration

Late Payment Success Event

```

\---

### NFR-OBS-006 – Inventory Logging

Phải log/audit tối thiểu:

```text

Inventory Adjustment

Inventory Reservation

Inventory Commit

Inventory Release

Inventory Restore after Paid Order Cancellation

```

\---

### NFR-OBS-007 – Manual Refund Logging

Manual Refund Recording phải audit:

* Payment reference;
* Order reference;
* ADMIN thực hiện;
* timestamp;
* previous Payment status;
* new Payment status.

\---

### NFR-OBS-008 – Unexpected Exception Logging

Unexpected server exception phải được log đủ để debug nhưng không leak secret vào client response.

\---

### NFR-OBS-009 – Notification Logging

Critical Notification business event creation hoặc failure cần có logging/audit phù hợp để truy vết khi:

* NEW\_ORDER không được tạo;
* PAYMENT\_SUCCESS không được tạo;
* duplicate business Notification bị ngăn chặn.

Exact logging granularity:

```text

To be defined in Implementation Specification.

```

\---

## 32.6. Maintainability Requirements

### NFR-MAINT-001 – OpenAPI Documentation

Backend phải cung cấp REST API documentation bằng:

```text

Swagger / OpenAPI

```

Developer phải có thể xem:

* Endpoint;
* HTTP Method;
* Request;
* Response;
* Validation;
* Authentication requirement;
* Authorization requirement.

\---

### NFR-MAINT-002 – Database Migration

Database schema phải được quản lý bằng migration mechanism.

Dự kiến:

```text

Flyway

```

Production schema không được chỉnh thủ công ngoài migration process thông thường.

\---

### NFR-MAINT-003 – Requirement Traceability

Functional Requirements và critical Non-functional Requirements phải trace được tới business/data rule và Test Reference.

\---

## 32.7. Deployment Requirements

### NFR-DEPLOY-001 – AWS Deployment

Production system phải được triển khai trên AWS.

\---

### NFR-DEPLOY-002 – PostgreSQL on AWS

Production PostgreSQL phải chạy trên AWS relational database service phù hợp, dự kiến:

```text

Amazon RDS

```

\---

### NFR-DEPLOY-003 – Amazon S3 Images

Production Product Images phải lưu trên Amazon S3.

\---

### NFR-DEPLOY-004 – Application Hosting

Spring Boot và Next.js phải được deploy bằng AWS hosting service phù hợp.

Exact services:

```text

To be defined in System Architecture.

```

\---

# 33\. LOGGING REQUIREMENTS

Phải log/audit các event quan trọng:

```text

Authentication Error

Admin Role Change

User Block/Unblock

Order Creation

Order Status Change

Order Cancellation

Payment Record Creation

payOS Payment Creation

payOS Payment Creation Failure

payOS Webhook

Webhook Verification Failure

Payment Status Change

Payment Expiration

Late Payment Event

Inventory Adjustment

Inventory Reservation

Inventory Commit

Inventory Release

Inventory Restoration

Manual Refund Status

Notification Business Event

Unexpected Exception

```

Không log:

```text

JWT Secret

Full JWT

payOS Secret

AWS Credentials

Database Credentials

Sensitive Authentication Credential

```

Log phải có timestamp và reference/correlation phù hợp.

Business-facing timestamps khi hiển thị cho ADMIN phải tuân theo `BR-TIME-001`.

\---

# 34\. TESTING REQUIREMENTS

Testing phải bao phủ business-critical path.

\---

## 34.1. Business Time Tests

### TR-TIME-001 – Discount Business Timezone

Kiểm tra Discount activation/effectiveness theo:

```text

Asia/Ho\\\_Chi\\\_Minh

UTC+07:00

```

Bao gồm:

* before `startAt`;
* at `startAt`;
* within effective period;
* at `endAt`;
* after `endAt`.

Expected:

```text

Backend quyết định Discount effective/inactive

theo business timezone thống nhất.

```

\---

### TR-TIME-002 – Payment Expiration Authoritative Time

Given:

```text

Payment created at authoritative Backend timestamp

```

Expected:

```text

expiresAt = Backend payment creation time + 15 minutes

```

Frontend local clock thay đổi không được làm thay đổi authoritative expiration result.

Payment expiration display phải được diễn giải theo:

```text

Asia/Ho\\\_Chi\\\_Minh

```

\---

## 34.2. Authentication Tests

### TR-AUTH-001 – Google Login

Kiểm tra:

* valid Google credential;
* invalid Google credential;
* new User creation;
* existing User login.

\---

### TR-AUTH-002 – JWT Validation

Kiểm tra:

```text

Valid Token

Expired Token

Invalid Token

Missing Token

```

Expected đối với invalid authentication:

```text

HTTP 401

```

\---

### TR-AUTH-003 – Authorization

Kiểm tra:

* USER truy cập USER resource;
* USER truy cập ADMIN resource;
* ADMIN truy cập ADMIN resource.

Expected insufficient permission:

```text

HTTP 403

```

\---

### TR-AUTH-004 – Logout

Kiểm tra client token/session được remove khi logout.

\---

### TR-AUTH-005 – Current User Read-only Profile

Kiểm tra Current User trả đúng Google-based profile data và Version 1.3 không expose profile-edit operation.

\---

## 34.3. Admin Tests

### TR-ADMIN-001 – Bootstrap ADMIN

Email thuộc `ADMIN\\\_EMAILS` login lần đầu:

```text

role = ADMIN

```

Email khác:

```text

role = USER

```

Existing demoted bootstrap email không được tự re-promote.

\---

### TR-ADMIN-002 – User List

Kiểm tra:

* list;
* search;
* pagination;
* detail.

\---

### TR-ADMIN-003 – Promote

Kiểm tra:

```text

USER → ADMIN

```

\---

### TR-ADMIN-004 – Demote

Kiểm tra:

* demote khi còn ADMIN hợp lệ khác;
* reject demote ADMIN cuối cùng.

\---

### TR-ADMIN-005 – Block

Kiểm tra:

* block USER;
* block ADMIN khi còn ADMIN hợp lệ khác;
* reject block ADMIN cuối cùng.

\---

### TR-ADMIN-006 – Unblock

Kiểm tra:

```text

BLOCKED → ACTIVE

```

\---

### TR-ADMIN-007 – Concurrent Last Admin Protection

Concurrent demote/block operation không làm hệ thống mất toàn bộ ACTIVE ADMIN.

\---

### TR-ADMIN-008 – Initial ADMIN Bootstrap Valid

Given:

```text

Database chưa có ADMIN



ADMIN\\\_EMAILS =

admin@gmail.com

```

When:

```text

admin@gmail.com Google Login lần đầu

```

Expected:

```text

role = ADMIN

status = ACTIVE

```

\---

### TR-ADMIN-009 – Initial ADMIN Bootstrap Missing

Given:

```text

Database chưa có ADMIN

ADMIN\\\_EMAILS empty

```

Expected:

```text

Production configuration được xem là invalid

hoặc deployment/startup phát hiện configuration problem

theo implementation policy.

```

System không được silently operate indefinitely mà không có cách thiết lập ADMIN đầu tiên.

Exact fail-fast mechanism:

```text

To be defined in Implementation Specification.

```

\---

## 34.4. Product Tests

### TR-PRODUCT-001 – Product CRUD

Kiểm tra:

* create;
* update;
* enable/disable;
* soft-delete.

\---

### TR-PRODUCT-002 – Public Visibility

Chỉ:

```text

ACTIVE + deletedAt IS NULL

```

được public.

\---

### TR-PRODUCT-003 – Product Detail

Kiểm tra Product Detail trả:

* Product data;
* pricing;
* images;
* Collection;
* availability.

\---

### TR-PRODUCT-004 – Search and Filter

Kiểm tra:

```text

keyword

collectionId

minPrice

maxPrice

```

\---

### TR-PRODUCT-005 – Pagination and Sorting

Kiểm tra:

```text

page

size

price sort

createdAt sort

```

\---

### TR-PRODUCT-006 – Soft Delete Historical Safety

Soft-delete Product không làm mất historical OrderItem.

\---

### TR-PRODUCT-007 – Integer VND Base Price

Given:

```text

basePrice = 100000.5 VND

```

Expected:

```text

REJECT

```

Verify:

* Backend không persist Product có fractional `basePrice`;
* Backend không silently round fractional `basePrice`;
* Frontend input validation không phải security/business validation boundary;
* `Product.basePrice` phải thỏa `BR-MONEY-001`.

\---

## 34.5. Collection Tests

### TR-COLLECTION-001 – Collection Core

Kiểm tra:

* public active Collection List;
* admin create;
* update;
* disable;
* soft-delete;
* no cascade Product delete.

\---

### TR-COLLECTION-002 – Collection Referential Integrity

Reject delete operation không hợp lệ khi Collection còn được Product sử dụng.

\---

### TR-COLLECTION-003 – Collection Detail

Nếu Collection Detail được triển khai theo SHOULD requirement, kiểm tra public detail hoạt động đúng.

\---

## 34.6. Discount Tests

### TR-DISCOUNT-001 – Percentage

Kiểm tra valid percentage Discount và sellingPrice.

\---

### TR-DISCOUNT-002 – Fixed Price

Kiểm tra:

```text

sellingPrice = discountValue

```

\---

### TR-DISCOUNT-003 – Time Window

Kiểm tra time-window cùng `TR-TIME-001`.

\---

### TR-DISCOUNT-004 – Inactive Discount

```text

isActive = false

```

không được áp dụng.

\---

### TR-DISCOUNT-005 – Invalid Discount

Reject:

* invalid percentage;
* fixed price >= basePrice;
* `FIXED\\\_PRICE discountValue` có fractional VND;
* invalid time window;
* sellingPrice <= 0;
* overlapping effective Discount.

Fractional `FIXED\\\_PRICE` example:

```text

Given:

basePrice = 100000 VND
discountType = FIXED\\\_PRICE
discountValue = 89999.5

Expected:

REJECT

```

Không được round:

```text

89999.5 → 90000

```

Invalid `FIXED\\\_PRICE` input phải bị reject.

\---

### TR-DISCOUNT-006 – Price Snapshot

Discount thay đổi sau Order creation không thay đổi OrderItem cũ.

\---

### TR-DISCOUNT-007 – VND Monetary Rounding

Given:

```text

basePrice = 100001 VND
discountType = PERCENTAGE
discountValue = 15
quantity = 2

```

Expected:

```text

rawSellingPrice = 85000.85

sellingPrice = 85001 VND

OrderItem.totalPrice = 170002 VND

Nếu Order chỉ có line item này:

Order.totalAmount = 170002 VND

Payment.amount = 170002 VND

```

Verify:

```text

sellingPrice là số nguyên VND

HALF\\\_UP được áp dụng đúng

OrderItem.totalPrice dùng rounded sellingPrice

Order.totalAmount = SUM(OrderItem.totalPrice)

Payment.amount = Order.totalAmount

Frontend không override authoritative rounded value

```

\---

## 34.7. Image Tests

### TR-IMAGE-001 – Maximum Images

Không cho Product vượt:

```text

10 images

```

\---

### TR-IMAGE-002 – Invalid Image

Reject:

* unsupported file type;
* invalid MIME type;
* file vượt configured maximum size.

\---

### TR-IMAGE-003 – Single Thumbnail

Không cho Product có nhiều hơn một thumbnail.

\---

### TR-IMAGE-004 – Reorder/Delete

Kiểm tra reorder và delete.

\---

### TR-IMAGE-005 – Frontend Interaction

Kiểm tra:

```text

Zoom In

Zoom Out

Thumbnail Selection

Mobile Swipe

```

\---

### TR-IMAGE-006 – S3 Storage

Kiểm tra Product Image được lưu trên S3 và database chỉ lưu metadata/reference cần thiết.

\---

## 34.8. Cart Tests

### TR-CART-001 – Cart Operations

Kiểm tra:

* view;
* add;
* update quantity;
* remove.

\---

### TR-CART-002 – Cart Validation

Reject:

* quantity < 1;
* Product inactive;
* Product deleted;
* requested quantity > availableQuantity.

\---

### TR-CART-003 – Cart Price Trust Boundary

Client gửi fake price không làm thay đổi Backend-calculated price.

\---

### TR-CART-004 – Successful Checkout Cart Lifecycle

Sau successful checkout tới mức payOS Payment Information được tạo thành công:

```text

checked-out Cart Items removed

```

Nếu toàn bộ Cart được checkout:

```text

Cart empty

```

\---

### TR-CART-005 – Failed Checkout Cart Preservation

Nếu checkout thất bại trước successful payOS Payment Information creation:

```text

Cart Items retained

```

Bao gồm payOS creation failure.

\---

## 34.9. Inventory Tests

### TR-INV-001 – Import/Adjustment

Kiểm tra tăng/giảm quantity và reason.

\---

### TR-INV-002 – Invalid Adjustment

Không cho resulting Inventory vi phạm invariant.

\---

### TR-INV-003 – Reservation

Expected:

```text

physical quantity unchanged

reservedQuantity increases

```

\---

### TR-INV-004 – Release

Expected:

```text

physical quantity unchanged

reservedQuantity decreases

```

\---

### TR-INV-005 – Commit

Expected:

```text

physical quantity decreases

reservedQuantity decreases

```

\---

### TR-INV-006 – Insufficient Stock

Nếu requested quantity > availableQuantity:

```text

operation rejected

HTTP 409

```

\---

### TR-INV-007 – Concurrency

Hai checkout cùng tranh Product cuối cùng:

```text

only one reservation succeeds

```

\---

### TR-INV-008 – Low Stock

Kiểm tra:

```text

LOW\\\_STOCK:

availableQuantity <= lowStockThreshold

AND

availableQuantity > 0



OUT\\\_OF\\\_STOCK:

availableQuantity = 0

```

\---

### TR-INV-009 – Inventory Transaction History

Mỗi Inventory business operation quan trọng tạo history phù hợp.

\---

### TR-INV-010 – Reserved Quantity Audit

Đối với `RESERVE` và `RELEASE`, Transaction History phải xác định được:

```text

beforeQuantity

afterQuantity

beforeReservedQuantity

afterReservedQuantity

```

hoặc representation tương đương.

\---

### TR-INV-011 – SALE Audit

`SALE` phải audit được:

```text

physical quantity decrease

reserved quantity decrease

```

\---

### TR-INV-012 – CANCEL\_ORDER Audit

Paid Order cancellation phải audit được:

```text

physical quantity increase

reserved quantity unchanged

```

và không restore duplicate.

\---

## 34.10. Order Tests

### TR-ORDER-001 – Create Order

Kiểm tra:

* receiver information;
* Backend pricing;
* OrderItems;
* reservation;
* unique Order Code.

\---

### TR-ORDER-002 – My Orders

USER chỉ thấy Order của chính mình.

\---

### TR-ORDER-003 – Order Detail Authorization

USER A không được xem Order của USER B.

\---

### TR-ORDER-004 – Historical OrderItem Snapshot

Given Product:

```text

nameVi = "Bình gốm"
nameEn = "Pottery Vase"
basePrice = 100000

```

Create Order.

Historical OrderItem:

```text

productNameVi = "Bình gốm"
productNameEn = "Pottery Vase"

```

Sau đó ADMIN update Product:

```text

nameVi = "Bình gốm mới"
nameEn = "New Pottery Vase"
basePrice = 120000

```

Expected:

```text

OrderItem.productNameVi = "Bình gốm"

OrderItem.productNameEn = "Pottery Vase"

Historical basePrice unchanged

Historical sellingPrice unchanged

Historical totalPrice unchanged

```

Order Detail với locale `vi` phải sử dụng `OrderItem.productNameVi`; với locale `en` phải sử dụng `OrderItem.productNameEn`.

Current Product rename, disable hoặc soft-delete không được thay thế historical Product Name snapshot.

\---

### TR-ORDER-005 – Cancellation Transitions

Kiểm tra:

```text

NEW → CANCELLED

CONFIRMED → CANCELLED

```

theo Payment/Inventory cancellation rule.

\---

### TR-ORDER-006 – Invalid Final-State Transition

Reject:

```text

COMPLETED → NEW

COMPLETED → CANCELLED

CANCELLED → CONFIRMED

CANCELLED → COMPLETED

```

\---

### TR-ORDER-007 – USER Cannot Cancel

USER không có quyền cancel Order.

\---

### TR-ORDER-008 – ADMIN Cancel Pending Order

Expected:

```text

Order = CANCELLED

Payment = CANCELLED

Reservation = RELEASED exactly once

```

\---

### TR-ORDER-009 – ADMIN Cancel Paid Order

Expected:

```text

Order = CANCELLED

Payment = PAID

Physical Inventory restored exactly once

No automatic refund

```

\---

### TR-ORDER-010 – Duplicate Checkout

Retry cùng logical checkout operation không tạo duplicate Order.

\---

### TR-ORDER-011 – NEW + PAID → CONFIRMED

Input:

```text

OrderStatus = NEW

Payment.status = PAID

```

Expected:

```text

CONFIRMED

```

\---

### TR-ORDER-012 – NEW + PENDING → CONFIRMED

Expected:

```text

REJECT

```

\---

### TR-ORDER-013 – NEW + FAILED/EXPIRED/CANCELLED/REFUNDED → CONFIRMED

Expected:

```text

REJECT

```

\---

### TR-ORDER-014 – CONFIRMED + PAID → COMPLETED

Expected:

```text

COMPLETED

```

\---

### TR-ORDER-015 – CONFIRMED + non-PAID → COMPLETED

Expected:

```text

REJECT

```

\---

### TR-ORDER-016 – Payment Success Does Not Auto-confirm

Verified payOS success phải tạo state:

```text

Order = NEW

Payment = PAID

```

không tự:

```text

Order = CONFIRMED

```

\---

### TR-ORDER-017 – Order Payment Cardinality

Một Order không được có nhiều Payment record.

\---

### TR-ORDER-018 – Cancelled Order Cannot Reuse Payment

Order đã `CANCELLED` không được tạo Payment mới.

USER phải checkout mới để tạo new Order/new Payment.

\---

### TR-ORDER-019 – Order Payment Response Consistency

Order response `paymentStatus` phải bằng associated authoritative:

```text

Payment.status

```

\---

## 34.11. Payment Tests

### TR-PAY-001 – Create payOS Payment

Kiểm tra:

```text

Payment.amount = Backend Order.totalAmount

```

\---

### TR-PAY-002 – Valid Webhook

Valid verified success:

```text

Payment → PAID

Inventory commit exactly once

Order remains NEW

```

\---

### TR-PAY-003 – Invalid Webhook

Invalid verification không được thay đổi business state.

\---

### TR-PAY-004 – Duplicate Webhook

Duplicate Webhook không gây:

* duplicate commit;
* duplicate release;
* duplicate InventoryTransaction;
* duplicate Payment transition.

\---

### TR-PAY-005 – Failed Payment

Expected:

```text

Payment = FAILED

Order = CANCELLED

Reservation = RELEASED

```

\---

### TR-PAY-006 – Cancelled Payment

Expected:

```text

Payment = CANCELLED

Order = CANCELLED

Reservation = RELEASED

```

\---

### TR-PAY-007 – Payment Expiration

Sau 15 phút nếu vẫn `PENDING`:

```text

Payment = EXPIRED

Order = CANCELLED

Reservation = RELEASED

```

Expiration phải dựa trên authoritative Backend time.

\---

### TR-PAY-008 – Expiration Idempotency

Retry expiration processing không release Inventory nhiều lần.

\---

### TR-PAY-009 – Late Success Event

Late verified success event sau:

```text

FAILED

CANCELLED

EXPIRED

```

không được tự reopen Order hoặc commit released Inventory.

Event phải được audit/flag manual resolution.

\---

### TR-PAY-010 – payOS Creation Failure

Nếu payOS Payment Information creation thất bại và không thể tiếp tục:

```text

Payment = FAILED

Order = CANCELLED

Reservation = RELEASED

InventoryTransaction = RELEASE

```

Cart Item phải giữ nguyên.

`NEW\\\_ORDER` Notification không được tạo.

\---

### TR-PAY-011 – Payment Source of Truth

Mọi Order API paymentStatus phải match:

```text

Payment.status

```

\---

### TR-PAY-012 – One Order One Payment

Database/business logic phải reject second Payment record cho cùng Order.

\---

### TR-PAY-013 – External Payment Identifier Lifecycle

Given:

```text

Local Payment record created

Payment = PENDING

payOS external identifier chưa có

```

Expected:

```text

Payment record vẫn là logical state hợp lệ

externalTransactionIdentifier có thể unavailable/null

```

Sau khi payOS trả identifier phù hợp:

```text

system persists identifier

```

Nếu identifier tồn tại:

```text

consistency/idempotency protection applies

```

\---

## 34.12. Refund Tests

### TR-REFUND-001 – Valid Manual Refund

Input:

```text

Order = CANCELLED

Payment = PAID

External refund completed

```

Expected:

```text

Payment → REFUNDED

```

Inventory không thay đổi.

\---

### TR-REFUND-002 – NEW Paid Refund Rejected

Input:

```text

Order = NEW

Payment = PAID

```

Expected Manual Refund Recording:

```text

REJECT

```

\---

### TR-REFUND-003 – CONFIRMED Paid Refund Rejected

Input:

```text

Order = CONFIRMED

Payment = PAID

```

Expected:

```text

REJECT

```

\---

### TR-REFUND-004 – COMPLETED Paid Refund Rejected

Input:

```text

Order = COMPLETED

Payment = PAID

```

Expected:

```text

REJECT

```

\---

### TR-REFUND-005 – Refund Does Not Restore Inventory Twice

Manual Refund Recording sau paid Order cancellation không được:

```text

increase physical quantity again

release again

restore again

```

\---

## 34.13. Notification Tests

### TR-NOTIFY-001 – Notification Types

Kiểm tra hệ thống hỗ trợ:

```text

NEW\\\_ORDER

PAYMENT\\\_SUCCESS

PAYMENT\\\_FAILED

LOW\\\_STOCK

OUT\\\_OF\\\_STOCK

```

\---

### TR-NOTIFY-002 – Read/Unread

ADMIN có thể xem và mark Notification read.

\---

### TR-NOTIFY-003 – Real-time Notification

Nếu SSE được triển khai, kiểm tra delivery/reconnection phù hợp.

\---

### TR-NOTIFY-004 – Multi-Admin Recipient Read State

Given:

```text

Notification #100

Recipient ADMIN A

Recipient ADMIN B

```

When:

```text

ADMIN A marks Notification #100 read

```

Expected:

```text

ADMIN A = READ

ADMIN B = UNREAD

```

\---

### TR-NOTIFY-005 – Successful Checkout Creates NEW\_ORDER

Given successful checkout initiation:

```text

Order = NEW

Payment = PENDING

Inventory = RESERVED

payOS Payment Information created successfully

Cart Items processed

```

Expected:

```text

NEW\\\_ORDER Notification created

```

cho các ADMIN recipient phù hợp.

\---

### TR-NOTIFY-006 – Verified Payment Success Creates PAYMENT\_SUCCESS

Given verified payOS success:

```text

Payment → PAID

Inventory → COMMITTED

Order remains NEW

```

Expected:

```text

PAYMENT\\\_SUCCESS Notification created

```

\---

### TR-NOTIFY-007 – NEW\_ORDER and PAYMENT\_SUCCESS Are Separate Events

Given:

```text

successful checkout initiation

↓

NEW\\\_ORDER created



later

↓

verified payment success

```

Expected:

```text

PAYMENT\\\_SUCCESS created separately

```

Hai Notification phải phản ánh hai business event riêng biệt.

\---

### TR-NOTIFY-008 – Duplicate Payment Success Notification Protection

Given duplicate verified Webhook delivery cho cùng payment-success business event:

```text

Webhook #1

Webhook #2

Webhook #3

```

Expected:

```text

Payment business operation processed once

PAYMENT\\\_SUCCESS business Notification không bị duplicate không kiểm soát

```

\---

### TR-NOTIFY-009 – Unsuccessful Payment Notification

Given Payment kết thúc ở một trong:

```text

FAILED

CANCELLED

EXPIRED

```

và cleanup hoàn tất:

```text

Order = CANCELLED

Inventory = RELEASED

```

Expected:

```text

PAYMENT\\\_FAILED Notification created

```

với message/detail thể hiện Payment Status thực tế.

\---

### TR-NOTIFY-010 – Low Stock Transition Notification

Khi Product chuyển từ trạng thái không Low Stock vào:

```text

availableQuantity <= lowStockThreshold

AND

availableQuantity > 0

```

Expected:

```text

LOW\\\_STOCK Notification created

```

Không tạo lặp không kiểm soát khi inventory state không thay đổi.

\---

### TR-NOTIFY-011 – Out of Stock Transition Notification

Khi Product chuyển vào:

```text

availableQuantity = 0

```

Expected:

```text

OUT\\\_OF\\\_STOCK Notification created

```

Không tạo lặp không kiểm soát khi inventory state không thay đổi.

\---

## 34.14. Reporting Tests

### TR-REPORT-001 – Revenue

Revenue chỉ tính:

```text

COMPLETED + PAID

```

Payment `REFUNDED` không được tính effective Revenue.

Date range phải theo business timezone.

\---

### TR-REPORT-002 – Best Selling

Best Selling chỉ dựa trên:

```text

COMPLETED + PAID

```

\---

### TR-REPORT-003 – Dashboard

Kiểm tra:

```text

Total Orders

Total Revenue

Low Stock

Revenue Chart

Recent Orders

Best Selling

```

\---

### TR-REPORT-004 – New Orders Metric

Chỉ:

```text

Order = NEW

Payment = PAID

```

được tính vào `New Orders`.

Các Order `NEW` với Payment khác `PAID` không được tính.

\---

### TR-REPORT-005 – NEW\_ORDER Notification Does Not Equal Dashboard New Orders

Given:

```text

Order = NEW

Payment = PENDING

NEW\\\_ORDER Notification exists

```

Expected:

```text

Order không được tính vào Dashboard New Orders

```

Sau khi:

```text

Payment = PAID

Order remains NEW

```

Expected:

```text

Order được tính vào Dashboard New Orders

```

\---

## 34.15. Static Content and Localization Tests

### TR-CONTENT-001 – Static Content

Kiểm tra:

```text

FAQ

Policy

Contact

```

hiển thị đúng nội dung configured.

\---

### TR-CONTENT-002 – Contact Information

Kiểm tra Email, Phone, Facebook và Address đúng dữ liệu đã xác nhận.

\---

### TR-I18N-001 – Vietnamese/English UI

Kiểm tra:

* navigation;
* buttons;
* forms;
* validation display;
* Product localization;
* Collection localization.

\---

## 34.16. Security Tests

### TR-SEC-001 – HTTP Authentication Mapping

Kiểm tra:

```text

Unauthenticated → 401

Authenticated but insufficient permission → 403

```

\---

### TR-SEC-002 – Secret Protection

Secret không xuất hiện trong:

* production error response;
* application logs mẫu.

\---

### TR-SEC-003 – Backend RBAC

Protected ADMIN endpoint không thể bypass bằng client/frontend modification.

\---

## 34.17. Compatibility Tests

### TR-COMPAT-001 – Responsive

Kiểm tra:

```text

Desktop

Tablet

Mobile

```

\---

### TR-COMPAT-002 – Browser

Kiểm tra:

```text

Google Chrome

Microsoft Edge

Modern Mobile Browser

```

\---

## 34.18. Maintainability Tests

### TR-MAINT-001 – OpenAPI

Kiểm tra Swagger/OpenAPI thể hiện:

* endpoint;
* request;
* response;
* validation;
* security requirement.

\---

### TR-MAINT-002 – Migration

Kiểm tra database có thể provision/update qua migration process.

\---

## 34.19. Deployment Tests

### TR-DEPLOY-001 – AWS Application Deployment

Kiểm tra Spring Boot và Next.js production deployment hoạt động trên AWS architecture đã chọn.

\---

### TR-DEPLOY-002 – PostgreSQL Deployment

Kiểm tra application kết nối thành công tới PostgreSQL production trên AWS.

\---

### TR-DEPLOY-003 – S3 Product Image

Kiểm tra production Product Image upload/read hoạt động với Amazon S3.

\---

### TR-DEPLOY-004 – Cloud Logging

Kiểm tra production application log có thể quan sát bằng CloudWatch architecture đã chọn.

\---

# 35\. PRIORITY

## 35.1. MUST HAVE

```text

Google Login

JWT

USER / ADMIN

Admin Bootstrap

Initial ADMIN\\\_EMAILS Bootstrap Validation



User List

User Search

User Pagination

User Detail

Promote/Demote

Block/Unblock

Last ADMIN Protection



Business Timezone

Consistent Business Time Interpretation

Backend-authoritative Payment Expiration Time



Product

Product Detail

Product Search

Product Filter

Product Pagination

Product Sorting

Product Soft Delete



Collection Public List

Collection Admin Management



Simple Product Discount

Percentage Discount

Fixed Price Discount

Discount Enable/Disable

Discount Time Window

Backend Selling Price Calculation

VND Monetary \\\& HALF\\\_UP Rounding



Product Images

Maximum 10 Images

Single Thumbnail

Amazon S3

Image Zoom

Thumbnail Selection

Mobile Swipe



Cart

Backend Cart Pricing

Successful Checkout Cart Lifecycle

Failed Checkout Cart Preservation



Inventory

Inventory List/Search/Pagination

Inventory Adjustment

Inventory Transaction History

Reserved Quantity Audit

Inventory Reservation

Inventory Commit

Inventory Release

Paid Order Inventory Restoration

Concurrency Safety

Low Stock

15-minute Reservation Expiration



Order

OrderItem

Receiver Information

Unique Order Code

Historical OrderItem Snapshot

My Orders

User Order Detail

Admin Order Management

Order Payment Preconditions

Order Status Validation

Admin Cancellation

USER Cannot Cancel

Checkout Idempotency

One Order One Payment



payOS

Payment Record

Payment Status Source of Truth

External Payment Identifier Lifecycle

payOS Payment Creation

payOS Creation Failure Consistency

Payment Webhook Verification

Payment Success Handling

Payment Failure Handling

Payment Cancellation Handling

Payment Expiration

Payment Idempotency

Late Success Event Safety



Manual Refund Eligibility

Manual Refund Recording

Manual Refund Inventory Isolation



Notification Database

NEW\\\_ORDER Trigger

PAYMENT\\\_SUCCESS Trigger

PAYMENT\\\_FAILED Semantics

Low Stock / Out of Stock Notification Trigger

Per-ADMIN Notification Read State

Notification Event Deduplication

Admin Notification Bell Data



Dashboard

Revenue

New Orders = NEW + PAID

Recent Orders

Best Selling Products

Low Stock Products



Vietnamese / English



Static FAQ

Static Policy

Contact Information



Responsive Website



AWS Deployment

PostgreSQL on AWS

Amazon S3

Application Logging

CloudWatch-compatible Observability

OpenAPI

Database Migration

```

\---

## 35.2. SHOULD HAVE

```text

Public Collection Detail

SSE Real-time Notification

Image Compression

Extended Reporting

```

Image Compression nếu triển khai phải bảo đảm hình ảnh vẫn phù hợp cho Product display.

Exact compression strategy:

```text

To be defined in Implementation Specification.

```

\---

## 35.3. COULD HAVE

```text

Advanced Analytics

Email Notification

Advanced Search Features

Additional Reporting Visualization

```

\---

# 36\. OUT OF SCOPE

Version 1.3 không bao gồm:

```text

Microservices



Marketplace

Multi-vendor

Multi-store



Multiple Warehouses

Supplier Management

Purchase Order

Accounting

ERP

Employee Management



Seller Role

Staff Role

Manager Role

Shipper Role

Delivery Driver Role



Product Variant

ProductVariant

Variant SKU

Size

Color



COD

MoMo

VNPay

Multiple Payment Gateways



Shipping Fee

shippingFee

Shipping Management

Shipping Module

Shipping Provider Integration

Shipping API

Delivery Tracking

Shipping Status

SHIPPING Order Status

Shipping Dashboard



USER Self Cancel



Automatic Refund

Automatic payOS Refund Integration



Multiple Payment Attempts per Order

Retry Payment on Cancelled Order



Completed Order Return Workflow

Completed Order Refund Workflow



User Profile Editing

Change Email

Change Avatar

Change Google Identity



Coupon

Voucher

Promotion Code

Complex Promotion Engine

Promotion Campaign Engine



AI Recommendation

Live Chat Backend



FAQ Admin CRUD

Policy Admin CRUD

Contact Admin CRUD

```

Manual Refund Recording cho cancelled paid Order vẫn thuộc active scope.

\---

# 37\. ASSUMPTIONS

1. Website thuộc một cửa hàng duy nhất.
2. User authentication sử dụng Google Login.
3. Actors chỉ gồm:

```text

GUEST

USER

ADMIN

```

4. Application role chỉ gồm:

```text

USER

ADMIN

```

5. Initial ADMIN bootstrap sử dụng:

```text

ADMIN\\\_EMAILS

```

6. Sau bootstrap, `User.role` và `User.status` trong database là runtime source of truth.
7. Hệ thống luôn phải có ít nhất một:

```text

ACTIVE ADMIN

```

sau khi runtime admin state đã được thiết lập.

8. Product không có variant/size/color.
9. Mỗi Product có một Inventory.
10. Giá sử dụng:

```text

VND

```

11. Order total chỉ gồm tổng OrderItem theo current scope.
12. Payment Gateway duy nhất là:

```text

payOS

```

13. Payment `PENDING` timeout sau:

```text

15 minutes

```

14. USER không được tự cancel Order.
15. Refund được thực hiện thủ công ngoài application.
16. Application chỉ ghi nhận `REFUNDED` khi Manual Refund Eligibility được đáp ứng và refund thực tế đã thành công.
17. FAQ, Policy và Contact là static content.
18. Mỗi Product tối đa:

```text

10 images

```

19. Website dự kiến khoảng:

```text

<= 100 Product Images

```

Đây là capacity assumption, không phải hard limit toàn hệ thống.

20. AWS được sử dụng cho production deployment.
21. PostgreSQL được sử dụng làm relational database.
22. Amazon S3 được sử dụng để lưu Product Image.
23. Frontend sử dụng Next.js.
24. Backend sử dụng Spring Boot.
25. Hệ thống dùng stateless JWT access-token-only trong Version 1.3.
26. Refresh Token không thuộc Version 1.3.
27. Logout chủ yếu loại bỏ authenticated session/token phía client theo access-token-only design.
28. Runtime role/status authorization phải phản ánh database authoritative state.
29. Nếu FAQ/Policy chỉ được cung cấp bằng một ngôn ngữ, hệ thống không tự tạo business translation.
30. Exact locking mechanism, transaction boundary với payOS, idempotency storage và scheduler implementation được xác định trong Implementation Specification.
31. Một Order có đúng một Payment trong Version 1.3.
32. Nếu Payment `FAILED`, `CANCELLED` hoặc `EXPIRED`, USER phải thực hiện checkout mới để mua lại; Order cũ không được reuse cho Payment mới.
33. User Profile Editing không thuộc Version 1.3.
34. Notification read/unread state được quản lý riêng theo từng ADMIN recipient.
35. Dashboard `New Orders` đại diện cho:

```text

OrderStatus = NEW

AND

Payment.status = PAID

```

36. Post-sale return/refund sau Order `COMPLETED` không thuộc Version 1.3.
37. Payment success không tự động confirm Order.
38. `Payment.status` là authoritative Payment State.
39. Order API có thể expose `paymentStatus` dưới dạng derived value từ associated Payment.
40. Khi successful checkout đã tạo được payOS Payment Information, checked-out CartItem được remove khỏi Cart.
41. Nếu checkout thất bại trước successful payOS Payment Information creation, CartItem được giữ nguyên.
42. Manual Refund Recording không làm Inventory thay đổi lần hai.
43. Late verified success event sau failed/cancelled/expired Payment cần audit/manual resolution, không tự reopen Order.
44. Business timezone là:

```text

Asia/Ho\\\_Chi\\\_Minh

UTC+07:00

```

45. Timestamp persistence strategy được quyết định trong Implementation Specification, nhưng business date/time phải được hiển thị và diễn giải nhất quán.
46. Initial production bootstrap phải có ít nhất một valid Google email trong `ADMIN\\\_EMAILS` nếu database chưa có ADMIN hợp lệ.
47. `NEW\\\_ORDER` Notification được tạo sau successful checkout initiation và không đồng nghĩa Payment đã `PAID`.
48. Dashboard `New Orders` vẫn chỉ bao gồm:

```text

NEW + PAID

```

49. `externalTransactionIdentifier` có thể chưa tồn tại trước khi payOS trả external payment identifier hợp lệ.
50. `PAYMENT\\\_FAILED` Notification đại diện cho unsuccessful terminal Payment Status:

```text

FAILED

CANCELLED

EXPIRED

```

và message/detail phải phản ánh status thực tế.

51. Notification cho LOW\_STOCK và OUT\_OF\_STOCK phải được tạo
khi Inventory chuyển vào trạng thái tương ứng.

Notification không được tạo lặp không kiểm soát
khi Inventory vẫn duy trì cùng trạng thái.
---

# 38\. ACCEPTANCE CRITERIA

## 38.1. Business Time

```text

✓ Business timezone là Asia/Ho\\\_Chi\\\_Minh (UTC+07:00).



✓ Discount startAt/endAt được đánh giá theo business-time rule thống nhất.



✓ Backend là source of truth cho Discount activation time.



✓ Payment expiration 15 phút dựa trên authoritative Backend timestamp.



✓ Frontend local clock không quyết định Payment expiration.



✓ Business-facing Order timestamps được diễn giải theo Asia/Ho\\\_Chi\\\_Minh.



✓ Dashboard date filter và Revenue date range sử dụng business timezone nhất quán.



✓ Recent Orders display sử dụng business timezone nhất quán.

```

\---

## 38.2. Authentication \& Authorization

```text

✓ Google Login hoạt động.



✓ Backend verify Google credential.



✓ New Google User được tạo tự động.



✓ JWT chứa userId, email, role, issuedAt, expiration.



✓ Valid Token truy cập protected resource hợp lệ.



✓ Expired Token bị từ chối với HTTP 401.



✓ Invalid Token bị từ chối với HTTP 401.



✓ Missing Token ở protected API bị từ chối với HTTP 401.



✓ Authenticated User thiếu quyền nhận HTTP 403.



✓ Logout loại bỏ authenticated session phía client.



✓ USER không truy cập được ADMIN API.



✓ User Profile chỉ read theo scope Version 1.3.



✓ Không có User Profile Editing trong Version 1.3.

```

\---

## 38.3. Admin Bootstrap \& User Management

```text

✓ Initial production bootstrap yêu cầu ADMIN\\\_EMAILS có ít nhất một valid Google email nếu database chưa có ADMIN.



✓ Initial zero-admin + empty ADMIN\\\_EMAILS được phát hiện là configuration problem.



✓ System không silently operate indefinitely mà không có cách tạo initial ADMIN.



✓ ADMIN bootstrap từ ADMIN\\\_EMAILS.



✓ First bootstrap ADMIN có role = ADMIN và status = ACTIVE.



✓ Existing bootstrap email không tự re-promote sau khi database role đã bị demote.



✓ Database role/status vẫn là runtime source of truth.



✓ ADMIN xem được User List.



✓ ADMIN search User được.



✓ User List có pagination.



✓ ADMIN xem User Detail.



✓ ADMIN promote USER thành ADMIN.



✓ ADMIN demote ADMIN khi vẫn còn ADMIN hợp lệ khác.



✓ Không thể demote ADMIN cuối cùng.



✓ ADMIN block User.



✓ ADMIN unblock User.



✓ Không thể block ADMIN cuối cùng.



✓ Concurrent role/status operation không làm mất toàn bộ ACTIVE ADMIN.

```

\---

## 38.4. Product

```text

✓ ADMIN tạo Product.



✓ ADMIN cập nhật Product.



✓ Product basePrice phải > 0.



✓ Product basePrice phải là số nguyên VND.



✓ Product basePrice có fractional VND bị reject.



✓ ADMIN enable/disable Product.



✓ ADMIN soft-delete Product.



✓ Soft Delete sử dụng deletedAt.



✓ Không dùng DELETED ProductStatus.



✓ Public chỉ thấy ACTIVE Product chưa soft-delete.



✓ Product Detail hoạt động.



✓ Product keyword search hoạt động.



✓ Product collectionId filter hoạt động.



✓ Product minPrice/maxPrice filter hoạt động.



✓ Product pagination hoạt động.



✓ Product sorting theo price hoạt động.



✓ Product sorting theo createdAt hoạt động.



✓ Backend không yêu cầu frontend tải toàn bộ Product để filter.

```

\---

## 38.5. Collection

```text

✓ Public Collection List hoạt động.



✓ Public chỉ thấy ACTIVE Collection chưa soft-delete.



✓ ADMIN tạo Collection.



✓ ADMIN cập nhật Collection.



✓ ADMIN enable/disable Collection.



✓ ADMIN soft-delete Collection theo business rule.



✓ Không hard-delete Collection đang được Product sử dụng.



✓ Không cascade delete Product.



✓ Collection Detail chỉ là SHOULD requirement.

```

\---

## 38.6. Discount \& Price

```text

✓ ADMIN cấu hình PERCENTAGE Discount.



✓ ADMIN cấu hình FIXED\\\_PRICE Discount.



✓ ADMIN cập nhật Discount.



✓ ADMIN enable Discount.



✓ ADMIN disable Discount.



✓ ADMIN remove Discount.



✓ PERCENTAGE validation đúng.



✓ FIXED\\\_PRICE validation đúng.



✓ FIXED\\\_PRICE discountValue phải là số nguyên VND.



✓ FIXED\\\_PRICE discountValue có fractional VND bị reject.



✓ PERCENTAGE discountValue không bị ép thành integer nếu vẫn thỏa 0 < discountValue < 100.



✓ PERCENTAGE monetary result vẫn sử dụng HALF\\\_UP theo BR-MONEY-001.



✓ startAt/endAt validation đúng.



✓ Discount time sử dụng Asia/Ho\\\_Chi\\\_Minh business semantics.



✓ Discount chỉ effective trong time window hợp lệ.



✓ Inactive Discount không được áp dụng.



✓ Không có nhiều effective Discount đồng thời trên một Product.



✓ Backend tính sellingPrice.



✓ Frontend không quyết định authoritative sellingPrice.



✓ Backend tính OrderItem total.



✓ Backend tính Order total.



✓ Currency authoritative của hệ thống là VND.



✓ basePrice, sellingPrice, OrderItem.totalPrice, Order.totalAmount và Payment.amount là số nguyên VND.



✓ PERCENTAGE Discount có fractional result được HALF\\\_UP về đơn vị 1 VND.



✓ OrderItem.totalPrice sử dụng sellingPrice đã round.



✓ Order.totalAmount bằng tổng các OrderItem.totalPrice.



✓ Payment.amount bằng chính xác Order.totalAmount.



✓ Product, Cart, Checkout, Order và Payment sử dụng cùng một rounding rule.



✓ Frontend không được override Backend monetary calculation.



✓ Product/Discount thay đổi không thay đổi historical OrderItem.

```

\---

## 38.7. Product Images

```text

✓ Product tối đa 10 images.



✓ Product tối đa 1 thumbnail.



✓ JPEG được hỗ trợ.



✓ PNG được hỗ trợ.



✓ WebP được hỗ trợ.



✓ Invalid file type bị reject.



✓ MIME type được validate.



✓ Configured maximum file size được validate.



✓ Images được lưu trên Amazon S3.



✓ PostgreSQL không lưu image binary.



✓ ADMIN upload image.



✓ ADMIN delete image.



✓ ADMIN reorder image.



✓ ADMIN set thumbnail.



✓ Product Detail hỗ trợ Zoom In.



✓ Product Detail hỗ trợ Zoom Out.



✓ Product Detail hỗ trợ Thumbnail Selection.



✓ Product Detail hỗ trợ Mobile Swipe.

```

\---

## 38.8. Cart

```text

✓ USER có Cart riêng.



✓ USER xem Cart.



✓ USER add item.



✓ USER update quantity.



✓ USER remove item.



✓ quantity < 1 bị reject.



✓ Inactive/deleted Product không được thêm hoặc checkout.



✓ Backend validate availableQuantity.



✓ Add Cart không reserve Inventory.



✓ Backend tính lại Cart pricing.



✓ Client fake price không override Backend price.



✓ Successful checkout remove các CartItem đã checkout.



✓ Nếu checkout toàn bộ Cart, Cart trở thành empty.



✓ Failed checkout trước payOS Payment Information success giữ nguyên CartItem.



✓ payOS creation failure không làm mất CartItem.



✓ Checkout idempotency vẫn hoạt động độc lập với Cart clearing.

```

\---

## 38.9. Inventory

```text

✓ Mỗi Product có một Inventory.



✓ ADMIN xem Inventory List.



✓ ADMIN search Inventory.



✓ Inventory List có pagination.



✓ ADMIN xem Quantity.



✓ ADMIN xem Reserved Quantity.



✓ ADMIN xem Available Quantity.



✓ ADMIN xem Low Stock Threshold.



✓ ADMIN điều chỉnh tăng quantity.



✓ ADMIN điều chỉnh giảm quantity.



✓ Adjustment yêu cầu reason.



✓ Không cho Inventory invariant âm.



✓ Inventory Transaction History hoạt động.



✓ Inventory Transaction audit được physical quantity.



✓ Inventory Transaction audit được reservedQuantity.



✓ RESERVE giữ physical quantity không đổi.



✓ RESERVE tăng reservedQuantity.



✓ RELEASE giữ physical quantity không đổi.



✓ RELEASE giảm reservedQuantity.



✓ PAID commit Inventory đúng một lần.



✓ SALE giảm physical quantity và reservedQuantity.



✓ CANCEL\\\_ORDER tăng physical quantity và không thay đổi reservedQuantity.



✓ Không restore Inventory hai lần.



✓ Concurrent checkout không oversell.



✓ Nếu chỉ còn một Product thì chỉ một concurrent checkout reserve thành công.



✓ Low Stock hoạt động theo availableQuantity > 0 và <= threshold.



✓ Out of Stock hoạt động khi availableQuantity = 0.

```

\---

## 38.10. Order

```text

✓ USER tạo Order.



✓ Order lưu receiverName.



✓ Order lưu phone.



✓ Order lưu email.



✓ Order lưu address.



✓ Order lưu note.



✓ Mỗi Order có orderCode unique.



✓ OrderItem snapshot productId.



✓ OrderItem snapshot productNameVi.



✓ OrderItem snapshot productNameEn.



✓ Product rename sau Order creation không thay đổi historical productNameVi/productNameEn.



✓ Product price/discount change sau Order creation không thay đổi historical OrderItem pricing.



✓ Order Detail sử dụng historical Product name phù hợp current locale.



✓ OrderItem snapshot basePrice.



✓ OrderItem snapshot sellingPrice.



✓ OrderItem snapshot quantity.



✓ OrderItem snapshot totalPrice.



✓ Order total bằng tổng OrderItem.



✓ USER xem My Orders.



✓ USER xem Order Detail của chính mình.



✓ USER không xem được Order của User khác.



✓ USER không được tự cancel Order.



✓ ADMIN xem tất cả Order.



✓ ADMIN search Order.



✓ ADMIN filter Order Status.



✓ ADMIN filter Payment Status.



✓ ADMIN filter date range.



✓ Order List có pagination.



✓ ADMIN xem Order Detail.



✓ NEW Order chỉ CONFIRMED khi Payment = PAID.



✓ NEW + PENDING không được CONFIRMED.



✓ NEW + FAILED không được CONFIRMED.



✓ NEW + CANCELLED không được CONFIRMED.



✓ NEW + EXPIRED không được CONFIRMED.



✓ NEW + REFUNDED không được CONFIRMED.



✓ CONFIRMED Order chỉ COMPLETED khi Payment = PAID.



✓ CONFIRMED + non-PAID không được COMPLETED.



✓ Payment Success không tự động chuyển Order sang CONFIRMED.



✓ NEW → CANCELLED hoạt động theo cancellation rule.



✓ CONFIRMED → CANCELLED hoạt động theo cancellation rule.



✓ COMPLETED → CANCELLED bị reject.



✓ CANCELLED → CONFIRMED bị reject.



✓ CANCELLED → COMPLETED bị reject.



✓ Duplicate logical checkout không tạo duplicate Order.



✓ Một Order chỉ có một Payment trong Version 1.3.



✓ Order CANCELLED không được reuse để tạo Payment mới.



✓ Payment failed/cancelled/expired muốn mua lại phải tạo checkout mới.

```

\---

## 38.11. Payment

```text

✓ Payment Gateway active duy nhất là payOS.



✓ Payment amount do Backend lấy từ Order total.



✓ Một Order chỉ tạo một Payment record.



✓ Local Payment record có thể tồn tại trước khi externalTransactionIdentifier có giá trị.



✓ externalTransactionIdentifier được lưu khi payOS cung cấp identifier phù hợp.



✓ Identifier tồn tại phải có consistency/idempotency protection phù hợp.



✓ Backend tạo payOS Payment Information.



✓ Frontend không tự set Payment = PAID.



✓ Backend verify payOS Webhook.



✓ Invalid Webhook không làm thay đổi business state.



✓ Valid success Webhook cập nhật Payment = PAID.



✓ Payment Success giữ Order = NEW.



✓ PAID commit Inventory đúng một lần.



✓ Duplicate Webhook không commit Inventory nhiều lần.



✓ Duplicate Webhook không release Inventory nhiều lần.



✓ Duplicate Webhook không tạo duplicate InventoryTransaction.



✓ Failed Payment làm Order CANCELLED.



✓ Failed Payment release reserved Inventory.



✓ Cancelled Payment làm Order CANCELLED.



✓ Cancelled Payment release reserved Inventory.



✓ Payment PENDING quá 15 phút chuyển EXPIRED.



✓ EXPIRED dùng authoritative Backend time.



✓ EXPIRED làm Order CANCELLED.



✓ EXPIRED release Inventory.



✓ Expiration processing idempotent.



✓ payOS creation failure làm Payment FAILED.



✓ payOS creation failure làm Order CANCELLED.



✓ payOS creation failure release reservation.



✓ payOS creation failure được log.



✓ Late success event không tự reopen Order.



✓ Late success event không tự commit released Inventory.



✓ Payment.status là authoritative payment state.



✓ Order response paymentStatus không contradiction Payment.status.

```

\---

## 38.12. Order Cancellation \& Manual Refund

```text

✓ ADMIN cancel pending Order release reserved Inventory đúng một lần.



✓ ADMIN cancel paid NEW/CONFIRMED Order không tự động refund.



✓ Paid Order cancellation restore committed Inventory đúng một lần.



✓ Paid Order cancellation tạo InventoryTransaction phù hợp.



✓ CANCELLED + PAID được phép tồn tại tạm thời trong manual refund flow.



✓ Manual Refund chỉ được record khi Order = CANCELLED và Payment = PAID.



✓ NEW + PAID không được trực tiếp REFUNDED.



✓ CONFIRMED + PAID không được trực tiếp REFUNDED.



✓ COMPLETED + PAID không được REFUNDED trong Version 1.3.



✓ Sau external refund thành công, ADMIN có thể record Payment = REFUNDED.



✓ USER không tự update refund.



✓ Manual Refund được audit/log.



✓ Manual Refund không restore Inventory lần hai.



✓ Manual Refund không release Inventory lần hai.



✓ Manual Refund không tạo duplicate Inventory restoration.



✓ COMPLETED Order không được cancel trong Version 1.3.

```

\---

## 38.13. Notification

```text

✓ NEW\\\_ORDER Notification hoạt động.



✓ Successful checkout initiation tạo NEW\\\_ORDER Notification.



✓ NEW\\\_ORDER có thể tồn tại khi Payment vẫn PENDING.



✓ NEW\\\_ORDER không đồng nghĩa Payment = PAID.



✓ Verified Payment Success tạo PAYMENT\\\_SUCCESS Notification.



✓ Payment Success Notification chỉ được tạo sau verified payOS success và Inventory commit.



✓ NEW\\\_ORDER và PAYMENT\\\_SUCCESS là hai business events riêng.



✓ Một checkout có thể tạo NEW\\\_ORDER, sau đó tạo PAYMENT\\\_SUCCESS.



✓ Duplicate Webhook không tạo duplicate PAYMENT\\\_SUCCESS Notification không kiểm soát.



✓ PAYMENT\\\_FAILED Notification hoạt động cho FAILED/CANCELLED/EXPIRED theo semantics đã chốt.



✓ PAYMENT\\\_FAILED message/detail phản ánh Payment Status thực tế.



✓ LOW\\\_STOCK Notification hoạt động.



✓ OUT\\\_OF\\\_STOCK Notification hoạt động.



✓ Notification không bị tạo lặp không kiểm soát cho unchanged inventory state.



✓ ADMIN xem Notification.



✓ ADMIN phân biệt read/unread.



✓ ADMIN mark Notification read.



✓ Notification read state độc lập theo từng ADMIN.



✓ ADMIN A đọc Notification không làm ADMIN B tự chuyển read.

```

\---

## 38.14. Reporting

```text

✓ Dashboard hiển thị Total Orders.



✓ Dashboard hiển thị Total Revenue.



✓ Dashboard hiển thị New Orders.



✓ Dashboard hiển thị Low Stock Products.



✓ Dashboard hiển thị Revenue Chart.



✓ Dashboard hiển thị Recent Orders.



✓ Dashboard hiển thị Best Selling Products.



✓ Revenue chỉ dựa trên COMPLETED + PAID.



✓ Refunded Payment không tính effective Revenue.



✓ Best Selling chỉ dựa trên COMPLETED + PAID.



✓ New Orders Dashboard chỉ tính NEW + PAID.



✓ NEW + PENDING không được tính New Orders.



✓ NEW + FAILED/CANCELLED/EXPIRED không được tính New Orders.



✓ Việc có NEW\\\_ORDER Notification không tự làm Order được tính vào Dashboard New Orders.



✓ Dashboard/Revenue date semantics sử dụng Asia/Ho\\\_Chi\\\_Minh.

```

\---

## 38.15. Localization \& Static Content

```text

✓ Navigation hỗ trợ VI/EN.



✓ Button label hỗ trợ VI/EN.



✓ Form label hỗ trợ VI/EN.



✓ Basic validation display hỗ trợ locale.



✓ Product hỗ trợ VI/EN.



✓ Collection hỗ trợ VI/EN.



✓ FAQ static hoạt động.



✓ Policy static hoạt động.



✓ Contact Page/Footer hiển thị Contact Information đã xác nhận.

```

\---

## 38.16. Compatibility \& Deployment

```text

✓ Website responsive trên Desktop.



✓ Website responsive trên Tablet.



✓ Website responsive trên Mobile.



✓ Website hoạt động trên Chrome.



✓ Website hoạt động trên Edge.



✓ Website hoạt động trên modern mobile browser.



✓ Next.js được deploy trên AWS.



✓ Spring Boot được deploy trên AWS.



✓ PostgreSQL production chạy trên AWS relational database service phù hợp.



✓ Product Images chạy trên Amazon S3.



✓ Application Logging hoạt động.



✓ Production logs có thể được quan sát trên CloudWatch architecture.



✓ Swagger/OpenAPI hoạt động.



✓ Database migration process hoạt động.

```

\---

# 39\. REQUIREMENT TRACEABILITY MATRIX

|Requirement ID|Module|Actor|Business Rule / Entity / Data|Priority|Test Reference|
|-|-|-|-|-|-|
|BR-TIME-001|Business Time|SYSTEM|Asia/Ho\_Chi\_Minh business-time semantics|MUST|TR-TIME-001, TR-TIME-002|
|BR-MONEY-001|Pricing|SYSTEM|VND integer monetary values + HALF\_UP rounding consistency|MUST|TR-DISCOUNT-007, TR-PRODUCT-007, TR-DISCOUNT-005|
|FR-AUTH-001|Authentication|USER|Google credential verified by Backend|MUST|TR-AUTH-001|
|FR-AUTH-002|Authentication|USER|Automatic User creation|MUST|TR-AUTH-001|
|FR-AUTH-003|Authentication|USER|JWT minimum claims|MUST|TR-AUTH-001|
|FR-AUTH-004|Authentication|USER/ADMIN|Valid/expired/invalid/missing token handling|MUST|TR-AUTH-002|
|FR-AUTH-005|Authorization|USER/ADMIN|Database role/status authoritative at runtime|MUST|TR-AUTH-003, TR-SEC-003|
|FR-AUTH-006|Authentication|USER/ADMIN|Current User read-only profile|MUST|TR-AUTH-005|
|FR-AUTH-007|Authentication|USER/ADMIN|Stateless access-token logout|MUST|TR-AUTH-004|
|FR-ADMIN-001|Admin Bootstrap|ADMIN|ADMIN\_EMAILS only bootstraps initial role|MUST|TR-ADMIN-001, TR-ADMIN-008|
|FR-ADMIN-002|User Management|ADMIN|User list/search/pagination/detail|MUST|TR-ADMIN-002|
|FR-ADMIN-003|User Management|ADMIN|USER → ADMIN|MUST|TR-ADMIN-003|
|FR-ADMIN-004|User Management|ADMIN|ADMIN → USER; last ADMIN protected|MUST|TR-ADMIN-004, TR-ADMIN-007|
|FR-ADMIN-005|User Management|ADMIN|ACTIVE → BLOCKED; last ADMIN protected|MUST|TR-ADMIN-005, TR-ADMIN-007|
|FR-ADMIN-006|User Management|ADMIN|BLOCKED → ACTIVE|MUST|TR-ADMIN-006|
|FR-ADMIN-007|Admin Bootstrap|SYSTEM/ADMIN|Initial production requires path to first ADMIN|MUST|TR-ADMIN-008, TR-ADMIN-009|
|FR-PRODUCT-001|Product|GUEST/USER|Public ACTIVE + not deleted|MUST|TR-PRODUCT-002|
|FR-PRODUCT-002|Product|GUEST/USER|Product Detail + price + images + availability|MUST|TR-PRODUCT-003|
|FR-PRODUCT-003|Product|GUEST/USER|keyword/collectionId/minPrice/maxPrice|MUST|TR-PRODUCT-004|
|FR-PRODUCT-004|Product|GUEST/USER|Pagination + price/createdAt sorting|MUST|TR-PRODUCT-005|
|FR-PRODUCT-005|Product|ADMIN|Admin Product List/Search/Pagination|MUST|TR-PRODUCT-001|
|FR-PRODUCT-006|Product|ADMIN|Create Product + positive integer VND basePrice|MUST|TR-PRODUCT-001, TR-PRODUCT-007|
|FR-PRODUCT-007|Product|ADMIN|Update Product + integer VND basePrice without historical mutation|MUST|TR-PRODUCT-001, TR-PRODUCT-006, TR-PRODUCT-007|
|FR-PRODUCT-008|Product|ADMIN|ACTIVE ↔ INACTIVE|MUST|TR-PRODUCT-001, TR-PRODUCT-002|
|FR-PRODUCT-009|Product|ADMIN|Soft Delete using deletedAt|MUST|TR-PRODUCT-001, TR-PRODUCT-006|
|FR-COLLECTION-001|Collection|GUEST/USER|Public Collection List|MUST|TR-COLLECTION-001|
|FR-COLLECTION-002|Collection|GUEST/USER|Optional public Collection Detail|SHOULD|TR-COLLECTION-003|
|FR-COLLECTION-003|Collection|ADMIN|Admin list/search/pagination|MUST|TR-COLLECTION-001|
|FR-COLLECTION-004|Collection|ADMIN|Create Collection|MUST|TR-COLLECTION-001|
|FR-COLLECTION-005|Collection|ADMIN|Update Collection|MUST|TR-COLLECTION-001|
|FR-COLLECTION-006|Collection|ADMIN|Enable/Disable without Product cascade|MUST|TR-COLLECTION-001|
|FR-COLLECTION-007|Collection|ADMIN|Soft Delete + referential integrity|MUST|TR-COLLECTION-002|
|FR-DISCOUNT-001|Discount|ADMIN|Configure Simple Discount|MUST|TR-DISCOUNT-001, TR-DISCOUNT-002|
|FR-DISCOUNT-002|Discount|ADMIN|Update valid Discount|MUST|TR-DISCOUNT-005|
|FR-DISCOUNT-003|Discount|ADMIN|Enable/Disable Discount|MUST|TR-DISCOUNT-004|
|FR-DISCOUNT-004|Discount|ADMIN|Remove Discount without historical mutation|MUST|TR-DISCOUNT-006|
|FR-DISCOUNT-005|Pricing|SYSTEM|Backend effective sellingPrice using business time and VND rounding|MUST|TR-DISCOUNT-001, TR-DISCOUNT-002, TR-DISCOUNT-003, TR-DISCOUNT-007, TR-TIME-001|
|FR-IMAGE-001|Product Image|GUEST/USER|Ordered Product Image metadata|MUST|TR-IMAGE-004|
|FR-IMAGE-002|Product Image|ADMIN|Upload + maximum image validation|MUST|TR-IMAGE-001, TR-IMAGE-002|
|FR-IMAGE-003|Product Image|ADMIN|Delete Image|MUST|TR-IMAGE-004|
|FR-IMAGE-004|Product Image|ADMIN|Reorder Images|MUST|TR-IMAGE-004|
|FR-IMAGE-005|Product Image|ADMIN|Maximum one thumbnail|MUST|TR-IMAGE-003|
|FR-IMAGE-006|Product Image|ADMIN|File/MIME/size validation + S3|MUST|TR-IMAGE-002, TR-IMAGE-006|
|FR-IMAGE-007|Product Image UI|GUEST/USER|Zoom/Thumbnail/Mobile Swipe|MUST|TR-IMAGE-005|
|FR-CART-001|Cart|USER|User-owned Cart|MUST|TR-CART-001|
|FR-CART-002|Cart|USER|Add valid Product/quantity|MUST|TR-CART-001, TR-CART-002|
|FR-CART-003|Cart|USER|Update valid quantity|MUST|TR-CART-001, TR-CART-002|
|FR-CART-004|Cart|USER|Remove Cart Item|MUST|TR-CART-001|
|FR-CART-005|Cart|SYSTEM|Backend recalculates Cart price|MUST|TR-CART-003|
|FR-CART-006|Cart/Checkout|USER|Checkout revalidates Product/Inventory/Price|MUST|TR-CART-002, TR-ORDER-001|
|FR-CART-007|Cart|USER/SYSTEM|Successful checkout removes checked-out Cart Items|MUST|TR-CART-004|
|FR-CART-008|Cart|USER/SYSTEM|Failed checkout preserves Cart Items|MUST|TR-CART-005, TR-PAY-010|
|FR-INV-001|Inventory|ADMIN|Inventory list/search/page + stock fields|MUST|TR-INV-008|
|FR-INV-002|Inventory|ADMIN|Adjustment + reason + invariants|MUST|TR-INV-001, TR-INV-002|
|FR-INV-003|Inventory|ADMIN|Inventory Transaction History|MUST|TR-INV-009, TR-INV-010|
|FR-INV-004|Inventory|SYSTEM|Reserve without physical decrease|MUST|TR-INV-003|
|FR-INV-005|Inventory|SYSTEM|Commit exactly once after PAID|MUST|TR-INV-005, TR-PAY-002|
|FR-INV-006|Inventory|SYSTEM|Release exactly once|MUST|TR-INV-004|
|FR-INV-007|Inventory|ADMIN/SYSTEM|Low Stock/Out of Stock|MUST|TR-INV-008, TR-NOTIFY-010, TR-NOTIFY-011|
|FR-INV-008|Inventory|SYSTEM|Concurrency-safe reservation|MUST|TR-INV-007|
|FR-ORDER-001|Order|USER|Canonical checkout creates complete Order|MUST|TR-ORDER-001|
|FR-ORDER-002|Order|USER|My Orders only|MUST|TR-ORDER-002|
|FR-ORDER-003|Order|USER|Own Order Detail only|MUST|TR-ORDER-003|
|FR-ORDER-004|Order|ADMIN|List/Search/Filter/Pagination|MUST|TR-ORDER-001|
|FR-ORDER-005|Order|ADMIN|Admin Order Detail|MUST|TR-ORDER-001|
|FR-ORDER-006|Order|ADMIN/SYSTEM|Payment-gated Order State Machine|MUST|TR-ORDER-011, TR-ORDER-012, TR-ORDER-013, TR-ORDER-014, TR-ORDER-015, TR-ORDER-016|
|FR-ORDER-007|Order|ADMIN|Only ADMIN cancels|MUST|TR-ORDER-007, TR-ORDER-008, TR-ORDER-009|
|FR-ORDER-008|Order|USER/SYSTEM|Receiver Information + unique Order Code|MUST|TR-ORDER-001|
|FR-ORDER-009|Order/Pricing|SYSTEM|Immutable bilingual Product Name + Price Snapshot|MUST|TR-ORDER-004|
|FR-ORDER-010|Checkout|USER/SYSTEM|Duplicate checkout protection|MUST|TR-ORDER-010|
|FR-ORDER-011|Order/Payment|SYSTEM|One Order → One Payment|MUST|TR-ORDER-017, TR-ORDER-018, TR-PAY-012|
|FR-PAY-001|Payment|SYSTEM|Payment.status authoritative|MUST|TR-PAY-011, TR-ORDER-019|
|FR-PAY-002|Payment|SYSTEM|One PAYOS Payment record per Order|MUST|TR-PAY-001, TR-PAY-012, TR-PAY-013|
|FR-PAY-003|Payment|SYSTEM|Create payOS Payment Information|MUST|TR-PAY-001, TR-PAY-013|
|FR-PAY-004|Payment|SYSTEM|payOS creation failure consistency|MUST|TR-PAY-010|
|FR-PAY-005|Payment|SYSTEM|Verify payOS Webhook|MUST|TR-PAY-002, TR-PAY-003|
|FR-PAY-006|Payment|SYSTEM|PAID → commit once; Order remains NEW|MUST|TR-PAY-002, TR-ORDER-016, TR-NOTIFY-006|
|FR-PAY-007|Payment|SYSTEM|FAILED → CANCELLED + release|MUST|TR-PAY-005, TR-NOTIFY-009|
|FR-PAY-008|Payment|SYSTEM|Payment CANCELLED → Order CANCELLED + release|MUST|TR-PAY-006, TR-NOTIFY-009|
|FR-PAY-009|Payment|SYSTEM|PENDING >15 min → EXPIRED + release using Backend time|MUST|TR-PAY-007, TR-PAY-008, TR-TIME-002, TR-NOTIFY-009|
|FR-PAY-010|Payment|SYSTEM|Webhook idempotency|MUST|TR-PAY-004, TR-NOTIFY-008|
|FR-PAY-011|Payment|SYSTEM|Late success event safe/manual resolution|MUST|TR-PAY-009|
|FR-PAY-012|Payment|SYSTEM|External payment identifier optional-before-assignment lifecycle|MUST|TR-PAY-013|
|FR-REFUND-001|Refund|ADMIN|Only CANCELLED + PAID may be recorded REFUNDED|MUST|TR-REFUND-001, TR-REFUND-002, TR-REFUND-003, TR-REFUND-004|
|FR-REFUND-002|Refund/Inventory|ADMIN/SYSTEM|Refund status update does not restore Inventory again|MUST|TR-REFUND-005|
|FR-NOTIFY-001|Notification|ADMIN/SYSTEM|Persist Notification + recipient state|MUST|TR-NOTIFY-001, TR-NOTIFY-004|
|FR-NOTIFY-002|Notification|SYSTEM|Required business Notification types|MUST|TR-NOTIFY-001|
|FR-NOTIFY-003|Notification|ADMIN|Per-recipient read/unread|MUST|TR-NOTIFY-002, TR-NOTIFY-004|
|FR-NOTIFY-004|Notification|ADMIN/SYSTEM|Near realtime Notification|SHOULD|TR-NOTIFY-003|
|FR-NOTIFY-005|Notification|SYSTEM/ADMIN|NEW\_ORDER/PAYMENT\_SUCCESS/PAYMENT\_FAILED/Stock trigger semantics|MUST|TR-NOTIFY-005, TR-NOTIFY-006, TR-NOTIFY-007, TR-NOTIFY-008, TR-NOTIFY-009, TR-NOTIFY-010, TR-NOTIFY-011|
|FR-REPORT-001|Reporting|ADMIN|Dashboard widgets|MUST|TR-REPORT-003|
|FR-REPORT-002|Reporting|ADMIN|Revenue = COMPLETED + PAID using business date semantics|MUST|TR-REPORT-001, TR-TIME-001|
|FR-REPORT-003|Reporting|ADMIN|Best Selling = COMPLETED + PAID|MUST|TR-REPORT-002|
|FR-REPORT-004|Reporting|ADMIN|Recent Orders + Low Stock|MUST|TR-REPORT-003|
|FR-REPORT-005|Reporting|ADMIN|New Orders = NEW + PAID|MUST|TR-REPORT-004, TR-REPORT-005|
|FR-CONTENT-001|Static Content|GUEST/USER|FAQ and Policy static|MUST|TR-CONTENT-001|
|FR-CONTENT-002|Contact|GUEST/USER|Confirmed Contact Information|MUST|TR-CONTENT-002|
|FR-I18N-001|Localization|GUEST/USER/ADMIN|Vietnamese/English UI and data|MUST|TR-I18N-001|
|NFR-SEC-001|Security|ALL|HTTPS production|MUST|TR-SEC-001|
|NFR-SEC-002|Security|USER/ADMIN|JWT validation|MUST|TR-AUTH-002|
|NFR-SEC-003|Security|USER/ADMIN|Backend RBAC|MUST|TR-AUTH-003, TR-SEC-003|
|NFR-SEC-004|Security|USER|Google identity verification|MUST|TR-AUTH-001|
|NFR-SEC-005|Security|SYSTEM|payOS Webhook verification|MUST|TR-PAY-003|
|NFR-SEC-006|Security|SYSTEM|Secret management|MUST|TR-SEC-002|
|NFR-SEC-007|Security|SYSTEM|Sensitive logging protection|MUST|TR-SEC-002|
|NFR-SEC-008|Security|ALL|Backend input validation|MUST|Module validation tests|
|NFR-SEC-009|Security|USER/ADMIN|Database role/status authority|MUST|TR-ADMIN-005, TR-SEC-003|
|NFR-PER-001|Performance|ALL|Pagination|MUST|List module tests|
|NFR-PER-002|Performance|GUEST/USER|Backend filtering|MUST|TR-PRODUCT-004|
|NFR-PER-003|Performance|SYSTEM|Database indexing|MUST|Performance Test|
|NFR-PER-004|Performance|ALL|Reasonable response target|SHOULD|Performance Test|
|NFR-REL-001|Reliability|SYSTEM|Payment idempotency|MUST|TR-PAY-004|
|NFR-REL-002|Reliability|SYSTEM|Checkout idempotency|MUST|TR-ORDER-010|
|NFR-REL-003|Reliability|SYSTEM|Inventory concurrency safety|MUST|TR-INV-007|
|NFR-REL-004|Reliability|SYSTEM|Expiration release reliability|MUST|TR-PAY-007, TR-PAY-008|
|NFR-REL-005|Reliability|SYSTEM|Transaction consistency|MUST|Critical integration tests|
|NFR-REL-006|Reliability|ADMIN/SYSTEM|Last ADMIN concurrency protection|MUST|TR-ADMIN-007|
|NFR-REL-007|Reliability|SYSTEM|payOS creation failure recovery|MUST|TR-PAY-010|
|NFR-REL-008|Reliability|SYSTEM|Order/Payment state consistency|MUST|TR-PAY-011, TR-ORDER-019|
|NFR-REL-009|Reliability|SYSTEM|Complete physical/reserved Inventory audit|MUST|TR-INV-010, TR-INV-011, TR-INV-012|
|NFR-REL-010|Reliability|ADMIN/SYSTEM|Notification recipient isolation|MUST|TR-NOTIFY-004|
|NFR-REL-011|Reliability|SYSTEM|Business time consistency|MUST|TR-TIME-001, TR-TIME-002|
|NFR-REL-012|Reliability|SYSTEM|Notification business-event idempotency|MUST|TR-NOTIFY-008|
|NFR-COMPAT-001|Compatibility|ALL|Responsive UI|MUST|TR-COMPAT-001|
|NFR-COMPAT-002|Compatibility|ALL|Browser support|MUST|TR-COMPAT-002|
|NFR-COMPAT-003|Compatibility|GUEST/USER|Mobile Product Image interaction|MUST|TR-IMAGE-005|
|NFR-OBS-001|Observability|SYSTEM|Application Logging|MUST|TR-DEPLOY-004|
|NFR-OBS-002|Observability|SYSTEM|CloudWatch observability|MUST|TR-DEPLOY-004|
|NFR-OBS-003|Observability|SYSTEM|Authentication logging|MUST|TR-SEC-002|
|NFR-OBS-004|Observability|SYSTEM|Order logging|MUST|TR-ORDER-001, TR-ORDER-008, TR-ORDER-009|
|NFR-OBS-005|Observability|SYSTEM|Payment logging|MUST|TR-PAY-003, TR-PAY-009, TR-PAY-010|
|NFR-OBS-006|Observability|SYSTEM|Inventory logging|MUST|TR-INV-009|
|NFR-OBS-007|Observability|ADMIN/SYSTEM|Manual Refund audit|MUST|TR-REFUND-001|
|NFR-OBS-008|Observability|SYSTEM|Unexpected exception logging|MUST|Error Handling Test|
|NFR-OBS-009|Observability|SYSTEM|Notification business event logging|MUST|TR-NOTIFY-005, TR-NOTIFY-006, TR-NOTIFY-008|
|NFR-MAINT-001|Maintainability|DEVELOPMENT|Swagger/OpenAPI|MUST|TR-MAINT-001|
|NFR-MAINT-002|Maintainability|DEVELOPMENT|Database migration|MUST|TR-MAINT-002|
|NFR-MAINT-003|Maintainability|DEVELOPMENT|Requirement traceability|MUST|Document Review|
|NFR-DEPLOY-001|Deployment|SYSTEM|Production on AWS|MUST|TR-DEPLOY-001|
|NFR-DEPLOY-002|Deployment|SYSTEM|PostgreSQL on AWS|MUST|TR-DEPLOY-002|
|NFR-DEPLOY-003|Deployment|SYSTEM|Product Images on S3|MUST|TR-DEPLOY-003|
|NFR-DEPLOY-004|Deployment|SYSTEM|Application hosting on AWS|MUST|TR-DEPLOY-001|

\---

# 40\. FINAL BUSINESS STATE RULES

## 40.1. Awaiting Payment

```text

Order = NEW

Payment = PENDING

Inventory = RESERVED

```

Allowed Payment outcomes:

```text

PAID

FAILED

CANCELLED

EXPIRED

```

Không được:

```text

NEW + PENDING → CONFIRMED

```

Nếu đây là successful checkout initiation:

```text

NEW\\\_ORDER Notification = CREATED

```

cho ADMIN recipients phù hợp.

\---

## 40.2. Paid / Awaiting ADMIN Processing

Sau verified payOS success:

```text

Order = NEW

Payment = PAID

Inventory = COMMITTED

```

Allowed:

```text

NEW + PAID

→ CONFIRMED

```

Payment success không tự động confirm Order.

Sau successful verified Payment processing:

```text

PAYMENT\\\_SUCCESS Notification = CREATED

```

`Dashboard New Orders` bắt đầu tính Order này vì:

```text

Order = NEW

AND

Payment = PAID

```

\---

## 40.3. Confirmed

```text

Order = CONFIRMED

Payment = PAID

Inventory = COMMITTED

```

Allowed:

```text

CONFIRMED + PAID

→ COMPLETED

```

hoặc:

```text

CONFIRMED

→ CANCELLED

```

Nếu cancel paid Order:

```text

Inventory = RESTORED exactly once

Payment remains PAID

```

cho đến khi Manual Refund được ghi nhận sau refund thực tế.

\---

## 40.4. Completed

```text

Order = COMPLETED

Payment = PAID

Inventory = COMMITTED

```

Đây là final business state trong Version 1.3.

Không cho phép:

```text

COMPLETED → CANCELLED

```

\---

## 40.5. Payment Failed

```text

Order = CANCELLED

Payment = FAILED

Inventory = RELEASED

```

Không được có Inventory Reservation còn tồn tại.

Sau cleanup:

```text

PAYMENT\\\_FAILED Notification

```

được tạo với detail thể hiện actual status `FAILED`.

\---

## 40.6. Payment Cancelled

```text

Order = CANCELLED

Payment = CANCELLED

Inventory = RELEASED

```

Sau cleanup:

```text

PAYMENT\\\_FAILED Notification

```

được tạo với detail thể hiện actual status `CANCELLED`.

\---

## 40.7. Payment Expired

Sau 15 phút dựa trên authoritative Backend time:

```text

Order = CANCELLED

Payment = EXPIRED

Inventory = RELEASED

```

Sau cleanup:

```text

PAYMENT\\\_FAILED Notification

```

được tạo với detail thể hiện actual status `EXPIRED`.

\---

## 40.8. payOS Creation Failure

Nếu không thể tạo payOS Payment Information:

```text

Order = CANCELLED

Payment = FAILED

Inventory = RELEASED

Cart Items = RETAINED

```

State cleanup phải idempotent và logged.

Không tạo:

```text

NEW\\\_ORDER Notification

```

vì successful checkout initiation chưa hoàn tất.

\---

## 40.9. ADMIN Cancels Paid Order

Nếu trước cancellation:

```text

Order = NEW hoặc CONFIRMED

Payment = PAID

Inventory = COMMITTED

```

sau cancellation:

```text

Order = CANCELLED

Payment = PAID

Inventory = RESTORED exactly once

```

Đây là state chờ Manual Refund.

\---

## 40.10. Manual Refund Completed

Sau khi refund thực tế thành công và ADMIN ghi nhận:

```text

Order = CANCELLED

Payment = REFUNDED

Inventory = already RESTORED

```

Manual Refund không làm Inventory thay đổi thêm.

\---

## 40.11. Successful Checkout Cart State

Khi:

```text

Order created

OrderItems created

Inventory reserved

Payment record created

payOS Payment Information created successfully

```

thì:

```text

checked-out Cart Items = REMOVED

```

Sau Cart lifecycle processing:

```text

NEW\\\_ORDER Notification = CREATED

```

Checkout idempotency vẫn được giữ ở Backend.

\---

## 40.12. Failed Checkout Cart State

Nếu checkout thất bại trước successful payOS Payment Information creation:

```text

Cart Items = RETAINED

```

Không tạo successful-checkout `NEW\\\_ORDER` Notification.

\---

## 40.13. Late Payment Success Event

Nếu:

```text

Order = CANCELLED

Payment = FAILED/CANCELLED/EXPIRED

Inventory = RELEASED

```

và nhận late verified success event:

```text

Do not reopen Order automatically

Do not commit Inventory automatically

Audit event

Flag manual resolution

```

\---

# 41\. FINAL CONSISTENCY RULES

Toàn hệ thống phải bảo đảm các invariants sau:

```text

1\\\\. NEW → CONFIRMED chỉ hợp lệ khi Payment.status = PAID.



2\\\\. CONFIRMED → COMPLETED chỉ hợp lệ khi Payment.status = PAID.



3\\\\. Order processing không được tiếp tục khi Payment chưa PAID.



4\\\\. Payment success không tự động chuyển Order từ NEW sang CONFIRMED.



5\\\\. Payment.status là authoritative Payment State.



6\\\\. Order response paymentStatus phải derive từ associated Payment.status

\\\&#x20;  hoặc được đồng bộ transactional nếu denormalized.



7\\\\. Không được tồn tại contradictory Order/Payment state do hai source

\\\&#x20;  of truth không đồng bộ.



8\\\\. Một Order chỉ có đúng một Payment trong Version 1.3.



9\\\\. Order đã CANCELLED không được reuse để tạo Payment mới.



10\\\\. Payment FAILED/CANCELLED/EXPIRED phải dẫn đến Order CANCELLED.



11\\\\. Payment FAILED/CANCELLED/EXPIRED không được còn Inventory RESERVED.



12\\\\. Payment PAID phải commit reserved Inventory đúng một lần.



13\\\\. Không commit Inventory hai lần.



14\\\\. Không release Reservation hai lần.



15\\\\. Không restore paid-cancelled Inventory hai lần.



16\\\\. CANCELLED + PAID là state hợp lệ tạm thời trong Manual Refund flow.



17\\\\. REFUNDED chỉ hợp lệ khi Order = CANCELLED và Payment trước đó = PAID

\\\&#x20;   theo current refund scope.



18\\\\. NEW + PAID không được direct REFUNDED.



19\\\\. CONFIRMED + PAID không được direct REFUNDED.



20\\\\. COMPLETED + PAID không được REFUNDED trong Version 1.3.



21\\\\. Manual Refund không restore Inventory lần hai.



22\\\\. Manual Refund không release Inventory lần hai.



23\\\\. COMPLETED Order không được cancel trong Version 1.3.



24\\\\. payOS creation failure không được để Inventory Reservation treo.



25\\\\. payOS creation failure phải eventual converge về:

\\\&#x20;   Order CANCELLED + Payment FAILED + Inventory RELEASED.



26\\\\. Duplicate Webhook không được tạo duplicate business operation.



27\\\\. Duplicate logical checkout không được tạo duplicate Order.



28\\\\. Successful checkout phải remove đúng các CartItem đã checkout.



29\\\\. Failed checkout trước successful payOS creation phải giữ CartItem.



30\\\\. Cart clearing không thay thế Checkout Idempotency.



31\\\\. Không oversell.



32\\\\. Inventory invariant luôn phải giữ:

\\\&#x20;   quantity >= 0,

\\\&#x20;   reservedQuantity >= 0,

\\\&#x20;   availableQuantity >= 0.



33\\\\. RESERVE chỉ tăng reservedQuantity.



34\\\\. RELEASE chỉ giảm reservedQuantity.



35\\\\. SALE giảm physical quantity và reservedQuantity.



36\\\\. CANCEL\\\_ORDER restore physical quantity và không tăng reservedQuantity.



37\\\\. InventoryTransaction phải audit đủ physical quantity

\\\&#x20;   và reserved quantity trước/sau.



38\\\\. Product/Discount thay đổi không được sửa historical OrderItem price.



39\\\\. Backend là authoritative source cho Product sellingPrice,

\\\&#x20;   OrderItem total và Order total.



40\\\\. Frontend không được override authoritative price.



41\\\\. USER không được tự cancel Order.



42\\\\. USER không được tự cập nhật Payment Status.



43\\\\. USER không được tự record Refund.



44\\\\. Runtime role/status phải dựa trên authoritative User state trong database.



45\\\\. Frontend không phải security boundary.



46\\\\. Hệ thống không được mất toàn bộ ACTIVE ADMIN ở runtime.



47\\\\. Multi-ADMIN Notification read state phải độc lập theo recipient.



48\\\\. ADMIN A mark Notification read không được làm ADMIN B tự read.



49\\\\. Dashboard New Orders chỉ gồm Order = NEW + Payment = PAID.



50\\\\. Revenue chỉ dựa trên COMPLETED + PAID.



51\\\\. Best Selling chỉ dựa trên COMPLETED + PAID.



52\\\\. Late verified success event sau terminal unsuccessful Payment

\\\&#x20;   không được tự reopen Order hoặc tự commit released Inventory.



53\\\\. Một Product không được có nhiều effective Discount đồng thời.



54\\\\. Product Soft Delete chỉ sử dụng deletedAt;

\\\&#x20;   không sử dụng DELETED status song song.



55\\\\. Mỗi Product tối đa 10 images và tối đa một thumbnail.



56\\\\. Static FAQ/Policy/Contact không yêu cầu Admin CRUD Backend.



57\\\\. User Profile Editing không thuộc Version 1.3.



58\\\\. Business-facing time sử dụng Asia/Ho\\\_Chi\\\_Minh semantics.



59\\\\. Frontend clock không phải source of truth cho Payment Expiration

\\\&#x20;   hoặc Discount effectiveness.



60\\\\. Initial production bootstrap phải có cách tạo ít nhất một ADMIN;

\\\&#x20;   nếu database chưa có ADMIN thì ADMIN\\\_EMAILS phải chứa ít nhất

\\\&#x20;   một valid Google email.



61\\\\. ADMIN\\\_EMAILS không được re-promote User đã có authoritative role

\\\&#x20;   trong database sau khi User đó bị demote.



62\\\\. NEW\\\_ORDER Notification chỉ được trigger sau successful checkout initiation.



63\\\\. PAYMENT\\\_SUCCESS Notification chỉ được trigger sau verified payOS

\\\&#x20;   payment success và successful Inventory commit.



64\\\\. NEW\\\_ORDER Notification không đồng nghĩa Payment = PAID.



65\\\\. Dashboard New Orders vẫn chỉ tính NEW + PAID,

\\\&#x20;   độc lập với NEW\\\_ORDER Notification semantics.



66\\\\. Duplicate technical event không được tạo duplicate business

\\\&#x20;   Notification không kiểm soát.



67\\\\. externalTransactionIdentifier có thể absent trước successful external

\\\&#x20;   identifier assignment; khi tồn tại phải được bảo vệ consistency

\\\&#x20;   và idempotency phù hợp.



68\\\\. Payment expiration 15 phút phải được tính từ authoritative Backend

\\\&#x20;   payment creation timestamp.



69\\\\. Cùng một business timestamp phải có interpretation nhất quán giữa

\\\&#x20;   Frontend, Backend, Database representation, Reporting và Discount logic.



70\\\\. PAYMENT\\\_FAILED Notification có thể đại diện cho FAILED,

\\\&#x20;   CANCELLED hoặc EXPIRED nhưng phải thể hiện actual status trong detail.



71\\\\. LOW\\\_STOCK chỉ áp dụng khi availableQuantity > 0

\\\&#x20;   và availableQuantity <= lowStockThreshold.



72\\\\. OUT\\\_OF\\\_STOCK áp dụng khi availableQuantity = 0.



73\\\\. LOW\\\_STOCK/OUT\\\_OF\\\_STOCK Notification không được tạo lặp không kiểm soát

\\\&#x20;   khi inventory state không thay đổi.



74\\\\. payOS creation failure không được tạo NEW\\\_ORDER Notification

\\\&#x20;   vì successful checkout initiation chưa hoàn tất.



75\\\\. Successful checkout có thể tạo NEW\\\_ORDER khi Payment còn PENDING,

\\\&#x20;   sau đó tạo PAYMENT\\\_SUCCESS khi Payment thực sự PAID.



76\\\\. Initial ADMIN Bootstrap Validation và Last-ADMIN Protection là

\\\&#x20;   hai business rule riêng biệt và không thay thế lẫn nhau.



77\\\\. Currency authoritative của hệ thống là VND.



78\\\\. basePrice, sellingPrice, OrderItem.totalPrice,

\\\&#x20;   Order.totalAmount và Payment.amount phải là số nguyên VND.



79\\\\. PERCENTAGE Discount fractional sellingPrice phải được

\\\&#x20;   HALF\\\_UP về nearest 1 VND trước khi tính OrderItem.totalPrice.



80\\\\. OrderItem.totalPrice phải sử dụng rounded sellingPrice.



81\\\\. Order.totalAmount phải bằng SUM(OrderItem.totalPrice).



82\\\\. Payment.amount phải bằng chính xác Order.totalAmount.



83\\\\. Product, Cart, Checkout, Order và Payment không được

\\\&#x20;   sử dụng rounding rule khác nhau.



84\\\\. Frontend không phải source of truth cho monetary rounding.



85\\\\. Product.basePrice phải là positive integer VND.



86\\\\. Product.basePrice có fractional VND phải bị reject;

\\\&#x20;   hệ thống không được silently round Product basePrice.



87\\\\. FIXED\\\_PRICE discountValue phải là positive integer VND

\\\&#x20;   và nhỏ hơn Product.basePrice.



88\\\\. FIXED\\\_PRICE discountValue có fractional VND phải bị reject.



89\\\\. PERCENTAGE discountValue không bắt buộc là integer;

\\\&#x20;   monetary result sau percentage calculation phải được

\\\&#x20;   HALF\\\_UP theo BR-MONEY-001.

```

\---

# 42\. DOCUMENT COMPLETION STATUS

`02\\\_SRS\\\_SPEC – Version 1.3` xác định requirement baseline cuối cùng cho:

```text

Authentication

Authorization

Admin Bootstrap

Initial Admin Configuration Validation

User Management

Last ADMIN Protection



Business Timezone

Business Time Consistency



Product

Collection

Simple Discount

Selling Price

VND Monetary Rule

VND Monetary Validation

HALF\\\_UP Rounding Consistency

Product Image



Cart

Cart Post-checkout Lifecycle



Inventory

Inventory Transaction Audit

Inventory Reservation

Concurrency Control



Order

Order Payment Preconditions

Historical OrderItem Snapshot

Checkout Idempotency

Order–Payment Cardinality



Payment Status Source of Truth

payOS Payment

External Payment Identifier Lifecycle

payOS Creation Failure Handling

Payment Expiration

Webhook Verification

Payment Idempotency

Late Payment Event Handling



Manual Refund Eligibility

Manual Refund Inventory Isolation



Multi-ADMIN Notification

NEW\\\_ORDER Trigger

PAYMENT\\\_SUCCESS Trigger

PAYMENT\\\_FAILED Semantics

Inventory Notification Trigger

Notification Event Deduplication



Reporting

New Orders Metric



Static Content

Contact

Localization



API Convention

Error Handling

Validation

Security

Transaction Consistency

Performance

Reliability

Compatibility

Observability

Maintainability

Testing

AWS Deployment

Acceptance Criteria

Traceability

Final Business State Rules

Final Consistency Rules

```

Tài liệu này có trạng thái:

```text

BASELINE / FROZEN

```

Các quyết định implementation chi tiết tiếp theo phải được mô tả trong:

```text

03\\\_SYSTEM\\\_ARCHITECTURE

04\\\_IMPLEMENTATION\\\_SPEC

```

và việc lập kế hoạch triển khai/phân công được thực hiện trong:

```text

05\\\_PROJECT\\\_PLAN

```

Các tài liệu downstream không được tự ý thay đổi các business rule đã được freeze trong:

```text

02\\\_SRS\\\_SPEC

Version 1.3

Status: BASELINE / FROZEN

```



