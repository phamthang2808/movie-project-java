# 🧪 Hướng Dẫn Test VNPay với Postman

## 📋 Chuẩn Bị

### 1. Kiểm Tra Application Đang Chạy

- Server: `http://localhost:8088`
- API Prefix: `/api/v1`

### 2. Thông Tin VNPay Config (Sandbox)

```
TMN Code: JGV9MSIF
Secret Key: E9QLQ1W7KCLQKQLE5522R5JNRR7WIV8I
Payment URL: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
Return URL: http://localhost:8088/api/v1/vnpay/return
```

---

## 🚀 Test Case 1: Tạo Payment Request

### Request

```
POST http://localhost:8088/api/v1/vnpay/create
Content-Type: application/json
```

### Body (JSON)

```json
{
  "amount": "100000",
  "bankCode": "BIDV"
}
```

**Lưu ý:**

- `bankCode` là tùy chọn, **mặc định là BIDV**
- Các bank code phổ biến: `BIDV`, `NCB`, `VCB`, `TCB`, `VIB`, etc.
- Không truyền `bankCode` = chọn ngân hàng mặc định

### Expected Response

```
https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...&vnp_TxnRef=12345678&vnp_SecureHash=...
```

### ✅ Trong Postman:

1. Click **New** → **HTTP Request**
2. Chọn method: **POST**
3. URL: `http://localhost:8088/api/v1/vnpay/create`
4. Tab **Body** → Chọn **raw** → Chọn **JSON**
5. Paste JSON body ở trên
6. Click **Send**

### 📊 Response Headers

```
Content-Type: text/plain;charset=UTF-8
```

---

## 🧪 Test Case 2: Thanh Toán Trên VNPay Sandbox

### Bước 1: Lấy Payment URL

- Chạy Test Case 1 để lấy URL

### Bước 2: Copy URL và Mở Trên Browser

- Copy toàn bộ URL từ response
- Dán vào trình duyệt

### Bước 3: Thanh Toán Thử Nghiệm

Trên trang VNPay Sandbox:

**Thông tin thẻ test:**

- **Ngân hàng:** NCB
- **Số thẻ:** `9704198526191432198`
- **Tên chủ thẻ:** `NGUYEN VAN A`
- **Ngày phát hành:** `07/15`
- **Mã OTP:** `123456`

---

## 🔙 Test Case 3: Callback Return URL

### Request

```
GET http://localhost:8088/api/v1/vnpay/return?vnp_ResponseCode=00
```

### Query Parameters

| Parameter        | Value | Mô Tả                                       |
| ---------------- | ----- | ------------------------------------------- |
| vnp_ResponseCode | 00    | Thành công                                  |
| vnp_ResponseCode | 07    | Giao dịch bị nghi ngờ                       |
| vnp_ResponseCode | 09    | Thẻ/Tài khoản chưa đăng ký                  |
| vnp_ResponseCode | 10    | Xác thực thông tin thẻ/tài khoản không đúng |

### Expected Response (Success)

```json
{
  "success": true,
  "message": "✅ Thanh toán thành công!",
  "orderId": "12345678",
  "paymentTime": "2025-10-30 15:30:00"
}
```

### Expected Response (Failed)

```json
{
  "success": false,
  "message": "❌ Thanh toán thất bại! Mã lỗi: 07"
}
```

---

## 📝 Test Scenarios

### Scenario 1: Thanh Toán Thành Công ✅

```
1. POST /api/v1/vnpay/create với amount = "100000"
2. Nhận được payment URL
3. Mở URL trên browser
4. Nhập thông tin thẻ test
5. VNPay redirect về: .../return?vnp_ResponseCode=00
6. Nhận message: "Thanh toán thành công!"
```

### Scenario 2: User Hủy Thanh Toán ❌

```
1. POST /api/v1/vnpay/create
2. Mở payment URL
3. Click "Hủy bỏ" trên trang VNPay
4. VNPay redirect về: .../return?vnp_ResponseCode=24
5. Nhận message: "Thanh toán thất bại! Mã lỗi: 24"
```

### Scenario 3: Test Với Số Tiền Khác Nhau

```json
// Test 1: 50,000 VND
{ "amount": "50000" }

// Test 2: 1,000,000 VND
{ "amount": "1000000" }

// Test 3: 99,999,999 VND
{ "amount": "99999999" }
```

### Scenario 4: Test Các Ngân Hàng Khác Nhau 🏦

```json
// BIDV (mặc định, không cần truyền bankCode)
{ "amount": "100000" }
{ "amount": "100000", "bankCode": "BIDV" }

// NCB
{ "amount": "100000", "bankCode": "NCB" }

// VCB (Vietcombank)
{ "amount": "100000", "bankCode": "VCB" }

// TCB (Techcombank)
{ "amount": "100000", "bankCode": "TCB" }

// VIB
{ "amount": "100000", "bankCode": "VIB" }
```

---

## 🐛 Debugging Tips

### Check Logs

```bash
# Theo dõi logs trong console:
📝 Bắt đầu tạo payment VNPay - Số tiền: 100000 VND
🏦 Ngân hàng được chọn: BIDV
💰 Số tiền sau khi convert: 10000000 (x100)
✅ Tạo VNPay payment URL thành công - Mã giao dịch: 12345678
🔗 Payment URL: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...
```

### Common Issues

#### Issue 1: "Số tiền không hợp lệ"

**Nguyên nhân:** Amount không phải là số  
**Giải pháp:** Đảm bảo amount là string số, VD: "100000"

#### Issue 2: "Invalid SecureHash"

**Nguyên nhân:** Secret key không đúng hoặc config sai  
**Giải pháp:** Kiểm tra `vnpay.secret-key` trong application.yml

#### Issue 3: Return URL không hoạt động

**Nguyên nhân:** URL không đúng hoặc server không chạy  
**Giải pháp:**

- Check return-url: `http://localhost:8088/api/v1/vnpay/return`
- Đảm bảo server đang chạy trên port 8088

---

## 📦 Postman Collection

### Import Collection

Tạo collection trong Postman:

```json
{
  "info": {
    "name": "VNPay Payment API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Create Payment",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"amount\": \"100000\"\n}"
        },
        "url": {
          "raw": "http://localhost:8088/api/v1/vnpay/create",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8088",
          "path": ["api", "v1", "vnpay", "create"]
        }
      }
    },
    {
      "name": "Payment Return (Success)",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8088/api/v1/vnpay/return?vnp_ResponseCode=00",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8088",
          "path": ["api", "v1", "vnpay", "return"],
          "query": [
            {
              "key": "vnp_ResponseCode",
              "value": "00"
            }
          ]
        }
      }
    }
  ]
}
```

---

## ✅ Checklist Test

- [ ] Tạo payment request thành công
- [ ] Nhận được valid payment URL
- [ ] Mở được trang VNPay sandbox
- [ ] Thanh toán thành công với thẻ test
- [ ] Nhận callback return với ResponseCode=00
- [ ] Xử lý thành công và hiển thị message đúng
- [ ] Test hủy thanh toán
- [ ] Test với số tiền khác nhau
- [ ] Check logs đầy đủ

---

**Chúc bạn test thành công! 🎉**
