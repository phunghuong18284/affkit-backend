1. **Tổng quan kiến trúc**
2. **Cấu trúc project / package**
3. **Vai trò từng package**
4. **Giải thích từng class**
5. **Luồng xử lý từ controller → service → repository → database**
6. **Các điểm đáng chú ý như validation, phân trang, soft delete, phân quyền theo user, cache Redis, sinh short code...**

Mình có thể giúp bạn viết lại phần này theo kiểu **tài liệu kỹ thuật / README / thuyết minh module**.
Dưới đây là một phiên bản **chi tiết hơn rất nhiều**, bạn có thể dùng gần như trực tiếp.
# Mô tả chi tiết module `link`
## 1. Mục tiêu của module
Module `link` chịu trách nhiệm quản lý các link affiliate của người dùng, bao gồm:
- Tạo link mới
- Lấy danh sách link của riêng người dùng
- Xem chi tiết một link
- Cập nhật thông tin link
- Xóa link theo kiểu **soft delete**
- Phân loại link theo nền tảng
- Gắn tag cho link
- Kiểm soát số lượng link theo gói miễn phí
- Tối ưu tra cứu / xử lý bằng cache Redis ở một số bước nghiệp vụ

Nói ngắn gọn, đây là module trung tâm để người dùng **tạo, quản lý và theo dõi link affiliate** trong hệ thống.
## 2. Cấu trúc tổng quan của project
Với cách tổ chức package hiện tại, module `link` thường được chia theo mô hình nhiều lớp:
- **controller**: nhận request từ client và trả response
- **service**: xử lý nghiệp vụ
- **entity**: ánh xạ database
- **repository**: thao tác với dữ liệu
- **dto**: lớp trung gian trao đổi dữ liệu giữa client và server

Một cấu trúc điển hình có thể như sau:
``` text
vn.affkit
├── auth
│   ├── entity
│   └── repository
├── common
│   ├── exception
│   ├── ApiResponse
│   └── ...
├── link
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
└── ...
```
## 3. Giải thích từng package
### 3.1. `vn.affkit.link.controller`
Đây là tầng tiếp nhận request từ phía client.
Nhiệm vụ chính:
- Định nghĩa các endpoint REST
- Nhận dữ liệu đầu vào
- Gọi service tương ứng
- Trả về response theo format thống nhất

Trong module này, `LinkController` cung cấp các API như:
- `POST /api/v1/links` → tạo link mới
- `GET /api/v1/links` → lấy danh sách link
- `GET /api/v1/links/{id}` → lấy chi tiết link
- `PATCH /api/v1/links/{id}` → cập nhật link
- `DELETE /api/v1/links/{id}` → xóa link

### Vai trò của controller
Controller **không nên chứa nghiệp vụ phức tạp**.
Nó chỉ làm nhiệm vụ:
- nhận request
- lấy user hiện tại từ `@AuthenticationPrincipal`
- truyền dữ liệu vào service
- đóng gói kết quả vào `ApiResponse`

### 3.2. `vn.affkit.link.service`
Đây là tầng xử lý nghiệp vụ chính.
chịu trách nhiệm: `LinkService`
- kiểm tra user
- validate logic nghiệp vụ ngoài validation của request
- tạo short code
- giới hạn số lượng link theo plan
- lưu tag
- xác định platform từ URL
- lấy dữ liệu có phân trang
- cập nhật và xóa link
- bảo đảm dữ liệu chỉ thuộc về đúng người dùng

Service là nơi quyết định **hệ thống có cho phép thao tác đó hay không**.
### 3.3. `vn.affkit.link.dto`
DTO là các lớp dữ liệu dùng để trao đổi giữa client và server.
Thông thường sẽ gồm:
- : dữ liệu để tạo link `CreateLinkRequest`
- : dữ liệu để cập nhật link `UpdateLinkRequest`
- : dữ liệu trả về cho client `LinkResponse`

Lý do dùng DTO:
- không expose trực tiếp entity ra ngoài
- kiểm soát dữ liệu đầu vào / đầu ra tốt hơn
- dễ thay đổi cấu trúc API mà không ảnh hưởng database

### 3.4. `vn.affkit.link.entity`
Đây là tầng ánh xạ với database.
Thường sẽ có các entity như:
- `Link`: đại diện cho link affiliate
- : đại diện tag gắn với link `LinkTag`

Các entity này chứa thông tin như:
- URL gốc
- short code
- tiêu đề
- nền tảng
- campaign
- trạng thái xóa
- thời gian tạo / cập nhật
- user sở hữu link
- danh sách tag

### 3.5. `vn.affkit.link.repository`
Tầng repository dùng để truy vấn database thông qua Spring Data JPA.
Các repository thường đảm nhiệm:
- tìm link theo ID
- tìm link theo user
- lọc theo platform
- lấy danh sách tag
- kiểm tra link tồn tại hay chưa
- truy vấn phân trang

Repository không xử lý nghiệp vụ; nó chỉ lo **đọc/ghi dữ liệu**.
## 4. Giải thích chi tiết từng class
## 4.1. `LinkController`
`LinkController` là REST controller quản lý toàn bộ API liên quan đến link.
### Các annotation chính
- `@RestController`
  Cho biết đây là controller trả dữ liệu JSON trực tiếp.
- `@RequestMapping("/api/v1/links")`
  Định nghĩa prefix chung cho các endpoint.
- `@RequiredArgsConstructor`
  Lombok tự sinh constructor cho các dependency `final`.
- `@Tag(...)`
  Dùng cho OpenAPI/Swagger, mô tả nhóm API “Links”.

### Các method trong controller
#### `create(...)`
Tạo một link affiliate mới.
Luồng điển hình:
1. Lấy user đang đăng nhập từ `@AuthenticationPrincipal`
2. Nhận dữ liệu từ body request
3. Gọi `linkService.create(userId, req)`
4. Trả về response thành công

Điểm đáng chú ý:
- Có `@Valid` để kiểm tra dữ liệu đầu vào
- Chỉ user đã xác thực mới gọi được API

#### `list(...)`
Lấy danh sách link của chính người dùng.
Thường hỗ trợ:
- lọc theo `platform`
- phân trang bằng `page`, `size`

Luồng:
1. lấy user hiện tại
2. gọi service lấy danh sách theo userId
3. trả về `Page<LinkResponse>`

#### `getById(...)`
Lấy chi tiết một link.
Luồng:
1. nhận `linkId`
2. kiểm tra link có thuộc user hiện tại không
3. trả về dữ liệu link

Điểm quan trọng:
- Không được phép xem link của người khác nếu không có quyền

#### `update(...)`
Cập nhật thông tin link.
Thông thường chỉ cho sửa các trường như:
- title
- tags
- campaign

Các trường như:
- short code
- owner
- createdAt

thường không cho sửa.
#### `delete(...)`
Xóa link theo kiểu soft delete.
Soft delete nghĩa là:
- không xóa vật lý khỏi database
- chỉ đánh dấu là đã xóa

Cách này giúp:
- giữ lịch sử dữ liệu
- tránh mất dữ liệu quan trọng
- hỗ trợ audit / phục hồi sau này nếu cần

## 4.2. `LinkService`
là phần quan trọng nhất của module. `LinkService`
### Các dependency chính
- 
thao tác với dữ liệu `Link` `LinkRepository`
- 
thao tác với tag của link `LinkTagRepository`
- 
kiểm tra và lấy thông tin user `UserRepository`
- 
sinh short code ngắn, duy nhất `ShortCodeService`
- 
tương tác với Redis, có thể dùng cho cache / chống trùng / tối ưu tra cứu `StringRedisTemplate`

### Biến `FREE_PLAN_LIMIT = 10`
Đây là giới hạn số lượng link cho người dùng gói miễn phí.
Ý nghĩa:
- user free chỉ tạo tối đa 10 link
- nếu vượt quá thì hệ thống sẽ từ chối tạo thêm

Đây là một rule nghiệp vụ rõ ràng ở tầng service.
### Method `create(UUID userId, CreateLinkRequest req)`
Đây là luồng tạo link mới.
Quy trình thường gồm:
1. kiểm tra user có tồn tại không
2. kiểm tra số link hiện tại của user
3. nếu vượt giới hạn plan thì ném exception
4. xác định platform từ URL
5. tạo short code
6. tạo entity `Link`
7. lưu link vào database
8. lưu tags nếu có
9. trả về `LinkResponse`

#### Những nghiệp vụ có thể xảy ra ở đây:
- validate URL hợp lệ
- tránh tạo link trùng quá nhiều
- sinh short code duy nhất
- gán platform tự động
- ghi nhận thời điểm tạo

### Method `list(UUID userId, String platform, int page, int size)`
Lấy danh sách link theo user.
Có thể hỗ trợ:
- lọc theo nền tảng
- phân trang
- chỉ lấy link chưa xóa

Quy trình:
1. lấy dữ liệu từ repository
2. áp dụng filter nếu có `platform`
3. tạo `PageRequest`
4. map entity sang `LinkResponse`

Điểm tốt:
- trả về `Page<LinkResponse>` giúp client dễ phân trang
- tránh tải toàn bộ dữ liệu một lúc

### Method `getById(UUID userId, UUID linkId)`
Lấy chi tiết link nhưng có kiểm tra quyền sở hữu.
Logic thường là:
1. tìm link theo `linkId`
2. nếu không tồn tại → ném lỗi
3. nếu link không thuộc user hiện tại → từ chối
4. trả về dữ liệu chi tiết

Điều này giúp đảm bảo mỗi user chỉ thao tác trên dữ liệu của mình.
### Method `update(UUID userId, UUID linkId, UpdateLinkRequest req)`
Cập nhật một số thuộc tính của link.
Thông thường có thể sửa:
- title
- tags
- campaign

Quy trình:
1. kiểm tra link tồn tại
2. kiểm tra quyền user
3. cập nhật các field cho phép
4. lưu lại database
5. đồng bộ lại tag nếu cần
6. trả về dữ liệu mới

### Method `delete(UUID userId, UUID linkId)`
Xóa link bằng soft delete.
Quy trình:
1. tìm link
2. kiểm tra quyền
3. đánh dấu trạng thái xóa
4. lưu thay đổi
5. có thể xóa cache / key liên quan trong Redis nếu có

## 5. Giải thích các helper method
### 5.1. `saveTags(Link link, List<String> tagNames)`
Hàm này xử lý việc lưu tag cho link.
Nhiệm vụ:
- nhận danh sách tag từ request
- chuẩn hóa dữ liệu tag
- tạo hoặc cập nhật `LinkTag`
- gắn tag vào link

Tác dụng:
- tách phần xử lý tag ra khỏi logic chính
- giúp code gọn hơn
- dễ bảo trì hơn

### 5.2. `detectPlatform(String url)`
Hàm này dùng để đoán nền tảng của link từ URL.
Ví dụ:
- nếu URL chứa domain đặc trưng của sàn thương mại điện tử thì nhận diện là nền tảng đó
- nếu không khớp quy tắc nào thì có thể trả về `unknown`

Mục đích:
- tự động phân loại link
- phục vụ filter / thống kê / báo cáo sau này

## 6. Luồng xử lý tổng thể
### 6.1. Luồng tạo link
1. Client gọi API tạo link
2. Controller nhận request và user hiện tại
3. Service kiểm tra user
4. Service kiểm tra giới hạn tạo link
5. Service xác định platform
6. Service sinh short code
7. Service lưu link xuống database
8. Service lưu tags
9. Trả response về client

### 6.2. Luồng lấy danh sách link
1. Client gọi API danh sách
2. Controller lấy user + query params
3. Service truy vấn database theo user
4. Áp dụng filter `platform` nếu có
5. Trả về dữ liệu phân trang

### 6.3. Luồng cập nhật link
1. Client gửi request cập nhật
2. Controller nhận dữ liệu
3. Service kiểm tra link có thuộc user không
4. Cập nhật các trường được phép
5. Đồng bộ tag nếu có
6. Trả kết quả mới

### 6.4. Luồng xóa link
1. Client gửi yêu cầu xóa
2. Controller truyền và userId `linkId`
3. Service kiểm tra quyền
4. Đánh dấu link đã xóa
5. Không xóa vật lý khỏi database

## 7. Các điểm thiết kế đáng chú ý
### 7.1. Phân tách rõ trách nhiệm
- Controller: nhận và trả request
- Service: xử lý nghiệp vụ
- Repository: thao tác DB
- DTO: trao đổi dữ liệu
- Entity: ánh xạ bảng dữ liệu

Đây là cách tổ chức code rất phù hợp với Spring Boot.
### 7.2. Bảo vệ dữ liệu theo user
Mọi thao tác đều gắn với `userId`.
Điều này đảm bảo:
- user chỉ thấy dữ liệu của mình
- tránh truy cập trái phép
- dễ kiểm soát phân quyền

### 7.3. Dùng `@Valid`
Giúp kiểm tra dữ liệu đầu vào ngay từ request.
Ví dụ:
- URL bắt buộc hợp lệ
- trường bắt buộc không được null
- dữ liệu tags không được rỗng nếu rule quy định vậy

### 7.4. Dùng phân trang `Page<>`
Rất cần thiết khi số lượng link lớn.
Ưu điểm:
- giảm tải query
- tối ưu hiệu năng
- client dễ hiển thị danh sách dạng trang

### 7.5. Soft delete
Thay vì xóa hẳn bản ghi, hệ thống chỉ đánh dấu deleted.
Lợi ích:
- giữ lịch sử
- hỗ trợ khôi phục
- tránh mất dữ liệu liên quan

### 7.6. Redis
Việc có cho thấy hệ thống có thể dùng Redis cho các mục đích như: `StringRedisTemplate`
- cache
- khóa tạm thời
- tránh sinh dữ liệu trùng
- tối ưu tra cứu ngắn hạn

## 8. Cách viết mô tả này trong tài liệu
Nếu bạn muốn đưa vào README hoặc tài liệu dự án, có thể viết theo format như sau:
- **Chức năng module**
- **Cấu trúc package**
- **Mô tả class**
- **Luồng xử lý**
- **Nghiệp vụ chính**
- **Điểm cần lưu ý**

## 9. Phiên bản mô tả ngắn gọn hơn để chèn vào tài liệu
### Module `link`
Module này chịu trách nhiệm quản lý toàn bộ vòng đời của link affiliate, bao gồm tạo mới, liệt kê, xem chi tiết, cập nhật và xóa. Kiến trúc được tổ chức theo mô hình controller-service-repository, kết hợp DTO để tách biệt dữ liệu trao đổi với client và entity để ánh xạ cơ sở dữ liệu.
### `LinkController`
Là lớp tiếp nhận các request REST cho chức năng quản lý link. Controller chỉ thực hiện nhiệm vụ nhận dữ liệu, lấy thông tin người dùng hiện tại và chuyển xử lý sang . `LinkService`
### `LinkService`
Chứa toàn bộ nghiệp vụ của module link. Service xử lý việc kiểm tra quyền sở hữu, giới hạn số lượng link theo gói, sinh short code, nhận diện nền tảng, lưu tag và thao tác với dữ liệu thông qua repository.
### `DTO`
Các lớp DTO dùng để nhận request và trả response cho client, giúp đảm bảo tách biệt dữ liệu API với cấu trúc database nội bộ.
### `Entity`
Các entity biểu diễn dữ liệu link và tag trong database, phục vụ cho việc lưu trữ và truy vấn bằng Spring Data JPA.
### `Repository`
Các repository cung cấp các phương thức truy cập dữ liệu, bao gồm tìm kiếm, lọc, phân trang và kiểm tra tồn tại.