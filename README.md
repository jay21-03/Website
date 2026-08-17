# Đàng Xem Ecommerce

Dự án được tách thành hai ứng dụng độc lập:

- `BE/`: Spring Boot, PostgreSQL/Flyway, Docker Compose, AWS infrastructure và backend scripts.
- `FE/`: React/Vite storefront, assets và cấu hình frontend.
- `DOC/`: tài liệu phân tích và đặc tả dùng chung.

## Chạy backend

```powershell
Set-Location BE
Copy-Item .env.example .env
docker compose up -d --build
```

Backend chạy tại `http://127.0.0.1:8080`.

## Chạy frontend

Mở terminal khác:

```powershell
Set-Location FE
Copy-Item .env.example .env
npm install
npm run dev
```

Frontend chạy tại `http://127.0.0.1:3000` và proxy API tới backend.
