# OrderHub OMS

Hệ thống quản lý đơn hàng và tồn kho nội bộ dành cho doanh nghiệp. OrderHub hỗ trợ nhân viên tạo và xử lý đơn hàng, theo dõi tồn kho theo lô, tính giá vốn xuất kho theo FIFO và tra cứu lịch sử biến động kho.

> Đây là ứng dụng OMS nội bộ phục vụ quản lý vận hành. Hiện tại hệ thống chưa tích hợp webhook/API thực tế với Shopee, Lazada, Shopify hoặc nền tảng bán hàng bên ngoài; đơn hàng được tạo từ giao diện nội bộ hoặc REST API.

## Mục lục

- [Chức năng chính](#chức-năng-chính)
- [Công nghệ](#công-nghệ)
- [Kiến trúc và nghiệp vụ](#kiến-trúc-và-nghiệp-vụ)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Cấu hình và chạy ứng dụng](#cấu-hình-và-chạy-ứng-dụng)
- [Tài khoản mẫu](#tài-khoản-mẫu)
- [Sử dụng và kiểm thử nghiệp vụ](#sử-dụng-và-kiểm-thử-nghiệp-vụ)
- [REST API tiêu biểu](#rest-api-tiêu-biểu)
- [Chạy automated tests](#chạy-automated-tests)
- [Cấu trúc mã nguồn](#cấu-trúc-mã-nguồn)
- [Giới hạn hiện tại và hướng phát triển](#giới-hạn-hiện-tại-và-hướng-phát-triển)

## Chức năng chính

- **Đăng nhập và phân quyền:** JWT stateless, hai vai trò `ADMIN` và `STAFF`; Admin tạo, xem và bật/tắt tài khoản nhân viên.
- **Danh mục và sản phẩm:** quản lý thông tin sản phẩm, SKU, giá bán, số lượng và giá nhập ban đầu.
- **Đơn hàng:** tạo đơn nhiều mặt hàng, tự ghi nhận nhân viên tạo đơn, lưu thông tin người nhận nếu có và chống tạo trùng request bằng `X-Idempotency-Key`.
- **Tồn kho:** giữ chỗ khi tạo đơn, xuất kho khi đơn được chuyển sang `SHIPPED`, giải phóng hàng khi hủy và ghi nhận hàng hoàn.
- **Quản lý lô và giá vốn:** mỗi lần nhập tạo một lô riêng với số lượng, mã lô và giá nhập riêng; xuất kho sử dụng FIFO.
- **Audit Logs:** lưu loại biến động, số lượng thay đổi, mã tham chiếu, thời điểm và người thực hiện.
- **Dashboard:** tổng hợp doanh thu, giá vốn, lợi nhuận gộp, tình hình đơn, cảnh báo tồn thấp và báo cáo theo danh mục.
- **Giao diện vận hành:** các trang Dashboard, danh mục, sản phẩm, kho, đơn hàng và quản lý nhân viên.
- **Tài liệu API:** Swagger UI được cung cấp bởi springdoc-openapi.

## Công nghệ

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 21 |
| Backend | Spring Boot 3.3.4, Spring MVC, Spring Validation |
| Bảo mật | Spring Security, JWT, BCrypt |
| Truy cập dữ liệu | Spring Data JPA, Hibernate |
| Cơ sở dữ liệu | PostgreSQL |
| Frontend | HTML, Tailwind CSS, JavaScript |
| Build và test | Gradle Wrapper, JUnit 5, Mockito, Spring Security Test, H2 |
| API documentation | springdoc-openapi / Swagger UI |

## Kiến trúc và nghiệp vụ

Ứng dụng được tổ chức theo các tầng controller, service, repository, entity và DTO:

1. **Controller** tiếp nhận HTTP request, xác thực dữ liệu đầu vào và trả response JSON.
2. **Service** xử lý quy tắc nghiệp vụ và transaction.
3. **Repository** truy vấn/cập nhật dữ liệu thông qua Spring Data JPA.
4. **Entity** ánh xạ dữ liệu quan hệ; DTO tách payload API khỏi entity lưu trữ.
5. **Spring Security filter** xác thực Bearer JWT và gắn username/role vào Security Context.

### Vai trò

| Vai trò | Quyền |
|---|---|
| `ADMIN` | Thực hiện nghiệp vụ nội bộ và quản lý tài khoản nhân viên |
| `STAFF` | Thực hiện nghiệp vụ đơn hàng, danh mục, sản phẩm và kho; không quản lý tài khoản nhân viên |

Các API `/api/**` yêu cầu tài khoản `ADMIN` hoặc `STAFF`, ngoại trừ endpoint đăng nhập. API `/api/admin/**` và trang quản lý nhân viên chỉ dành cho Admin.

### Vòng đời đơn hàng và tác động đến kho

`PENDING → CONFIRMED → SHIPPED → DELIVERED`

- **Tạo đơn (`PENDING`):** giữ chỗ hàng; tồn thực tế không giảm, tồn khả dụng giảm.
- **Xác nhận (`CONFIRMED`):** xác nhận đơn; không thay đổi số lượng kho.
- **Xuất kho (`SHIPPED`):** trừ tồn thực tế và số lượng giữ chỗ; phân bổ giá vốn từ các lô cũ trước theo FIFO.
- **Giao thành công (`DELIVERED`):** hoàn tất giao hàng; không trừ kho lần nữa.
- **Hủy (`CANCELLED`):** chỉ được chuyển từ `PENDING` hoặc `CONFIRMED`; giải phóng số hàng đã giữ chỗ.
- **Hoàn hàng (`RETURNED`):** có thể chuyển từ `SHIPPED` hoặc `DELIVERED`; cộng hàng trở lại kho và hoàn về các lô gốc theo phần phân bổ đã lưu.

Các đơn `PENDING` quá thời hạn sẽ được scheduler tự hủy và giải phóng lượng hàng giữ chỗ. Cấu hình hiện tại trong `application.yml` là 1 phút, quét mỗi 30 giây; có thể chỉnh bằng các thuộc tính `order.expiration-minutes` và `order.scheduler-interval-ms`.

### Giá nhập, giá bán và FIFO

- `Product.price` là **giá bán hiện tại**, không phải giá nhập.
- Giá nhập được lưu trên từng `InventoryBatch`; lần nhập tiếp theo tạo lô mới, không ghi đè giá của lô trước.
- Giá vốn trong danh mục sản phẩm là bình quân gia quyền của các lô còn tồn; khi xuất đơn, giá vốn được tính theo FIFO và lưu trên dòng đơn hàng.
- Nếu lô tồn đầu kỳ cũ chưa có giá, hệ thống đánh dấu chưa đủ dữ liệu giá vốn. Dashboard không hiển thị lợi nhuận gộp khi không thể tính chính xác.

### Dashboard

Doanh thu và giá vốn hiện là số liệu lũy kế, không có bộ lọc thời gian. Báo cáo tài chính tính trên đơn ở trạng thái `SHIPPED` hoặc `DELIVERED`. Cảnh báo tồn thấp dựa trên tồn khả dụng nhỏ hơn hoặc bằng ngưỡng 10 đơn vị.

## Yêu cầu môi trường

- JDK 21.
- PostgreSQL đang chạy và có thể truy cập từ ứng dụng.
- Git (nếu clone repository).

## Cấu hình và chạy ứng dụng

### 1. Lấy mã nguồn

```bash
git clone <repository-url>
cd orderhub-api
```

Mở terminal tại thư mục chứa `build.gradle` và `gradlew.bat`.

### 2. Tạo database PostgreSQL

Tạo database rỗng tên `orderhub_db` trong PostgreSQL. Cấu hình mặc định của ứng dụng kết nối tới:

```text
jdbc:postgresql://localhost:5434/orderhub_db
```

Port, username và password có thể thay đổi qua biến môi trường Spring Boot. Ví dụ PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5434/orderhub_db"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "<mat-khau-postgresql-cua-ban>"
```

Không sử dụng mật khẩu mặc định trong môi trường triển khai thật. Lưu ý: `JwtTokenProvider` hiện đang giữ signing key trực tiếp trong mã nguồn; cấu hình `application.security.jwt` trong YAML chưa được provider sử dụng. Trước khi triển khai, cần chuyển signing key sang biến môi trường/secret manager, cấu hình thời hạn token tập trung và không commit secret vào repository.

### 3. Khởi chạy

Windows:

```powershell
.\gradlew.bat bootRun
```

macOS/Linux:

```bash
./gradlew bootRun
```

Ứng dụng chạy tại `http://localhost:8080`. Trong môi trường development, Hibernate đang dùng `ddl-auto: update` để cập nhật schema. DataSeeder tạo dữ liệu mẫu khi database tương ứng còn trống; tồn kho mẫu cũ chưa có giá nhập được đánh dấu là chưa định giá.

### 4. Truy cập giao diện và Swagger

- Dashboard: `http://localhost:8080/` hoặc `http://localhost:8080/index.html`
- Đăng nhập: `http://localhost:8080/login.html`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Tài khoản mẫu

DataSeeder **chỉ tạo** các tài khoản dưới đây khi bảng `users` hoàn toàn chưa có user. Nếu database đã chứa bất kỳ tài khoản nào, seeder sẽ không tạo hoặc đặt lại mật khẩu cho các tài khoản mẫu. Vì vậy, các thông tin này chỉ dùng được với database mới/đang trống:

| Vai trò | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Staff | `staff` | `staff123` |

Nếu đăng nhập thất bại trên database đã dùng trước đó, hãy đăng nhập bằng tài khoản đã được tạo trong database đó; mật khẩu đã mã hóa không thể xem lại từ DB. Hiện ứng dụng chưa có chức năng tự đặt lại mật khẩu Admin. Không xóa dữ liệu database chỉ để làm tài khoản mẫu xuất hiện.

Chỉ sử dụng tài khoản mẫu trên để chạy thử cục bộ. Đổi hoặc loại bỏ mật khẩu mẫu trước khi triển khai thực tế.

## Sử dụng và kiểm thử nghiệp vụ

### Kiểm thử qua giao diện

1. Đăng nhập bằng `admin`; tạo tài khoản Staff nếu cần.
2. Tạo danh mục và sản phẩm. Nếu nhập tồn ban đầu, khai báo giá nhập cho lô tồn ban đầu.
3. Vào **Kho hàng** để nhập lô bổ sung; mỗi đợt nhập cần mã lô/phiếu nhập riêng và giá nhập tương ứng.
4. Vào **Đơn hàng**, tạo đơn có một hoặc nhiều sản phẩm. Kiểm tra đơn được gắn với nhân viên đăng nhập và lượng hàng được giữ chỗ.
5. Xác nhận đơn rồi xuất kho. Kiểm tra tồn thực tế giảm, Audit Logs có người thực hiện và chi phí FIFO được ghi nhận.
6. Chuyển đơn sang giao thành công. Tồn kho không bị trừ thêm; Dashboard phản ánh doanh thu, giá vốn và lợi nhuận nếu dữ liệu giá vốn đầy đủ.
7. Thử hủy đơn trước khi xuất để kiểm tra lượng giữ chỗ được giải phóng; thử ghi nhận hoàn từ đơn đã xuất/giao để kiểm tra tồn được nhập lại.
8. Dùng `staff` thử truy cập `/staffs.html` hoặc `/api/admin/staffs`; thao tác quản lý nhân viên phải bị từ chối.

> Lưu ý: đơn `PENDING` có thể bị scheduler tự hủy sau thời gian cấu hình (mặc định 1 phút). Khi demo, hãy xác nhận đơn kịp thời hoặc tăng `order.expiration-minutes`.

### Kiểm thử REST API bằng Postman

1. Gọi `POST /api/auth/login` với JSON:

   ```json
   {
     "username": "staff",
     "password": "staff123"
   }
   ```

2. Sao chép `data.token` trong response.
3. Trong Postman thêm header:

   ```text
   Authorization: Bearer <token-nhan-duoc>
   ```

4. Thử gọi `GET /api/products`, `GET /api/dashboard/stats` và các endpoint nghiệp vụ. Swagger UI dùng để đọc/tìm hiểu schema API; cấu hình hiện tại chưa khai báo Bearer security scheme cho nút Authorize.
5. Để mô phỏng client ngoài tạo đơn, gọi `POST /api/orders` kèm Bearer token và header `X-Idempotency-Key: <UUID>`. Gửi lại cùng key trong thời gian bản ghi idempotency còn tồn tại để thử chống tạo đơn trùng. Đây chỉ là mô phỏng HTTP client, không phải kết nối webhook thật.

Ví dụ payload tạo đơn:

```json
{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ],
  "receiverName": "Nguyen Van A",
  "receiverPhone": "0900000000",
  "shippingAddress": "Ha Noi"
}
```

Sau khi tạo đơn, lấy `orderCode` trong response và chuyển trạng thái bằng:

```http
PATCH /api/orders/{orderCode}/status
Content-Type: application/json
```

```json
{
  "status": "CONFIRMED"
}
```

Thực hiện tiếp lần lượt với `SHIPPED`, sau đó `DELIVERED`. Chỉ chuyển trạng thái theo đúng state machine; các trạng thái cuối `RETURNED` và `CANCELLED` không thể chuyển tiếp.

### API nghiệp vụ thường dùng

| Nhóm | Endpoint | Mục đích |
|---|---|---|
| Auth | `POST /api/auth/login` | Đăng nhập và nhận JWT |
| Nhân viên (Admin) | `GET, POST /api/admin/staffs` | Xem danh sách / tạo Staff |
| Nhân viên (Admin) | `PATCH /api/admin/staffs/{id}/toggle-status` | Bật hoặc tắt tài khoản |
| Danh mục | `/api/categories` | CRUD danh mục |
| Sản phẩm | `/api/products` | CRUD sản phẩm và điều chỉnh tồn |
| Đơn hàng | `POST, GET /api/orders` | Tạo đơn và xem danh sách |
| Đơn hàng | `PATCH /api/orders/{orderCode}/status` | Chuyển trạng thái hợp lệ |
| Tồn kho | `POST /api/inventories/import` | Nhập lô hàng mới |
| Tồn kho | `GET /api/inventories/product/{productId}/batches` | Xem lô và giá vốn theo lô |
| Audit Logs | `GET /api/inventories/logs` | Xem biến động kho |
| Dashboard | `GET /api/dashboard/stats` | KPI vận hành/tài chính |
| Dashboard | `GET /api/dashboard/revenue-by-category` | Doanh thu theo danh mục |
| Dashboard | `GET /api/dashboard/stock-by-category` | Tồn kho theo danh mục |

Các API nghiệp vụ cần Bearer JWT. Xem Swagger UI để biết schema request/response đầy đủ và các endpoint chi tiết.

## Chạy automated tests

Windows:

```powershell
.\gradlew.bat test
```

macOS/Linux:

```bash
./gradlew test
```

Test dùng JUnit 5, Mockito, Spring Security Test và H2. Các phạm vi hiện có gồm:

- Quyền truy cập endpoint: Admin, Staff và người chưa xác thực.
- Đăng nhập và từ chối tài khoản bị vô hiệu hóa.
- Xuất kho FIFO, tính giá vốn/lợi nhuận và hoàn hàng về lô gốc.
- Scheduler giải phóng tồn giữ chỗ khi đơn `PENDING` hết hạn.

Để chạy sạch và test:

```powershell
.\gradlew.bat clean test
```

## Cấu trúc mã nguồn

```text
src/main/java/com/orderhub/
├── config/          # Security, OpenAPI, seed dữ liệu mẫu
├── controller/      # REST API
├── dto/              # Request/response API
├── entity/           # JPA entities và enums
├── event/            # Domain event
├── listener/         # Xử lý event sau khi tạo đơn
├── repository/       # Truy vấn dữ liệu
├── scheduler/        # Tự động hủy đơn giữ chỗ quá hạn
├── security/         # JWT filter và token provider
└── service/          # Nghiệp vụ ứng dụng

src/main/resources/
├── application.yml   # Cấu hình ứng dụng
└── static/           # Giao diện HTML/CSS/JavaScript

src/test/java/        # Unit và Spring MVC security tests
```

## Giới hạn hiện tại và hướng phát triển

- Chưa có tích hợp webhook/API thật với nền tảng bán hàng hoặc hãng vận chuyển.
- Chưa có quản lý vị trí/bin, picking wave, packing station, barcode/RFID và các chức năng của một WMS chuyên sâu.
- Dashboard hiện là số liệu lũy kế, chưa hỗ trợ bộ lọc khoảng thời gian hoặc xuất báo cáo.
- Schema được quản lý bằng `ddl-auto: update` trong cấu hình hiện tại; triển khai production nên dùng migration có versioning và cấu hình riêng theo môi trường.
- Bổ sung idempotency cho tích hợp bên ngoài, đối chiếu SKU, lưu mã đơn nguồn và xử lý webhook lặp/thứ tự trạng thái là các bước cần làm trước khi kết nối kênh bán thực tế.
