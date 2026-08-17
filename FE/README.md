# Frontend

React storefront chạy bằng Vite và kết nối Spring Boot qua proxy `/api`.

```powershell
Copy-Item .env.example .env
npm install
npm run dev
```

Frontend: `http://127.0.0.1:3000`

Backend mặc định: `http://127.0.0.1:8080`

Mã giao diện cũ không còn sử dụng được lưu trong `legacy/` để tham khảo.
